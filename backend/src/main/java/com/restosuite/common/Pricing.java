package com.restosuite.common;

import java.math.BigDecimal;

/**
 * Precios exclusivos de impuesto por defecto. Si el pedido se abrió con precios inclusivos,
 * el importe capturado ya trae el impuesto y aquí se separa base e impuesto.
 * El impuesto trasladado no forma parte del ingreso contable.
 */
public final class Pricing {

    private Pricing() {
    }

    public record Parts(BigDecimal gross, BigDecimal discount, BigDecimal net, BigDecimal tax, BigDecimal total) {
    }

    public static Parts price(BigDecimal entered, BigDecimal discountInMode, BigDecimal rate, boolean pricesIncludeTax) {
        BigDecimal shelf = Money.money(entered);
        BigDecimal discountMode = Money.money(discountInMode);
        BigDecimal taxRate = rate == null ? BigDecimal.ZERO : rate;
        if (discountMode.compareTo(shelf) > 0) {
            throw ApiException.badRequest("El descuento de un artículo supera su importe. Reduzca el descuento.");
        }
        if (pricesIncludeTax) {
            BigDecimal payable = shelf.subtract(discountMode);
            BigDecimal divisor = BigDecimal.ONE.add(taxRate);
            BigDecimal gross = payableExclusive(shelf, divisor);
            BigDecimal net = payableExclusive(payable, divisor);
            BigDecimal tax = payable.subtract(net);
            BigDecimal discount = gross.subtract(net);
            return new Parts(gross, discount, net, tax, payable);
        }
        BigDecimal net = shelf.subtract(discountMode);
        BigDecimal tax = Money.money(net.multiply(taxRate));
        return new Parts(shelf, discountMode, net, tax, net.add(tax));
    }

    private static BigDecimal payableExclusive(BigDecimal inclusive, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return Money.money(inclusive);
        }
        return inclusive.divide(divisor, Money.SCALE, Money.ROUND);
    }
}
