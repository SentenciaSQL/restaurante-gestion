package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.domain.KitchenTicket;
import com.restosuite.domain.KitchenTicketLine;
import com.restosuite.domain.SalesOrder;
import com.restosuite.domain.SalesOrderLine;
import com.restosuite.repo.KitchenTicketLineRepository;
import com.restosuite.repo.KitchenTicketRepository;
import com.restosuite.repo.SalesOrderLineRepository;
import com.restosuite.repo.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class KitchenService {

    private final KitchenTicketRepository tickets;
    private final KitchenTicketLineRepository ticketLines;
    private final SalesOrderRepository orders;
    private final SalesOrderLineRepository lines;
    private final OrderWorkflow workflow;
    private final Support support;
    private final KitchenHub hub;
    private final AuditService audit;

    public KitchenService(KitchenTicketRepository tickets, KitchenTicketLineRepository ticketLines, SalesOrderRepository orders,
                          SalesOrderLineRepository lines, OrderWorkflow workflow, Support support, KitchenHub hub, AuditService audit) {
        this.tickets = tickets;
        this.ticketLines = ticketLines;
        this.orders = orders;
        this.lines = lines;
        this.workflow = workflow;
        this.support = support;
        this.hub = hub;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TicketView> board(Long branchId, boolean history) {
        support.access().checkBranch(branchId);
        Instant now = Instant.now();
        double factor = Double.parseDouble(support.setting("kitchen.delay_factor", "1.25"));
        List<TicketView> views = new ArrayList<>();
        for (KitchenTicket ticket : tickets.findAll()) {
            if (!ticket.getBranchId().equals(branchId)) {
                continue;
            }
            boolean open = List.of(Codes.QUEUED, Codes.IN_PROGRESS, Codes.READY).contains(ticket.getStatus());
            if (history == open) {
                continue;
            }
            views.add(toView(ticket, now, factor));
        }
        views.sort((a, b) -> a.receivedAt().compareTo(b.receivedAt()));
        return views;
    }

    @Transactional
    public TicketView advance(Long ticketId, String target) {
        KitchenTicket ticket = tickets.findById(ticketId).orElseThrow(() -> ApiException.notFound("La comanda"));
        support.access().checkBranch(ticket.getBranchId());
        if (target == null || target.isBlank()) {
            target = switch (ticket.getStatus()) {
                case Codes.QUEUED -> Codes.IN_PROGRESS;
                case Codes.IN_PROGRESS -> Codes.READY;
                case Codes.READY -> Codes.DELIVERED;
                default -> throw ApiException.badRequest("Esta comanda ya no puede avanzar.");
            };
        }
        Instant now = Instant.now();
        if (Codes.IN_PROGRESS.equals(target)) {
            if (!Codes.QUEUED.equals(ticket.getStatus())) {
                throw ApiException.badRequest("Solo una comanda recibida puede pasar a preparación.");
            }
            ticket.setStatus(Codes.IN_PROGRESS);
            ticket.setStartedAt(now);
            setLines(ticket, Codes.IN_PROGRESS);
        } else if (Codes.READY.equals(target)) {
            if (!Codes.IN_PROGRESS.equals(ticket.getStatus()) && !Codes.QUEUED.equals(ticket.getStatus())) {
                throw ApiException.badRequest("La comanda no está en preparación.");
            }
            ticket.setStatus(Codes.READY);
            ticket.setReadyAt(now);
            if (ticket.getStartedAt() == null) {
                ticket.setStartedAt(now);
            }
            setLines(ticket, Codes.READY);
        } else if (Codes.DELIVERED.equals(target)) {
            ticket.setStatus(Codes.DELIVERED);
            ticket.setDeliveredAt(now);
            setLines(ticket, Codes.DELIVERED);
        } else {
            throw ApiException.badRequest("El estado de cocina solicitado no es válido.");
        }
        SalesOrder order = orders.findById(ticket.getOrderId()).orElseThrow();
        workflow.refreshPreparation(order, lines.findByOrderIdOrderByIdAsc(order.getId()));
        audit.record("KITCHEN_" + target, "KITCHEN_TICKET", ticket.getId(), order.getOrderNumber());
        hub.publish(ticket.getBranchId());
        return toView(ticket, now, Double.parseDouble(support.setting("kitchen.delay_factor", "1.25")));
    }

    private void setLines(KitchenTicket ticket, String status) {
        Instant now = Instant.now();
        for (KitchenTicketLine ticketLine : ticketLines.findByTicketId(ticket.getId())) {
            if (Codes.CANCELLED.equals(ticketLine.getStatus())) {
                continue;
            }
            ticketLine.setStatus(status);
            SalesOrderLine line = lines.findById(ticketLine.getOrderLineId()).orElse(null);
            if (line == null || Codes.CANCELLED.equals(line.getStatus())) {
                continue;
            }
            if (Codes.IN_PROGRESS.equals(status) && (Codes.SENT.equals(line.getStatus()) || Codes.PENDING.equals(line.getStatus()))) {
                line.setStatus(Codes.PREPARING);
            } else if (Codes.READY.equals(status) && !Codes.DELIVERED.equals(line.getStatus())) {
                line.setStatus(Codes.READY);
                line.setReadyAt(now);
            } else if (Codes.DELIVERED.equals(status)) {
                line.setStatus(Codes.DELIVERED);
                line.setDeliveredAt(now);
            }
        }
    }

    private TicketView toView(KitchenTicket ticket, Instant now, double factor) {
        SalesOrder order = orders.findById(ticket.getOrderId()).orElseThrow();
        List<ItemView> items = new ArrayList<>();
        int prep = 0;
        for (KitchenTicketLine ticketLine : ticketLines.findByTicketId(ticket.getId())) {
            SalesOrderLine line = lines.findById(ticketLine.getOrderLineId()).orElse(null);
            String notes = ticketLine.getNotes();
            items.add(new ItemView(ticketLine.getId(), ticketLine.getProductName(), ticketLine.getQuantity(), notes, ticketLine.getStatus()));
            if (line != null && line.getProductId() != null) {
                prep = Math.max(prep, 10);
            }
        }
        long elapsed = Duration.between(ticket.getReceivedAt(), ticket.getReadyAt() == null ? now : ticket.getReadyAt()).toMinutes();
        int threshold = (int) Math.ceil(Math.max(prep, 15) * factor);
        return new TicketView(ticket.getId(), ticket.getOrderId(), order.getOrderNumber(), order.getTableId(), ticket.getStationName(),
                ticket.getStatus(), ticket.getReceivedAt(), elapsed, elapsed > threshold && !Codes.DELIVERED.equals(ticket.getStatus())
                && !Codes.CANCELLED.equals(ticket.getStatus()), threshold, items);
    }

    public record ItemView(Long id, String name, java.math.BigDecimal quantity, String notes, String status) {
    }

    public record TicketView(Long id, Long orderId, String orderNumber, Long tableId, String stationName, String status,
                             Instant receivedAt, long elapsedMinutes, boolean delayed, int delayAfterMinutes, List<ItemView> items) {
    }
}
