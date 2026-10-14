package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.Money;
import com.restosuite.domain.GoodsReceipt;
import com.restosuite.domain.GoodsReceiptLine;
import com.restosuite.domain.Ingredient;
import com.restosuite.domain.PurchaseOrder;
import com.restosuite.domain.PurchaseOrderLine;
import com.restosuite.domain.Supplier;
import com.restosuite.repo.GoodsReceiptLineRepository;
import com.restosuite.repo.GoodsReceiptRepository;
import com.restosuite.repo.IngredientRepository;
import com.restosuite.repo.PurchaseOrderLineRepository;
import com.restosuite.repo.PurchaseOrderRepository;
import com.restosuite.repo.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseService {

    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final GoodsReceiptRepository receipts;
    private final GoodsReceiptLineRepository receiptLines;
    private final IngredientRepository ingredients;
    private final Support support;
    private final SequenceService sequences;
    private final InventoryService inventory;
    private final CashService cash;
    private final AuditService audit;

    public PurchaseService(SupplierRepository suppliers, PurchaseOrderRepository orders, PurchaseOrderLineRepository lines,
                           GoodsReceiptRepository receipts, GoodsReceiptLineRepository receiptLines, IngredientRepository ingredients,
                           Support support, SequenceService sequences, InventoryService inventory, CashService cash, AuditService audit) {
        this.suppliers = suppliers;
        this.orders = orders;
        this.lines = lines;
        this.receipts = receipts;
        this.receiptLines = receiptLines;
        this.ingredients = ingredients;
        this.support = support;
        this.sequences = sequences;
        this.inventory = inventory;
        this.cash = cash;
        this.audit = audit;
    }

    public List<Supplier> suppliers() {
        return suppliers.findByRestaurantIdOrderByNameAsc(support.user().getRestaurantId());
    }

    @Transactional
    public Supplier saveSupplier(Supplier input) {
        Supplier supplier = input.getId() == null ? new Supplier() : suppliers.findByIdAndRestaurantId(input.getId(), support.user().getRestaurantId())
                .orElseThrow(() -> ApiException.notFound("El proveedor"));
        supplier.setRestaurantId(support.user().getRestaurantId());
        supplier.setName(Support.required(input.getName(), "El nombre", 160));
        supplier.setTaxId(Support.clean(input.getTaxId(), 40));
        supplier.setPhone(Support.clean(input.getPhone(), 40));
        supplier.setEmail(Support.clean(input.getEmail(), 160));
        supplier.setAddress(Support.clean(input.getAddress(), 300));
        supplier.setNotes(Support.clean(input.getNotes(), 400));
        supplier.setActive(input.getId() == null || input.isActive());
        return suppliers.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> list(Long branchId) {
        Long resolved = support.access().resolveBranch(branchId);
        return orders.findAll().stream()
                .filter(order -> resolved == null || order.getBranchId().equals(resolved))
                .filter(order -> support.user().getRestaurantId().equals(branchRestaurant(order.getBranchId())))
                .toList();
    }

    public PurchaseDetail get(Long id) {
        PurchaseOrder order = lockReadable(id);
        return new PurchaseDetail(order, lines.findByPurchaseOrderId(id), receipts.findByPurchaseOrderId(id));
    }

    @Transactional
    public PurchaseOrder create(Long branchId, Long supplierId, Long warehouseId, LocalDate expected, String notes, List<LineInput> items) {
        support.access().checkBranch(branchId);
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("Agregue al menos un artículo a la orden de compra.");
        }
        suppliers.findByIdAndRestaurantId(supplierId, support.user().getRestaurantId()).orElseThrow(() -> ApiException.notFound("El proveedor"));
        PurchaseOrder order = new PurchaseOrder();
        order.setBranchId(branchId);
        order.setSupplierId(supplierId);
        order.setWarehouseId(warehouseId);
        order.setNumber(sequences.next(branchId, Codes.DOC_PURCHASE));
        order.setStatus(Codes.DRAFT);
        order.setOrderDate(LocalDate.now(support.zone()));
        order.setExpectedDate(expected);
        order.setNotes(Support.clean(notes, 400));
        order.setCreatedBy(support.user().getId());
        order.setPaidAmount(Money.ZERO);
        orders.saveAndFlush(order);
        for (LineInput item : items) {
            Ingredient ingredient = ingredients.findByIdAndRestaurantId(item.ingredientId(), support.user().getRestaurantId())
                    .orElseThrow(() -> ApiException.notFound("El ingrediente"));
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrderId(order.getId());
            line.setIngredientId(ingredient.getId());
            line.setQuantityOrdered(Money.qty(item.quantity()));
            line.setQuantityReceived(Money.qty(BigDecimal.ZERO));
            line.setUnitCost(Money.cost(item.unitCost()));
            line.setTaxRate(item.taxRate() == null ? BigDecimal.ZERO : item.taxRate());
            lines.save(line);
        }
        totals(order);
        audit.record("PO_CREATE", "PURCHASE", order.getId(), order.getNumber());
        return order;
    }

    @Transactional
    public PurchaseOrder markOrdered(Long id, Long version) {
        PurchaseOrder order = lock(id, version);
        if (!Codes.DRAFT.equals(order.getStatus())) {
            throw ApiException.badRequest("Solo una orden en borrador puede enviarse al proveedor.");
        }
        order.setStatus(Codes.ORDERED);
        audit.record("PO_ORDER", "PURCHASE", order.getId(), order.getNumber());
        return order;
    }

    @Transactional
    public PurchaseOrder receive(Long id, Long version, String idempotencyKey, String documentRef, List<ReceiveInput> items) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("Envíe Idempotency-Key para no duplicar la recepción.");
        }
        var existing = receipts.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().getPurchaseOrderId().equals(id) ? lockReadable(id)
                    : throwConflict();
        }
        PurchaseOrder order = lock(id, version);
        if (!List.of(Codes.ORDERED, Codes.PARTIAL).contains(order.getStatus())) {
            throw ApiException.badRequest("La orden debe estar enviada para recibir mercancía.");
        }
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrderId(order.getId());
        receipt.setWarehouseId(order.getWarehouseId());
        receipt.setDocumentRef(Support.clean(documentRef, 80));
        receipt.setReceivedBy(support.user().getId());
        receipt.setIdempotencyKey(idempotencyKey);
        receipt.setReceivedAt(Instant.now());
        receipts.saveAndFlush(receipt);
        boolean any = false;
        for (ReceiveInput item : items) {
            PurchaseOrderLine line = lines.findById(item.lineId()).orElseThrow(() -> ApiException.notFound("La línea"));
            if (!line.getPurchaseOrderId().equals(order.getId())) {
                throw ApiException.badRequest("La línea no pertenece a esta orden.");
            }
            BigDecimal qty = Money.qty(item.quantity());
            BigDecimal remaining = line.getQuantityOrdered().subtract(line.getQuantityReceived());
            if (qty.compareTo(remaining) > 0) {
                throw ApiException.badRequest("No puede recibir más de lo pendiente en " + line.getIngredientId() + ". Pendiente: " + remaining + ".");
            }
            line.setQuantityReceived(line.getQuantityReceived().add(qty));
            GoodsReceiptLine receiptLine = new GoodsReceiptLine();
            receiptLine.setReceiptId(receipt.getId());
            receiptLine.setPurchaseOrderLineId(line.getId());
            receiptLine.setQuantity(qty);
            receiptLine.setUnitCost(line.getUnitCost());
            receiptLines.save(receiptLine);
            inventory.move(order.getBranchId(), order.getWarehouseId(), line.getIngredientId(), Codes.PURCHASE, Codes.IN,
                    qty, line.getUnitCost(), "Recepción " + order.getNumber());
            any = true;
        }
        if (!any) {
            throw ApiException.badRequest("Indique cantidades recibidas mayores que cero.");
        }
        boolean complete = lines.findByPurchaseOrderId(order.getId()).stream()
                .allMatch(line -> line.getQuantityReceived().compareTo(line.getQuantityOrdered()) >= 0);
        order.setStatus(complete ? Codes.RECEIVED : Codes.PARTIAL);
        audit.record("PO_RECEIVE", "PURCHASE", order.getId(), order.getNumber());
        return order;
    }

    @Transactional
    public PurchaseOrder pay(Long id, Long version, BigDecimal amount, Long methodId) {
        PurchaseOrder order = lock(id, version);
        BigDecimal value = Money.money(amount);
        if (value.signum() <= 0) {
            throw ApiException.badRequest("El pago al proveedor debe ser mayor que cero.");
        }
        if (order.getPaidAmount().add(value).compareTo(order.getTotalAmount()) > 0) {
            throw ApiException.badRequest("El pago supera el total de la orden. Pendiente: " + order.getTotalAmount().subtract(order.getPaidAmount()) + ".");
        }
        var method = cash.method(methodId);
        var session = cash.requireOpen(order.getBranchId());
        cash.record(session, Codes.SUPPLIER_PAYMENT, Codes.OUT, value, method.isAffectsCash(), method.getId(),
                "PURCHASE", order.getId().toString(), "Pago " + order.getNumber(), null);
        order.setPaidAmount(order.getPaidAmount().add(value));
        audit.record("PO_PAY", "PURCHASE", order.getId(), value.toPlainString());
        return order;
    }

    @Transactional
    public PurchaseOrder cancel(Long id, Long version, String reason) {
        PurchaseOrder order = lock(id, version);
        if (lines.findByPurchaseOrderId(id).stream().anyMatch(line -> line.getQuantityReceived().signum() > 0)) {
            throw ApiException.conflict("Ya hay mercancía recibida. No puede cancelar la orden; registre una devolución al proveedor.");
        }
        if (!List.of(Codes.DRAFT, Codes.ORDERED).contains(order.getStatus())) {
            throw ApiException.badRequest("Esta orden ya no se puede cancelar.");
        }
        order.setStatus(Codes.CANCELLED);
        order.setNotes(Support.required(reason, "El motivo", 300));
        audit.record("PO_CANCEL", "PURCHASE", order.getId(), reason);
        return order;
    }

    private void totals(PurchaseOrder order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (PurchaseOrderLine line : lines.findByPurchaseOrderId(order.getId())) {
            BigDecimal net = Money.money(line.getQuantityOrdered().multiply(line.getUnitCost()));
            BigDecimal lineTax = Money.money(net.multiply(line.getTaxRate()));
            subtotal = subtotal.add(net);
            tax = tax.add(lineTax);
        }
        order.setSubtotal(Money.money(subtotal));
        order.setTaxAmount(Money.money(tax));
        order.setTotalAmount(order.getSubtotal().add(order.getTaxAmount()));
    }

    private PurchaseOrder lock(Long id, Long version) {
        PurchaseOrder order = orders.lockById(id).orElseThrow(() -> ApiException.notFound("La orden de compra"));
        support.access().checkBranch(order.getBranchId());
        support.checkVersion(order.getVersion(), version);
        return order;
    }

    private PurchaseOrder lockReadable(Long id) {
        PurchaseOrder order = orders.findById(id).orElseThrow(() -> ApiException.notFound("La orden de compra"));
        support.access().checkBranch(order.getBranchId());
        return order;
    }

    private Long branchRestaurant(Long branchId) {
        return support.restaurant().getId();
    }

    private PurchaseOrder throwConflict() {
        throw ApiException.conflict("Esta clave de idempotencia ya se usó en otra recepción.");
    }

    public record LineInput(Long ingredientId, BigDecimal quantity, BigDecimal unitCost, BigDecimal taxRate) {
    }

    public record ReceiveInput(Long lineId, BigDecimal quantity) {
    }

    public record PurchaseDetail(PurchaseOrder order, List<PurchaseOrderLine> lines, List<GoodsReceipt> receipts) {
    }
}
