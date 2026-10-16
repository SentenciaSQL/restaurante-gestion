package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.common.PageResponse;
import com.restosuite.domain.CashSession;
import com.restosuite.domain.DiningArea;
import com.restosuite.domain.DiningTable;
import com.restosuite.domain.KitchenTicket;
import com.restosuite.domain.KitchenTicketLine;
import com.restosuite.domain.Modifier;
import com.restosuite.domain.Payment;
import com.restosuite.domain.PaymentMethod;
import com.restosuite.domain.Product;
import com.restosuite.domain.ProductVariant;
import com.restosuite.domain.Refund;
import com.restosuite.domain.SalesOrder;
import com.restosuite.domain.SalesOrderDiscount;
import com.restosuite.domain.SalesOrderEvent;
import com.restosuite.domain.SalesOrderLine;
import com.restosuite.domain.SalesOrderLineModifier;
import com.restosuite.domain.TableOccupancy;
import com.restosuite.domain.TaxRate;
import com.restosuite.domain.UserAccount;
import com.restosuite.dto.OrderDto;
import com.restosuite.repo.CashSessionRepository;
import com.restosuite.repo.DiningAreaRepository;
import com.restosuite.repo.DiningTableRepository;
import com.restosuite.repo.KitchenTicketLineRepository;
import com.restosuite.repo.KitchenTicketRepository;
import com.restosuite.repo.ModifierRepository;
import com.restosuite.repo.PaymentRepository;
import com.restosuite.repo.ProductRepository;
import com.restosuite.repo.ProductVariantRepository;
import com.restosuite.repo.RefundRepository;
import com.restosuite.repo.SalesOrderDiscountRepository;
import com.restosuite.repo.SalesOrderEventRepository;
import com.restosuite.repo.SalesOrderLineModifierRepository;
import com.restosuite.repo.SalesOrderLineRepository;
import com.restosuite.repo.SalesOrderRepository;
import com.restosuite.repo.TableOccupancyRepository;
import com.restosuite.repo.TaxRateRepository;
import com.restosuite.repo.UserAccountRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class OrderService {

    private final SalesOrderRepository orders;
    private final SalesOrderLineRepository lines;
    private final SalesOrderLineModifierRepository lineModifiers;
    private final SalesOrderDiscountRepository discounts;
    private final SalesOrderEventRepository events;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ModifierRepository modifiers;
    private final TaxRateRepository taxes;
    private final DiningTableRepository tables;
    private final DiningAreaRepository areas;
    private final TableOccupancyRepository occupancy;
    private final UserAccountRepository users;
    private final KitchenTicketRepository tickets;
    private final KitchenTicketLineRepository ticketLines;
    private final CashSessionRepository sessions;
    private final Support support;
    private final SequenceService sequences;
    private final OrderWorkflow workflow;
    private final InventoryService inventory;
    private final CashService cash;
    private final KitchenHub kitchenHub;
    private final AuditService audit;
    private final ReceiptService receipts;

    public OrderService(SalesOrderRepository orders, SalesOrderLineRepository lines, SalesOrderLineModifierRepository lineModifiers,
                        SalesOrderDiscountRepository discounts, SalesOrderEventRepository events, PaymentRepository payments,
                        RefundRepository refunds, ProductRepository products, ProductVariantRepository variants,
                        ModifierRepository modifiers, TaxRateRepository taxes, DiningTableRepository tables,
                        DiningAreaRepository areas, TableOccupancyRepository occupancy, UserAccountRepository users,
                        KitchenTicketRepository tickets, KitchenTicketLineRepository ticketLines, CashSessionRepository sessions,
                        Support support, SequenceService sequences, OrderWorkflow workflow, InventoryService inventory,
                        CashService cash, KitchenHub kitchenHub, AuditService audit, ReceiptService receipts) {
        this.orders = orders;
        this.lines = lines;
        this.lineModifiers = lineModifiers;
        this.discounts = discounts;
        this.events = events;
        this.payments = payments;
        this.refunds = refunds;
        this.products = products;
        this.variants = variants;
        this.modifiers = modifiers;
        this.taxes = taxes;
        this.tables = tables;
        this.areas = areas;
        this.occupancy = occupancy;
        this.users = users;
        this.tickets = tickets;
        this.ticketLines = ticketLines;
        this.sessions = sessions;
        this.support = support;
        this.sequences = sequences;
        this.workflow = workflow;
        this.inventory = inventory;
        this.cash = cash;
        this.kitchenHub = kitchenHub;
        this.audit = audit;
        this.receipts = receipts;
    }

    @Transactional
    public OrderDto.OrderView create(OrderDto.Create request) {
        support.access().checkBranch(request.branchId());
        String type = request.orderType();
        if (!List.of(Codes.DINE_IN, Codes.TAKEAWAY, Codes.DELIVERY).contains(type)) {
            throw ApiException.badRequest("El tipo de pedido debe ser en mesa, para llevar o a domicilio.");
        }
        SalesOrder order = new SalesOrder();
        order.setBranchId(request.branchId());
        order.setOrderNumber(sequences.next(request.branchId(), Codes.DOC_ORDER));
        order.setOrderType(type);
        order.setWaiterId(support.user().getId());
        if (Codes.DINE_IN.equals(type) && request.tableId() != null) {
            tables.findById(request.tableId())
                    .map(com.restosuite.domain.DiningTable::getAssignedWaiterId)
                    .ifPresent(order::setWaiterId);
        }
        order.setCustomerName(Support.clean(request.customerName(), 160));
        order.setCustomerPhone(Support.clean(request.customerPhone(), 40));
        order.setDeliveryAddress(Support.clean(request.deliveryAddress(), 300));
        order.setGuestCount(request.guestCount() == null ? 1 : request.guestCount());
        order.setPreparationStatus(Codes.DRAFT);
        order.setAccountStatus(Codes.OPEN);
        order.setPricesIncludeTax(support.flag("tax.prices_include_tax", false));
        order.setInventoryTrigger(support.setting("inventory.deduct_trigger", Codes.ON_SEND));
        order.setNotes(Support.clean(request.notes(), 400));
        order.setOpenedAt(Instant.now());
        order.setCreatedBy(support.user().getId());
        zero(order);
        if (Codes.DINE_IN.equals(type)) {
            if (request.tableId() == null) {
                throw ApiException.badRequest("Seleccione una mesa para el pedido en salón.");
            }
            occupy(request.tableId(), request.branchId(), order);
        } else if (Codes.DELIVERY.equals(type) && order.getDeliveryAddress() == null) {
            throw ApiException.badRequest("Indique la dirección para el pedido a domicilio.");
        }
        orders.saveAndFlush(order);
        event(order, "CREATED", "Pedido abierto");
        audit.record("ORDER_CREATE", "ORDER", order.getId(), order.getOrderNumber());
        return view(order);
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderView get(Long id) {
        return view(loadReadable(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDto.Summary> openOnTable(Long tableId) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        support.access().checkBranch(area.getBranchId());
        return orders.findByTableIdAndAccountStatus(tableId, Codes.OPEN).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto.Summary> list(Long branchId, String accountStatus, String orderType, String q,
                                               LocalDate from, LocalDate to, int page, int size) {
        Long resolved = support.access().resolveBranch(branchId);
        List<Long> allowedBranches = resolved == null ? branchIds() : List.of(resolved);
        if (allowedBranches.isEmpty()) {
            return new PageResponse<>(List.of(), 0, page, size);
        }
        Support.Period period = from == null && to == null ? null : support.period(from, to);
        var result = orders.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("branchId").in(allowedBranches));
            if (accountStatus != null && !accountStatus.isBlank()) {
                predicates.add(cb.equal(root.get("accountStatus"), accountStatus));
            }
            if (orderType != null && !orderType.isBlank()) {
                predicates.add(cb.equal(root.get("orderType"), orderType));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("customerName"), "")), like)));
            }
            if (period != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("openedAt"), period.start()));
                predicates.add(cb.lessThan(root.get("openedAt"), period.end()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "openedAt")));
        return new PageResponse<>(result.getContent().stream().map(this::summary).toList(), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional
    public OrderDto.OrderView addLine(Long orderId, OrderDto.AddLine request) {
        SalesOrder order = lockOpen(orderId, request.version());
        Product product = products.findByIdAndRestaurantId(request.productId(), support.user().getRestaurantId())
                .orElseThrow(() -> ApiException.notFound("El producto"));
        if (!product.isActive() || !product.isAvailable()) {
            throw ApiException.badRequest(product.getName() + " no está disponible. Actívelo en el menú o elija otro producto.");
        }
        ProductVariant variant = null;
        if (request.variantId() != null) {
            variant = variants.findById(request.variantId()).orElseThrow(() -> ApiException.notFound("La variante"));
            if (!variant.getProductId().equals(product.getId()) || !variant.isActive()) {
                throw ApiException.badRequest("La variante no corresponde a este producto.");
            }
        }
        List<Modifier> chosen = new ArrayList<>();
        BigDecimal modifierPerUnit = BigDecimal.ZERO;
        List<Long> allowed = products.modifierIds(product.getId());
        if (request.modifierIds() != null) {
            for (Long modifierId : new HashSet<>(request.modifierIds())) {
                if (!allowed.contains(modifierId)) {
                    throw ApiException.badRequest("Uno de los extras no aplica a este producto.");
                }
                Modifier modifier = modifiers.findById(modifierId).orElseThrow(() -> ApiException.notFound("El extra"));
                if (!modifier.isActive()) {
                    throw ApiException.badRequest(modifier.getName() + " no está activo.");
                }
                chosen.add(modifier);
                modifierPerUnit = modifierPerUnit.add(modifier.getPrice());
            }
        }
        TaxRate tax = taxes.findById(product.getTaxRateId()).orElseThrow(() -> ApiException.notFound("El impuesto"));
        SalesOrderLine line = new SalesOrderLine();
        line.setOrderId(order.getId());
        line.setProductId(product.getId());
        line.setVariantId(variant == null ? null : variant.getId());
        line.setProductName(product.getName());
        line.setVariantName(variant == null ? null : variant.getName());
        line.setUnitPrice(Money.money(product.getBasePrice().add(variant == null ? BigDecimal.ZERO : variant.getPriceDelta())));
        line.setModifierAmount(Money.money(modifierPerUnit));
        line.setQuantity(Money.qty(request.quantity()));
        line.setEnteredAmount(Money.ZERO);
        line.setLineDiscount(Money.ZERO);
        line.setGrossAmount(Money.ZERO);
        line.setDiscountAmount(Money.ZERO);
        line.setNetAmount(Money.ZERO);
        line.setTaxAmount(Money.ZERO);
        line.setTotalAmount(Money.ZERO);
        line.setTaxRate(tax.getRate());
        line.setStatus(Codes.PENDING);
        line.setNotes(Support.clean(request.notes(), 300));
        line.setStationId(product.getStationId());
        line.setStationName(stationName(product.getStationId()));
        line.setCostAmount(Money.cost(BigDecimal.ZERO));
        lines.saveAndFlush(line);
        for (Modifier modifier : chosen) {
            SalesOrderLineModifier snapshot = new SalesOrderLineModifier();
            snapshot.setOrderLineId(line.getId());
            snapshot.setModifierId(modifier.getId());
            snapshot.setName(modifier.getName());
            snapshot.setUnitPrice(Money.money(modifier.getPrice()));
            lineModifiers.save(snapshot);
        }
        recompute(order);
        event(order, "LINE_ADDED", line.getQuantity() + " × " + line.getProductName());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView updateLine(Long orderId, Long lineId, OrderDto.UpdateLine request) {
        SalesOrder order = lockOpen(orderId, request.version());
        SalesOrderLine line = line(order, lineId);
        if (!Codes.PENDING.equals(line.getStatus())) {
            throw ApiException.badRequest("Este artículo ya se envió a cocina. Anúlelo y agréguelo de nuevo si debe cambiar la cantidad.");
        }
        line.setQuantity(Money.qty(request.quantity()));
        line.setNotes(Support.clean(request.notes(), 300));
        recompute(order);
        event(order, "LINE_UPDATED", "Cantidad actualizada de " + line.getProductName());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView cancelLine(Long orderId, Long lineId, OrderDto.Reason request) {
        SalesOrder order = lockOpen(orderId, request.version());
        SalesOrderLine line = line(order, lineId);
        if (Codes.CANCELLED.equals(line.getStatus())) {
            throw ApiException.badRequest("El artículo ya está anulado.");
        }
        if (!Codes.PENDING.equals(line.getStatus()) && !support.user().has("orders.cancel")) {
            throw ApiException.forbidden("Anular un artículo ya enviado requiere el permiso de anulación.");
        }
        String reason = Support.required(request.reason(), "El motivo", 300);
        if (line.isInventoryDeducted()) {
            inventory.reverseLine(line, "Anulación: " + reason);
        }
        cancelTicketLines(line.getId(), reason);
        line.setStatus(Codes.CANCELLED);
        line.setCancelReason(reason);
        line.setCancelledAt(Instant.now());
        recompute(order);
        event(order, "LINE_CANCELLED", line.getProductName() + ": " + reason);
        audit.record("ORDER_LINE_CANCEL", "ORDER", order.getId(), line.getProductName());
        kitchenHub.publish(order.getBranchId());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView discount(Long orderId, OrderDto.Discount request) {
        SalesOrder order = lockOpen(orderId, request.version());
        String reason = Support.required(request.reason(), "El motivo", 300);
        List<SalesOrderLine> current = lines.findByOrderIdOrderByIdAsc(order.getId());
        BigDecimal base = current.stream().filter(line -> !Codes.CANCELLED.equals(line.getStatus()))
                .map(line -> Money.money(line.getUnitPrice().add(line.getModifierAmount()).multiply(line.getQuantity())))
                .reduce(Money.ZERO, BigDecimal::add);
        BigDecimal amount;
        if (Codes.PERCENT.equals(request.discountType())) {
            if (request.value().compareTo(new BigDecimal("100")) > 0) {
                throw ApiException.badRequest("El porcentaje de descuento no puede superar 100.");
            }
            amount = Money.money(base.multiply(request.value()).divide(new BigDecimal("100"), 8, Money.ROUND));
        } else if (Codes.AMOUNT.equals(request.discountType())) {
            amount = Money.money(request.value());
        } else {
            throw ApiException.badRequest("El descuento debe ser un porcentaje o un importe.");
        }
        order.setHeaderDiscount(amount);
        SalesOrderDiscount history = new SalesOrderDiscount();
        history.setOrderId(order.getId());
        history.setDiscountType(request.discountType());
        history.setValue(request.value());
        history.setAmount(amount);
        history.setReason(reason);
        history.setAuthorizedBy(support.user().getId());
        discounts.save(history);
        recompute(order);
        event(order, "DISCOUNT", "Descuento de " + amount + ": " + reason);
        audit.record("ORDER_DISCOUNT", "ORDER", order.getId(), reason);
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView send(Long orderId, OrderDto.Send request) {
        SalesOrder order = lockOpen(orderId, request.version());
        List<SalesOrderLine> current = lines.findByOrderIdOrderByIdAsc(order.getId());
        List<SalesOrderLine> selected = new ArrayList<>();
        for (SalesOrderLine line : current) {
            boolean wanted = request.lineIds() == null || request.lineIds().isEmpty() || request.lineIds().contains(line.getId());
            if (wanted && Codes.PENDING.equals(line.getStatus())) {
                selected.add(line);
            }
        }
        if (selected.isEmpty()) {
            throw ApiException.badRequest("No hay artículos pendientes de envío. Agregue productos o seleccione líneas en borrador.");
        }
        Map<Long, List<SalesOrderLine>> grouped = new LinkedHashMap<>();
        for (SalesOrderLine line : selected) {
            grouped.computeIfAbsent(line.getStationId() == null ? 0L : line.getStationId(), key -> new ArrayList<>()).add(line);
        }
        Instant now = Instant.now();
        for (var entry : grouped.entrySet()) {
            SalesOrderLine sample = entry.getValue().getFirst();
            KitchenTicket ticket = new KitchenTicket();
            ticket.setOrderId(order.getId());
            ticket.setBranchId(order.getBranchId());
            ticket.setStationId(entry.getKey() == 0L ? null : entry.getKey());
            ticket.setStationName(sample.getStationName() == null ? "General" : sample.getStationName());
            ticket.setStatus(Codes.QUEUED);
            ticket.setReceivedAt(now);
            tickets.saveAndFlush(ticket);
            for (SalesOrderLine line : entry.getValue()) {
                line.setStatus(Codes.SENT);
                line.setSentAt(now);
                KitchenTicketLine ticketLine = new KitchenTicketLine();
                ticketLine.setTicketId(ticket.getId());
                ticketLine.setOrderLineId(line.getId());
                ticketLine.setQuantity(line.getQuantity());
                ticketLine.setProductName(display(line));
                ticketLine.setNotes(line.getNotes());
                ticketLine.setStatus(Codes.QUEUED);
                ticketLines.save(ticketLine);
            }
        }
        if (Codes.ON_SEND.equals(order.getInventoryTrigger())) {
            inventory.deductOrderLines(order, selected);
        }
        recompute(order);
        event(order, "SENT", selected.size() + " artículo(s) enviados a cocina");
        audit.record("ORDER_SEND", "ORDER", order.getId(), order.getOrderNumber());
        kitchenHub.publish(order.getBranchId());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView deliver(Long orderId, OrderDto.Deliver request) {
        SalesOrder order = lockOpen(orderId, request.version());
        List<SalesOrderLine> current = lines.findByOrderIdOrderByIdAsc(order.getId());
        int delivered = 0;
        Instant now = Instant.now();
        for (SalesOrderLine line : current) {
            boolean wanted = request.lineIds() == null || request.lineIds().isEmpty() || request.lineIds().contains(line.getId());
            if (!wanted || Codes.DELIVERED.equals(line.getStatus()) || Codes.CANCELLED.equals(line.getStatus())) {
                continue;
            }
            if (!Codes.READY.equals(line.getStatus())) {
                throw ApiException.badRequest(line.getProductName() + " todavía no está listo. Espere a cocina o marque solo los artículos listos.");
            }
            line.setStatus(Codes.DELIVERED);
            line.setDeliveredAt(now);
            markTicketLines(line.getId(), Codes.DELIVERED);
            delivered++;
        }
        if (delivered == 0) {
            throw ApiException.badRequest("No hay artículos listos para entregar.");
        }
        recompute(order);
        event(order, "DELIVERED", delivered + " artículo(s) entregados");
        kitchenHub.publish(order.getBranchId());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView pay(Long orderId, OrderDto.Pay request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("Envíe el encabezado Idempotency-Key para evitar un cobro duplicado.");
        }
        var existing = payments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!payment.getOrderId().equals(orderId) || payment.getAmount().compareTo(Money.money(request.amount())) != 0) {
                throw ApiException.conflict("Esta clave de idempotencia ya se usó en otro cobro. Genere una clave nueva.");
            }
            return view(loadReadable(orderId));
        }
        SalesOrder order = lockOpen(orderId, request.version());
        BigDecimal amount = Money.money(request.amount());
        BigDecimal tip = Money.money(request.tipAmount() == null ? BigDecimal.ZERO : request.tipAmount());
        BigDecimal remaining = order.getTotalAmount().subtract(order.getPaidAmount());
        if (amount.compareTo(remaining) > 0) {
            throw ApiException.badRequest("El cobro supera el saldo de " + remaining + ". Registre solo el saldo pendiente; la propina va aparte.");
        }
        PaymentMethod method = cash.method(request.methodId());
        if (method.isRequiresReference() && (request.reference() == null || request.reference().isBlank())) {
            throw ApiException.badRequest("Este método de pago requiere una referencia.");
        }
        CashSession session = cash.requireOpen(order.getBranchId());
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setSessionId(session == null ? null : session.getId());
        payment.setMethodId(method.getId());
        payment.setAmount(amount);
        payment.setTipAmount(tip);
        payment.setReference(Support.clean(request.reference(), 80));
        payment.setStatus(Codes.COMPLETED);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setPaidAt(Instant.now());
        payment.setUserId(support.user().getId());
        payments.saveAndFlush(payment);
        var movement = cash.record(session, Codes.SALE, Codes.IN, amount.add(tip), method.isAffectsCash(), method.getId(),
                "PAYMENT", payment.getId().toString(), "Cobro " + order.getOrderNumber(), "pay-" + idempotencyKey);
        if (movement != null) {
            payment.setCashMovementId(movement.getId());
        }
        order.setPaidAmount(order.getPaidAmount().add(amount));
        order.setTipAmount(order.getTipAmount().add(tip));
        event(order, "PAYMENT", method.getName() + " " + amount);
        audit.record("PAYMENT", "ORDER", order.getId(), amount + " " + method.getCode());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView voidPayment(Long orderId, Long paymentId, OrderDto.VoidPayment request) {
        SalesOrder order = lockOpen(orderId, request.version());
        Payment payment = payments.findById(paymentId).orElseThrow(() -> ApiException.notFound("El cobro"));
        if (!payment.getOrderId().equals(order.getId()) || !Codes.COMPLETED.equals(payment.getStatus())) {
            throw ApiException.badRequest("El cobro no se puede anular.");
        }
        String reason = Support.required(request.reason(), "El motivo", 300);
        if (payment.getSessionId() != null) {
            CashSession session = sessions.findById(payment.getSessionId()).orElseThrow();
            if (!Codes.OPEN.equals(session.getStatus())) {
                throw ApiException.conflict("La caja de ese cobro ya está cerrada. Registre un reembolso en el turno actual después de cerrar la cuenta, o un ajuste de caja.");
            }
            PaymentMethod method = cash.methodExisting(payment.getMethodId());
            cash.record(session, Codes.REFUND, Codes.OUT, payment.getAmount().add(payment.getTipAmount()), method.isAffectsCash(),
                    method.getId(), "PAYMENT", payment.getId().toString(), "Anulación de cobro: " + reason, "void-" + payment.getId());
        }
        payment.setStatus(Codes.VOIDED);
        payment.setVoidReason(reason);
        payment.setVoidedAt(Instant.now());
        payment.setVoidedBy(support.user().getId());
        order.setPaidAmount(order.getPaidAmount().subtract(payment.getAmount()));
        order.setTipAmount(order.getTipAmount().subtract(payment.getTipAmount()));
        event(order, "PAYMENT_VOID", reason);
        audit.record("PAYMENT_VOID", "ORDER", order.getId(), reason);
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView refund(Long orderId, OrderDto.Refund request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("Envíe el encabezado Idempotency-Key para evitar un reembolso duplicado.");
        }
        var existing = refunds.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return view(loadReadable(orderId));
        }
        SalesOrder order = orders.lockById(orderId).orElseThrow(() -> ApiException.notFound("El pedido"));
        support.access().checkBranch(order.getBranchId());
        support.checkVersion(order.getVersion(), request.version());
        if (!Codes.CLOSED.equals(order.getAccountStatus())) {
            throw ApiException.badRequest("Reembolse solo cuentas cerradas. Si la cuenta sigue abierta, anule el cobro.");
        }
        Payment payment = payments.findById(request.paymentId()).orElseThrow(() -> ApiException.notFound("El cobro"));
        if (!payment.getOrderId().equals(order.getId()) || !Codes.COMPLETED.equals(payment.getStatus())) {
            throw ApiException.badRequest("El cobro indicado no admite reembolso.");
        }
        BigDecimal already = refunds.findByPaymentId(payment.getId()).stream().map(Refund::getAmount).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal amount = Money.money(request.amount());
        if (amount.compareTo(payment.getAmount().subtract(already)) > 0) {
            throw ApiException.badRequest("El reembolso supera el saldo reembolsable de ese cobro.");
        }
        String reason = Support.required(request.reason(), "El motivo", 300);
        BigDecimal net;
        BigDecimal tax;
        BigDecimal refundedBefore = refunds.findByOrderIdOrderByIdAsc(order.getId()).stream().map(Refund::getAmount).reduce(Money.ZERO, BigDecimal::add);
        if (refundedBefore.add(amount).compareTo(order.getTotalAmount()) == 0) {
            BigDecimal netBefore = refunds.findByOrderIdOrderByIdAsc(order.getId()).stream().map(Refund::getNetAmount).reduce(Money.ZERO, BigDecimal::add);
            BigDecimal taxBefore = refunds.findByOrderIdOrderByIdAsc(order.getId()).stream().map(Refund::getTaxAmount).reduce(Money.ZERO, BigDecimal::add);
            net = order.getNetAmount().subtract(netBefore);
            tax = order.getTaxAmount().subtract(taxBefore);
        } else if (order.getTotalAmount().signum() == 0) {
            net = Money.ZERO;
            tax = Money.ZERO;
        } else {
            net = Money.money(amount.multiply(order.getNetAmount()).divide(order.getTotalAmount(), 8, Money.ROUND));
            tax = amount.subtract(net);
        }
        BigDecimal cost = Money.cost(BigDecimal.ZERO);
        if (request.restock()) {
            if (request.lineIds() == null || request.lineIds().isEmpty()) {
                throw ApiException.badRequest("Indique los artículos que regresan al inventario.");
            }
            for (Long lineId : request.lineIds()) {
                SalesOrderLine line = line(order, lineId);
                if (line.isRestocked()) {
                    throw ApiException.conflict(line.getProductName() + " ya regresó al inventario.");
                }
                cost = cost.add(line.getCostAmount());
                inventory.reverseLine(line, "Devolución: " + reason);
            }
        }
        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setOrderId(order.getId());
        refund.setAmount(amount);
        refund.setNetAmount(net);
        refund.setTaxAmount(tax);
        refund.setCostAmount(Money.cost(cost));
        refund.setRestock(request.restock());
        refund.setReason(reason);
        refund.setAuthorizedBy(support.user().getId());
        refund.setIdempotencyKey(idempotencyKey);
        refunds.saveAndFlush(refund);
        PaymentMethod method = cash.methodExisting(payment.getMethodId());
        CashSession session = cash.requireOpen(order.getBranchId());
        var movement = cash.record(session, Codes.REFUND, Codes.OUT, amount, method.isAffectsCash(), method.getId(),
                "REFUND", refund.getId().toString(), "Reembolso " + order.getOrderNumber(), "refund-" + idempotencyKey);
        if (movement != null) {
            refund.setCashMovementId(movement.getId());
        }
        event(order, "REFUND", reason);
        audit.record("REFUND", "ORDER", order.getId(), amount + " " + reason);
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView close(Long orderId, Long version) {
        SalesOrder order = lockOpen(orderId, version);
        if (order.getPaidAmount().compareTo(order.getTotalAmount()) < 0) {
            throw ApiException.badRequest("El saldo no está en cero. Falta " + order.getTotalAmount().subtract(order.getPaidAmount()) + " antes de cerrar la cuenta.");
        }
        List<SalesOrderLine> current = lines.findByOrderIdOrderByIdAsc(order.getId());
        for (SalesOrderLine line : current) {
            if (Codes.PENDING.equals(line.getStatus())) {
                throw ApiException.badRequest("Hay artículos sin enviar a cocina. Envíelos o anúlelos antes de cerrar.");
            }
            if (Codes.SENT.equals(line.getStatus()) || Codes.PREPARING.equals(line.getStatus())) {
                throw ApiException.badRequest("Hay artículos todavía en cocina. Espere a que estén listos antes de cerrar.");
            }
        }
        Instant now = Instant.now();
        for (SalesOrderLine line : current) {
            if (Codes.READY.equals(line.getStatus())) {
                line.setStatus(Codes.DELIVERED);
                line.setDeliveredAt(now);
                markTicketLines(line.getId(), Codes.DELIVERED);
            }
        }
        if (Codes.ON_CLOSE.equals(order.getInventoryTrigger())) {
            inventory.deductOrderLines(order, current);
        }
        order.setAccountStatus(Codes.CLOSED);
        order.setClosedAt(now);
        recompute(order);
        releaseTable(order, Codes.CLEANING);
        receipts.issue(order);
        event(order, "CLOSED", "Cuenta cerrada");
        audit.record("ORDER_CLOSE", "ORDER", order.getId(), order.getOrderNumber());
        kitchenHub.publish(order.getBranchId());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView cancel(Long orderId, OrderDto.Reason request) {
        SalesOrder order = lockOpen(orderId, request.version());
        if (order.getPaidAmount().signum() > 0) {
            throw ApiException.badRequest("La cuenta tiene cobros. Anule los cobros antes de cancelar el pedido.");
        }
        String reason = Support.required(request.reason(), "El motivo", 300);
        for (SalesOrderLine line : lines.findByOrderIdOrderByIdAsc(order.getId())) {
            if (Codes.CANCELLED.equals(line.getStatus())) {
                continue;
            }
            if (line.isInventoryDeducted()) {
                inventory.reverseLine(line, "Cancelación: " + reason);
            }
            cancelTicketLines(line.getId(), reason);
            line.setStatus(Codes.CANCELLED);
            line.setCancelReason(reason);
            line.setCancelledAt(Instant.now());
        }
        order.setAccountStatus(Codes.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledAt(Instant.now());
        recompute(order);
        releaseTable(order, Codes.CLEANING);
        event(order, "CANCELLED", reason);
        audit.record("ORDER_CANCEL", "ORDER", order.getId(), reason);
        kitchenHub.publish(order.getBranchId());
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView transfer(Long orderId, OrderDto.Transfer request) {
        SalesOrder order = lockOpen(orderId, request.version());
        if (!Codes.DINE_IN.equals(order.getOrderType())) {
            throw ApiException.badRequest("Solo las cuentas de mesa se pueden transferir.");
        }
        Long previous = order.getTableId();
        occupyExisting(order, request.tableId());
        if (previous != null) {
            markTable(previous, Codes.CLEANING);
        }
        event(order, "TRANSFER", "Cuenta movida a " + tableLabel(order.getTableId()));
        audit.record("ORDER_TRANSFER", "ORDER", order.getId(), tableLabel(order.getTableId()));
        return view(order);
    }

    @Transactional
    public OrderDto.OrderView split(Long orderId, OrderDto.Split request) {
        SalesOrder order = lockOpen(orderId, request.version());
        if (request.lineIds().isEmpty()) {
            throw ApiException.badRequest("Seleccione los artículos que pasan a la cuenta nueva.");
        }
        SalesOrder target = new SalesOrder();
        target.setBranchId(order.getBranchId());
        target.setOrderNumber(sequences.next(order.getBranchId(), Codes.DOC_ORDER));
        target.setWaiterId(order.getWaiterId());
        target.setGuestCount(order.getGuestCount());
        target.setPreparationStatus(Codes.DRAFT);
        target.setAccountStatus(Codes.OPEN);
        target.setPricesIncludeTax(order.isPricesIncludeTax());
        target.setInventoryTrigger(order.getInventoryTrigger());
        target.setOpenedAt(Instant.now());
        target.setCreatedBy(support.user().getId());
        zero(target);
        if (request.targetTableId() == null) {
            target.setOrderType(Codes.TAKEAWAY);
            target.setCustomerName(order.getCustomerName());
        } else {
            target.setOrderType(Codes.DINE_IN);
            orders.saveAndFlush(target);
            occupyExisting(target, request.targetTableId());
        }
        if (target.getId() == null) {
            orders.saveAndFlush(target);
        }
        List<Long> moved = new ArrayList<>();
        for (Long lineId : request.lineIds()) {
            SalesOrderLine line = line(order, lineId);
            if (Codes.CANCELLED.equals(line.getStatus())) {
                throw ApiException.badRequest("No puede mover un artículo anulado.");
            }
            line.setOrderId(target.getId());
            moved.add(line.getId());
        }
        retargetTickets(moved, target);
        recompute(order);
        recompute(target);
        event(order, "SPLIT", "Artículos movidos a " + target.getOrderNumber());
        event(target, "SPLIT", "Cuenta originada desde " + order.getOrderNumber());
        audit.record("ORDER_SPLIT", "ORDER", order.getId(), target.getOrderNumber());
        return view(target);
    }

    @Transactional
    public OrderDto.OrderView merge(Long orderId, OrderDto.Merge request) {
        SalesOrder target = lockOpen(orderId, request.version());
        SalesOrder source = orders.lockById(request.sourceOrderId()).orElseThrow(() -> ApiException.notFound("La cuenta origen"));
        support.access().checkBranch(source.getBranchId());
        if (source.getId().equals(target.getId()) || !Codes.OPEN.equals(source.getAccountStatus())) {
            throw ApiException.badRequest("Elija otra cuenta abierta para consolidar.");
        }
        if (!source.getBranchId().equals(target.getBranchId())) {
            throw ApiException.badRequest("Las cuentas deben ser de la misma sucursal.");
        }
        if (source.getPaidAmount().signum() > 0) {
            throw ApiException.badRequest("La cuenta origen tiene cobros. Anúlelos antes de consolidar.");
        }
        List<Long> moved = new ArrayList<>();
        for (SalesOrderLine line : lines.findByOrderIdOrderByIdAsc(source.getId())) {
            if (!Codes.CANCELLED.equals(line.getStatus())) {
                line.setOrderId(target.getId());
                moved.add(line.getId());
            }
        }
        if (moved.isEmpty()) {
            throw ApiException.badRequest("La cuenta origen no tiene artículos para consolidar.");
        }
        BigDecimal discount = target.getHeaderDiscount().add(source.getHeaderDiscount());
        target.setHeaderDiscount(discount);
        source.setHeaderDiscount(Money.ZERO);
        source.setAccountStatus(Codes.CANCELLED);
        source.setCancelReason("Consolidada en " + target.getOrderNumber());
        source.setCancelledAt(Instant.now());
        retargetTickets(moved, target);
        recompute(source);
        recompute(target);
        releaseTable(source, Codes.CLEANING);
        event(target, "MERGE", "Se consolidó " + source.getOrderNumber());
        audit.record("ORDER_MERGE", "ORDER", target.getId(), source.getOrderNumber());
        return view(target);
    }

    private void retargetTickets(List<Long> lineIds, SalesOrder target) {
        Set<Long> oldTickets = new HashSet<>();
        Map<String, KitchenTicket> created = new LinkedHashMap<>();
        for (Long lineId : lineIds) {
            for (KitchenTicketLine ticketLine : ticketLines.findByOrderLineId(lineId)) {
                KitchenTicket old = tickets.findById(ticketLine.getTicketId()).orElseThrow();
                oldTickets.add(old.getId());
                String key = Objects.toString(old.getStationId(), "0") + ":" + old.getStatus();
                KitchenTicket dest = created.computeIfAbsent(key, ignored -> {
                    KitchenTicket copy = new KitchenTicket();
                    copy.setOrderId(target.getId());
                    copy.setBranchId(target.getBranchId());
                    copy.setStationId(old.getStationId());
                    copy.setStationName(old.getStationName());
                    copy.setStatus(old.getStatus());
                    copy.setReceivedAt(old.getReceivedAt());
                    copy.setStartedAt(old.getStartedAt());
                    copy.setReadyAt(old.getReadyAt());
                    return tickets.saveAndFlush(copy);
                });
                ticketLine.setTicketId(dest.getId());
            }
        }
        ticketLines.flush();
        for (Long ticketId : oldTickets) {
            KitchenTicket ticket = tickets.findById(ticketId).orElseThrow();
            if (ticketLines.findByTicketId(ticketId).isEmpty()) {
                ticket.setStatus(Codes.CANCELLED);
            }
        }
    }

    private SalesOrder lockOpen(Long orderId, Long version) {
        SalesOrder order = orders.lockById(orderId).orElseThrow(() -> ApiException.notFound("El pedido"));
        support.access().checkBranch(order.getBranchId());
        support.checkVersion(order.getVersion(), version);
        if (!Codes.OPEN.equals(order.getAccountStatus())) {
            throw ApiException.conflict("La cuenta ya no está abierta.");
        }
        return order;
    }

    private SalesOrder loadReadable(Long id) {
        SalesOrder order = orders.findById(id).orElseThrow(() -> ApiException.notFound("El pedido"));
        support.access().checkBranch(order.getBranchId());
        return order;
    }

    private SalesOrderLine line(SalesOrder order, Long lineId) {
        SalesOrderLine line = lines.findById(lineId).orElseThrow(() -> ApiException.notFound("El artículo"));
        if (!line.getOrderId().equals(order.getId())) {
            throw ApiException.notFound("El artículo");
        }
        return line;
    }

    private void recompute(SalesOrder order) {
        workflow.recalculate(order, lines.findByOrderIdOrderByIdAsc(order.getId()));
        orders.saveAndFlush(order);
    }

    private void zero(SalesOrder order) {
        order.setGrossAmount(Money.ZERO);
        order.setDiscountAmount(Money.ZERO);
        order.setNetAmount(Money.ZERO);
        order.setTaxAmount(Money.ZERO);
        order.setTotalAmount(Money.ZERO);
        order.setPaidAmount(Money.ZERO);
        order.setTipAmount(Money.ZERO);
        order.setHeaderDiscount(Money.ZERO);
    }

    private void occupy(Long tableId, Long branchId, SalesOrder order) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        if (!area.getBranchId().equals(branchId)) {
            throw ApiException.badRequest("La mesa no pertenece a esta sucursal.");
        }
        if (!table.isActive() || Codes.OCCUPIED.equals(table.getStatus())) {
            throw ApiException.conflict("La mesa " + table.getTableNumber() + " no está libre. Elija otra o transfiera la cuenta actual.");
        }
        orders.saveAndFlush(order);
        order.setTableId(table.getId());
        table.setStatus(Codes.OCCUPIED);
        TableOccupancy row = new TableOccupancy();
        row.setTableId(table.getId());
        row.setOrderId(order.getId());
        row.setWaiterId(order.getWaiterId());
        row.setStatus(Codes.OCCUPIED);
        row.setOpenedAt(Instant.now());
        occupancy.save(row);
    }

    private void occupyExisting(SalesOrder order, Long tableId) {
        DiningTable table = tables.findById(tableId).orElseThrow(() -> ApiException.notFound("La mesa"));
        DiningArea area = areas.findById(table.getAreaId()).orElseThrow();
        if (!area.getBranchId().equals(order.getBranchId())) {
            throw ApiException.badRequest("La mesa no pertenece a esta sucursal.");
        }
        if (table.getId().equals(order.getTableId())) {
            return;
        }
        if (!table.isActive() || Codes.OCCUPIED.equals(table.getStatus())) {
            throw ApiException.conflict("La mesa " + table.getTableNumber() + " está ocupada.");
        }
        order.setTableId(table.getId());
        order.setOrderType(Codes.DINE_IN);
        table.setStatus(Codes.OCCUPIED);
        TableOccupancy row = new TableOccupancy();
        row.setTableId(table.getId());
        row.setOrderId(order.getId());
        row.setWaiterId(order.getWaiterId());
        row.setStatus(Codes.OCCUPIED);
        row.setOpenedAt(Instant.now());
        occupancy.save(row);
    }

    private void releaseTable(SalesOrder order, String status) {
        if (order.getTableId() == null) {
            return;
        }
        markTable(order.getTableId(), status);
        occupancy.findByTableIdOrderByOpenedAtDesc(order.getTableId()).stream()
                .filter(row -> order.getId().equals(row.getOrderId()) && row.getClosedAt() == null)
                .findFirst()
                .ifPresent(row -> row.setClosedAt(Instant.now()));
    }

    private void markTable(Long tableId, String status) {
        tables.findById(tableId).ifPresent(table -> table.setStatus(status));
    }

    private void cancelTicketLines(Long lineId, String reason) {
        for (KitchenTicketLine ticketLine : ticketLines.findByOrderLineId(lineId)) {
            ticketLine.setStatus(Codes.CANCELLED);
            ticketLine.setNotes(reason);
            refreshTicket(ticketLine.getTicketId());
        }
    }

    private void markTicketLines(Long lineId, String status) {
        Instant now = Instant.now();
        for (KitchenTicketLine ticketLine : ticketLines.findByOrderLineId(lineId)) {
            ticketLine.setStatus(status);
            KitchenTicket ticket = tickets.findById(ticketLine.getTicketId()).orElseThrow();
            if (Codes.DELIVERED.equals(status)) {
                ticket.setDeliveredAt(now);
            }
            refreshTicket(ticket.getId());
        }
    }

    private void refreshTicket(Long ticketId) {
        List<KitchenTicketLine> items = ticketLines.findByTicketId(ticketId);
        KitchenTicket ticket = tickets.findById(ticketId).orElseThrow();
        if (items.stream().allMatch(item -> Codes.CANCELLED.equals(item.getStatus()))) {
            ticket.setStatus(Codes.CANCELLED);
        } else if (items.stream().filter(item -> !Codes.CANCELLED.equals(item.getStatus())).allMatch(item -> Codes.DELIVERED.equals(item.getStatus()))) {
            ticket.setStatus(Codes.DELIVERED);
        }
    }

    private void event(SalesOrder order, String type, String message) {
        SalesOrderEvent row = new SalesOrderEvent();
        row.setOrderId(order.getId());
        row.setEventType(type);
        row.setMessage(message);
        row.setUserId(support.user().getId());
        events.save(row);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.restosuite.repo.KitchenStationRepository stations;

    @org.springframework.beans.factory.annotation.Autowired
    private com.restosuite.repo.BranchRepository branches;

    private String stationName(Long stationId) {
        if (stationId == null) {
            return "General";
        }
        return stations.findById(stationId).map(station -> station.getName()).orElse("General");
    }

    private String display(SalesOrderLine line) {
        return line.getVariantName() == null ? line.getProductName() : line.getProductName() + " (" + line.getVariantName() + ")";
    }

    private List<Long> branchIds() {
        return branches.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId()).stream()
                .map(com.restosuite.domain.Branch::getId).toList();
    }

    private OrderDto.Summary summary(SalesOrder order) {
        return new OrderDto.Summary(order.getId(), order.getVersion(), order.getOrderNumber(), order.getOrderType(),
                order.getPreparationStatus(), order.getAccountStatus(), order.getTotalAmount(), order.getPaidAmount(),
                order.getOpenedAt(), tableLabel(order.getTableId()), order.getCustomerName());
    }

    public OrderDto.OrderView view(SalesOrder order) {
        List<OrderDto.LineView> lineViews = new ArrayList<>();
        for (SalesOrderLine line : lines.findByOrderIdOrderByIdAsc(order.getId())) {
            List<OrderDto.ModifierView> mods = lineModifiers.findByOrderLineId(line.getId()).stream()
                    .map(mod -> new OrderDto.ModifierView(mod.getId(), mod.getName(), mod.getUnitPrice())).toList();
            lineViews.add(new OrderDto.LineView(line.getId(), line.getProductId(), line.getVariantId(), line.getProductName(),
                    line.getVariantName(), line.getUnitPrice(), line.getModifierAmount(), line.getQuantity(), line.getEnteredAmount(),
                    line.getGrossAmount(), line.getDiscountAmount(), line.getNetAmount(), line.getTaxRate(), line.getTaxAmount(),
                    line.getTotalAmount(), line.getCostAmount(), line.isInventoryDeducted(), line.isRestocked(), line.getStatus(),
                    line.getNotes(), line.getStationName(), mods));
        }
        List<OrderDto.PaymentView> paymentViews = payments.findByOrderIdOrderByIdAsc(order.getId()).stream().map(payment -> {
            String name = cash.methodLabel(payment.getMethodId());
            return new OrderDto.PaymentView(payment.getId(), payment.getMethodId(), name, payment.getAmount(), payment.getTipAmount(),
                    payment.getReference(), payment.getStatus(), payment.getPaidAt());
        }).toList();
        List<OrderDto.RefundView> refundViews = refunds.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(refund -> new OrderDto.RefundView(refund.getId(), refund.getPaymentId(), refund.getAmount(), refund.getNetAmount(),
                        refund.getTaxAmount(), refund.isRestock(), refund.getReason(), refund.getCreatedAt())).toList();
        List<OrderDto.EventView> eventViews = events.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(row -> new OrderDto.EventView(row.getEventType(), row.getMessage(), row.getCreatedAt())).toList();
        String waiter = order.getWaiterId() == null ? null : users.findById(order.getWaiterId()).map(UserAccount::getFullName).orElse(null);
        return new OrderDto.OrderView(order.getId(), order.getVersion(), order.getBranchId(), order.getOrderNumber(), order.getOrderType(),
                order.getTableId(), tableLabel(order.getTableId()), order.getWaiterId(), waiter, order.getCustomerName(),
                order.getCustomerPhone(), order.getDeliveryAddress(), order.getGuestCount(), order.getPreparationStatus(),
                order.getAccountStatus(), order.isPricesIncludeTax(), order.getInventoryTrigger(), order.getGrossAmount(),
                order.getDiscountAmount(), order.getNetAmount(), order.getTaxAmount(), order.getTotalAmount(), order.getPaidAmount(),
                order.getTotalAmount().subtract(order.getPaidAmount()), order.getTipAmount(), order.getHeaderDiscount(), order.getNotes(),
                order.getCancelReason(), order.getOpenedAt(), order.getClosedAt(), lineViews, paymentViews, refundViews, eventViews);
    }

    private String tableLabel(Long tableId) {
        if (tableId == null) {
            return null;
        }
        return tables.findById(tableId).map(table -> {
            String area = areas.findById(table.getAreaId()).map(DiningArea::getName).orElse("");
            return area + " " + table.getTableNumber();
        }).orElse(null);
    }
}
