package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.domain.CashMovement;
import com.restosuite.domain.CashSession;
import com.restosuite.domain.PaymentMethod;
import com.restosuite.repo.CashMovementRepository;
import com.restosuite.repo.CashSessionRepository;
import com.restosuite.repo.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CashService {

    private final CashSessionRepository sessions;
    private final CashMovementRepository movements;
    private final PaymentMethodRepository methods;
    private final Support support;
    private final AuditService audit;

    public CashService(CashSessionRepository sessions, CashMovementRepository movements, PaymentMethodRepository methods,
                       Support support, AuditService audit) {
        this.sessions = sessions;
        this.movements = movements;
        this.methods = methods;
        this.support = support;
        this.audit = audit;
    }

    public java.util.Optional<CashSession> findOpen(Long branchId) {
        return sessions.findByBranchIdAndStatus(branchId, Codes.OPEN);
    }

    public CashSession requireOpen(Long branchId) {
        if (!support.flag("cash.require_session", true)) {
            return sessions.findByBranchIdAndStatus(branchId, Codes.OPEN).orElse(null);
        }
        return sessions.findByBranchIdAndStatus(branchId, Codes.OPEN)
                .orElseThrow(() -> ApiException.badRequest("No hay una caja abierta en esta sucursal. Abra la caja antes de cobrar o registrar salidas."));
    }

    @Transactional
    public CashSession open(Long branchId, BigDecimal openingFloat, String notes) {
        support.access().checkBranch(branchId);
        if (sessions.findByBranchIdAndStatus(branchId, Codes.OPEN).isPresent()) {
            throw ApiException.conflict("Ya existe una caja abierta en esta sucursal. Ciérrela antes de abrir otra.");
        }
        BigDecimal amount = Money.money(openingFloat);
        if (amount.signum() < 0) {
            throw ApiException.badRequest("El fondo inicial no puede ser negativo.");
        }
        CashSession session = new CashSession();
        session.setBranchId(branchId);
        session.setOpenedBy(support.user().getId());
        session.setOpenedAt(Instant.now());
        session.setOpeningFloat(amount);
        session.setStatus(Codes.OPEN);
        session.setNotes(Support.clean(notes, 400));
        sessions.saveAndFlush(session);
        record(session, Codes.OPENING, Codes.IN, amount, true, null, "SESSION", session.getId().toString(),
                "Fondo inicial", "opening-" + session.getId());
        audit.record("CASH_OPEN", "CASH_SESSION", session.getId(), "Fondo inicial " + amount);
        return session;
    }

    @Transactional
    public CashSession close(Long sessionId, Long version, BigDecimal countedCash, String notes) {
        CashSession session = sessions.lockById(sessionId).orElseThrow(() -> ApiException.notFound("La caja"));
        support.access().checkBranch(session.getBranchId());
        support.checkVersion(session.getVersion(), version);
        if (!Codes.OPEN.equals(session.getStatus())) {
            throw ApiException.conflict("Esta caja ya está cerrada.");
        }
        BigDecimal counted = Money.money(countedCash);
        if (counted.signum() < 0) {
            throw ApiException.badRequest("El efectivo contado no puede ser negativo.");
        }
        BigDecimal expected = expectedCash(session.getId());
        session.setExpectedCash(expected);
        session.setCountedCash(counted);
        session.setDifference(counted.subtract(expected));
        session.setStatus(Codes.CLOSED);
        session.setClosedAt(Instant.now());
        session.setClosedBy(support.user().getId());
        session.setCloseNotes(Support.clean(notes, 400));
        audit.record("CASH_CLOSE", "CASH_SESSION", session.getId(),
                "Esperado " + expected + ", contado " + counted + ", diferencia " + session.getDifference());
        return session;
    }

    @Transactional
    public CashMovement manual(Long sessionId, Long version, String type, BigDecimal amount, String description) {
        CashSession session = sessions.lockById(sessionId).orElseThrow(() -> ApiException.notFound("La caja"));
        support.access().checkBranch(session.getBranchId());
        support.checkVersion(session.getVersion(), version);
        if (!Codes.PAY_IN.equals(type) && !Codes.PAY_OUT.equals(type)) {
            throw ApiException.badRequest("Solo se permiten entradas o salidas manuales de efectivo.");
        }
        String direction = Codes.PAY_IN.equals(type) ? Codes.IN : Codes.OUT;
        return record(session, type, direction, Money.money(amount), true, null, "MANUAL", null,
                Support.required(description, "La descripción", 300), null);
    }

    @Transactional
    public CashMovement record(CashSession session, String type, String direction, BigDecimal amount, boolean affectsCash,
                               Long methodId, String referenceType, String referenceId, String description, String idempotencyKey) {
        if (session == null) {
            return null;
        }
        if (!Codes.OPEN.equals(session.getStatus())) {
            throw ApiException.conflict("La caja de este movimiento ya está cerrada. Abra un turno nuevo.");
        }
        if (idempotencyKey != null) {
            var existing = movements.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BigDecimal value = Money.money(amount);
        if (value.signum() < 0) {
            throw ApiException.badRequest("El importe del movimiento de caja no puede ser negativo.");
        }
        CashMovement movement = new CashMovement();
        movement.setSessionId(session.getId());
        movement.setMovementType(type);
        movement.setDirection(direction);
        movement.setAmount(value);
        movement.setAffectsCash(affectsCash);
        movement.setPaymentMethodId(methodId);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setDescription(description);
        movement.setUserId(support.user().getId());
        movement.setIdempotencyKey(idempotencyKey);
        return movements.save(movement);
    }

    public BigDecimal expectedCash(Long sessionId) {
        BigDecimal expected = Money.ZERO;
        for (CashMovement movement : movements.findBySessionIdOrderByCreatedAtAsc(sessionId)) {
            if (!movement.isAffectsCash()) {
                continue;
            }
            if (Codes.IN.equals(movement.getDirection())) {
                expected = expected.add(movement.getAmount());
            } else {
                expected = expected.subtract(movement.getAmount());
            }
        }
        return Money.money(expected);
    }

    public String methodLabel(Long methodId) {
        return methods.findById(methodId).map(PaymentMethod::getName).orElse("Método eliminado");
    }

    public PaymentMethod methodExisting(Long methodId) {
        PaymentMethod method = methods.findById(methodId).orElseThrow(() -> ApiException.notFound("El método de pago"));
        if (!method.getRestaurantId().equals(support.user().getRestaurantId())) {
            throw ApiException.forbidden("El método de pago no pertenece a este restaurante.");
        }
        return method;
    }

    public PaymentMethod method(Long methodId) {
        PaymentMethod method = methods.findById(methodId).orElseThrow(() -> ApiException.notFound("El método de pago"));
        if (!method.getRestaurantId().equals(support.user().getRestaurantId()) || !method.isActive()) {
            throw ApiException.badRequest("El método de pago no está activo en este restaurante.");
        }
        return method;
    }

    public List<CashMovement> movements(Long sessionId) {
        return movements.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
