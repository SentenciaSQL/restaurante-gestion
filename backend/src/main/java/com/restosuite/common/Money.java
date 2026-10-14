package com.restosuite.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class Money {

    public static final int SCALE = 2;
    public static final int QTY_SCALE = 3;
    public static final int COST_SCALE = 4;
    public static final RoundingMode ROUND = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUND);
    public static final BigDecimal CENT = new BigDecimal("0.01");

    private Money() {
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(SCALE, ROUND);
    }

    public static BigDecimal qty(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QTY_SCALE, ROUND);
        }
        return value.setScale(QTY_SCALE, ROUND);
    }

    public static BigDecimal cost(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(COST_SCALE, ROUND);
        }
        return value.setScale(COST_SCALE, ROUND);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean greaterThan(BigDecimal left, BigDecimal right) {
        return money(left).compareTo(money(right)) > 0;
    }

    /**
     * Reparte un importe entre topes, en centavos, sin superar cada tope ni perder centavos.
     */
    public static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> caps) {
        BigDecimal target = money(amount);
        if (caps.isEmpty()) {
            if (target.signum() != 0) {
                throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "DISCOUNT",
                        "No hay artículos sobre los que aplicar el descuento.");
            }
            return List.of();
        }
        BigDecimal capSum = caps.stream().map(Money::money).reduce(ZERO, BigDecimal::add);
        if (target.compareTo(capSum) > 0) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "DISCOUNT",
                    "El descuento supera el importe de los artículos. Reduzca el descuento e intente de nuevo.");
        }
        BigDecimal[] shares = new BigDecimal[caps.size()];
        BigDecimal used = ZERO;
        for (int i = 0; i < caps.size(); i++) {
            BigDecimal cap = money(caps.get(i));
            BigDecimal share;
            if (i == caps.size() - 1) {
                share = target.subtract(used);
            } else if (capSum.signum() == 0) {
                share = ZERO;
            } else {
                share = money(target.multiply(cap).divide(capSum, 8, ROUND));
            }
            if (share.compareTo(cap) > 0) {
                share = cap;
            }
            if (share.signum() < 0) {
                share = ZERO;
            }
            shares[i] = share;
            used = used.add(share);
        }
        BigDecimal drift = target.subtract(used);
        int guard = 0;
        while (drift.signum() != 0 && guard++ < 100000) {
            boolean moved = false;
            for (int i = 0; i < shares.length && drift.signum() != 0; i++) {
                BigDecimal cap = money(caps.get(i));
                if (drift.signum() > 0 && cap.subtract(shares[i]).compareTo(CENT) >= 0) {
                    shares[i] = shares[i].add(CENT);
                    drift = drift.subtract(CENT);
                    moved = true;
                } else if (drift.signum() < 0 && shares[i].compareTo(CENT) >= 0) {
                    shares[i] = shares[i].subtract(CENT);
                    drift = drift.add(CENT);
                    moved = true;
                }
            }
            if (!moved) {
                break;
            }
        }
        if (drift.signum() != 0) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "DISCOUNT",
                    "No se pudo repartir el descuento sin superar el importe de un artículo. Ajuste el descuento.");
        }
        return List.of(shares);
    }
}
