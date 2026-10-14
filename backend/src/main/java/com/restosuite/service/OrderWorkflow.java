package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.common.Pricing;
import com.restosuite.domain.SalesOrder;
import com.restosuite.domain.SalesOrderLine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderWorkflow {

    public void recalculate(SalesOrder order, List<SalesOrderLine> lines) {
        List<SalesOrderLine> active = lines.stream().filter(line -> !Codes.CANCELLED.equals(line.getStatus())).toList();
        for (SalesOrderLine line : lines) {
            if (Codes.CANCELLED.equals(line.getStatus())) {
                line.setEnteredAmount(Money.ZERO);
                line.setGrossAmount(Money.ZERO);
                line.setDiscountAmount(Money.ZERO);
                line.setNetAmount(Money.ZERO);
                line.setTaxAmount(Money.ZERO);
                line.setTotalAmount(Money.ZERO);
            } else {
                BigDecimal entered = Money.money(line.getUnitPrice().add(line.getModifierAmount()).multiply(line.getQuantity()));
                line.setEnteredAmount(entered);
            }
        }
        List<BigDecimal> caps = active.stream().map(SalesOrderLine::getEnteredAmount).toList();
        BigDecimal header = Money.money(order.getHeaderDiscount());
        List<BigDecimal> allocated = active.isEmpty() ? List.of() : Money.allocate(header, caps);
        for (int i = 0; i < active.size(); i++) {
            SalesOrderLine line = active.get(i);
            BigDecimal modeDiscount = Money.money(line.getLineDiscount()).add(allocated.get(i));
            Pricing.Parts parts = Pricing.price(line.getEnteredAmount(), modeDiscount, line.getTaxRate(), order.isPricesIncludeTax());
            line.setGrossAmount(parts.gross());
            line.setDiscountAmount(parts.discount());
            line.setNetAmount(parts.net());
            line.setTaxAmount(parts.tax());
            line.setTotalAmount(parts.total());
        }
        order.setGrossAmount(sum(active, SalesOrderLine::getGrossAmount));
        order.setDiscountAmount(sum(active, SalesOrderLine::getDiscountAmount));
        order.setNetAmount(sum(active, SalesOrderLine::getNetAmount));
        order.setTaxAmount(sum(active, SalesOrderLine::getTaxAmount));
        order.setTotalAmount(sum(active, SalesOrderLine::getTotalAmount));
        if (Money.money(order.getPaidAmount()).compareTo(order.getTotalAmount()) > 0) {
            throw ApiException.conflict("El total quedó por debajo de lo ya cobrado. Anule o reembolse el excedente antes de continuar.");
        }
        refreshPreparation(order, lines);
    }

    public void refreshPreparation(SalesOrder order, List<SalesOrderLine> lines) {
        if (Codes.CANCELLED.equals(order.getAccountStatus())) {
            order.setPreparationStatus(Codes.CANCELLED);
            return;
        }
        List<SalesOrderLine> active = new ArrayList<>();
        for (SalesOrderLine line : lines) {
            if (!Codes.CANCELLED.equals(line.getStatus())) {
                active.add(line);
            }
        }
        if (active.isEmpty()) {
            order.setPreparationStatus(lines.isEmpty() ? Codes.DRAFT : Codes.CANCELLED);
            return;
        }
        boolean pending = active.stream().anyMatch(line -> Codes.PENDING.equals(line.getStatus()));
        boolean preparing = active.stream().anyMatch(line -> Codes.PREPARING.equals(line.getStatus()));
        boolean sent = active.stream().anyMatch(line -> Codes.SENT.equals(line.getStatus()));
        boolean allDelivered = active.stream().allMatch(line -> Codes.DELIVERED.equals(line.getStatus()));
        boolean ready = active.stream().allMatch(line -> Codes.READY.equals(line.getStatus()) || Codes.DELIVERED.equals(line.getStatus()));
        if (allDelivered) {
            order.setPreparationStatus(Codes.DELIVERED);
        } else if (preparing) {
            order.setPreparationStatus(Codes.IN_PREPARATION);
        } else if (ready) {
            order.setPreparationStatus(Codes.READY);
        } else if (sent || active.stream().anyMatch(line -> Codes.READY.equals(line.getStatus()) || Codes.DELIVERED.equals(line.getStatus()))) {
            order.setPreparationStatus(pending ? Codes.PENDING_SEND : Codes.SENT);
        } else {
            order.setPreparationStatus(Codes.PENDING_SEND);
        }
    }

    private BigDecimal sum(List<SalesOrderLine> lines, java.util.function.Function<SalesOrderLine, BigDecimal> getter) {
        return lines.stream().map(getter).map(Money::money).reduce(Money.ZERO, BigDecimal::add);
    }
}
