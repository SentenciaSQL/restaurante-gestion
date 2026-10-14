package com.restosuite.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDto {

    private OrderDto() {
    }

    public record Create(
            @NotNull Long branchId,
            @NotBlank String orderType,
            Long tableId,
            String customerName,
            String customerPhone,
            String deliveryAddress,
            Integer guestCount,
            String notes
    ) {
    }

    public record AddLine(
            @NotNull Long version,
            @NotNull Long productId,
            Long variantId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            List<Long> modifierIds,
            String notes
    ) {
    }

    public record UpdateLine(@NotNull Long version, @NotNull @DecimalMin("0.001") BigDecimal quantity, String notes) {
    }

    public record Reason(@NotNull Long version, @NotBlank String reason) {
    }

    public record Discount(
            @NotNull Long version,
            @NotBlank String discountType,
            @NotNull @DecimalMin("0.00") BigDecimal value,
            @NotBlank String reason
    ) {
    }

    public record Send(@NotNull Long version, List<Long> lineIds) {
    }

    public record Deliver(@NotNull Long version, List<Long> lineIds) {
    }

    public record Pay(
            @NotNull Long version,
            @NotNull Long methodId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @DecimalMin("0.00") BigDecimal tipAmount,
            String reference
    ) {
    }

    public record VoidPayment(@NotNull Long version, @NotBlank String reason) {
    }

    public record Refund(
            @NotNull Long version,
            @NotNull Long paymentId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String reason,
            boolean restock,
            List<Long> lineIds
    ) {
    }

    public record Transfer(@NotNull Long version, @NotNull Long tableId) {
    }

    public record Split(@NotNull Long version, @NotNull List<Long> lineIds, Long targetTableId) {
    }

    public record Merge(@NotNull Long version, @NotNull Long sourceOrderId) {
    }

    public record ModifierView(Long id, String name, BigDecimal unitPrice) {
    }

    public record LineView(
            Long id, Long productId, Long variantId, String productName, String variantName,
            BigDecimal unitPrice, BigDecimal modifierAmount, BigDecimal quantity, BigDecimal enteredAmount,
            BigDecimal grossAmount, BigDecimal discountAmount, BigDecimal netAmount, BigDecimal taxRate,
            BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal costAmount, boolean inventoryDeducted,
            boolean restocked, String status, String notes, String stationName, List<ModifierView> modifiers
    ) {
    }

    public record PaymentView(
            Long id, Long methodId, String methodName, BigDecimal amount, BigDecimal tipAmount,
            String reference, String status, Instant paidAt
    ) {
    }

    public record RefundView(Long id, Long paymentId, BigDecimal amount, BigDecimal netAmount, BigDecimal taxAmount,
                             boolean restock, String reason, Instant createdAt) {
    }

    public record EventView(String type, String message, Instant createdAt) {
    }

    public record OrderView(
            Long id, Long version, Long branchId, String orderNumber, String orderType, Long tableId,
            String tableLabel, Long waiterId, String waiterName, String customerName, String customerPhone,
            String deliveryAddress, Integer guestCount, String preparationStatus, String accountStatus,
            boolean pricesIncludeTax, String inventoryTrigger, BigDecimal grossAmount, BigDecimal discountAmount,
            BigDecimal netAmount, BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal paidAmount,
            BigDecimal balance, BigDecimal tipAmount, BigDecimal headerDiscount, String notes, String cancelReason,
            Instant openedAt, Instant closedAt, List<LineView> lines, List<PaymentView> payments,
            List<RefundView> refunds, List<EventView> events
    ) {
    }

    public record Summary(
            Long id, Long version, String orderNumber, String orderType, String preparationStatus, String accountStatus,
            BigDecimal totalAmount, BigDecimal paidAmount, Instant openedAt, String tableLabel, String customerName
    ) {
    }
}
