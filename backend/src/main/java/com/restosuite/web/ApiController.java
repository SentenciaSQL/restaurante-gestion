package com.restosuite.web;

import com.restosuite.domain.DiningTable;
import com.restosuite.domain.Expense;
import com.restosuite.domain.Product;
import com.restosuite.domain.ProductVariant;
import com.restosuite.domain.PurchaseOrder;
import com.restosuite.domain.RecipeLine;
import com.restosuite.dto.OrderDto;
import com.restosuite.service.CatalogService;
import com.restosuite.service.ExpenseService;
import com.restosuite.service.InventoryService;
import com.restosuite.service.KitchenHub;
import com.restosuite.service.KitchenService;
import com.restosuite.service.OrderService;
import com.restosuite.service.PurchaseService;
import com.restosuite.service.ReceiptService;
import com.restosuite.service.ReportService;
import com.restosuite.service.Support;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class ApiController {

    private final CatalogService catalog;
    private final OrderService orders;
    private final KitchenService kitchen;
    private final KitchenHub hub;
    private final InventoryService inventory;
    private final PurchaseService purchases;
    private final ExpenseService expenses;
    private final com.restosuite.service.CashService cash;
    private final ReportService reports;
    private final ReceiptService receipts;
    private final Support support;

    public ApiController(CatalogService catalog, OrderService orders, KitchenService kitchen, KitchenHub hub,
                         InventoryService inventory, PurchaseService purchases, ExpenseService expenses,
                         com.restosuite.service.CashService cash, ReportService reports, ReceiptService receipts, Support support) {
        this.catalog = catalog;
        this.orders = orders;
        this.kitchen = kitchen;
        this.hub = hub;
        this.inventory = inventory;
        this.purchases = purchases;
        this.expenses = expenses;
        this.cash = cash;
        this.reports = reports;
        this.receipts = receipts;
        this.support = support;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/settings/restaurant")
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('settings.operations') or hasAuthority('dashboard.view')")
    public Object restaurant() {
        return catalog.restaurant();
    }

    @PutMapping("/api/settings/restaurant")
    @PreAuthorize("hasAuthority('settings.manage')")
    public Object saveRestaurant(@RequestBody com.restosuite.domain.Restaurant body) {
        return catalog.saveRestaurant(body);
    }

    @GetMapping("/api/branches")
    public Object branches() {
        return catalog.branches();
    }

    @PostMapping("/api/branches")
    @PreAuthorize("hasAuthority('branches.manage')")
    public Object createBranch(@RequestBody com.restosuite.domain.Branch body) {
        body.setId(null);
        return catalog.saveBranch(body);
    }

    @PutMapping("/api/branches/{id}")
    @PreAuthorize("hasAuthority('branches.manage')")
    public Object updateBranch(@PathVariable Long id, @RequestBody com.restosuite.domain.Branch body) {
        body.setId(id);
        return catalog.saveBranch(body);
    }

    @GetMapping("/api/settings/taxes")
    public Object taxes() { return catalog.taxes(); }

    @PostMapping("/api/settings/taxes")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object saveTax(@RequestBody com.restosuite.domain.TaxRate body) { return catalog.saveTax(body); }

    @GetMapping("/api/settings/payment-methods")
    public Object paymentMethods() { return catalog.paymentMethods(); }

    @PostMapping("/api/settings/payment-methods")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object savePaymentMethod(@RequestBody com.restosuite.domain.PaymentMethod body) { return catalog.savePaymentMethod(body); }

    @GetMapping("/api/settings/units")
    public Object units() { return catalog.units(); }

    @PostMapping("/api/settings/units")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object saveUnit(@RequestBody com.restosuite.domain.UnitOfMeasure body) { return catalog.saveUnit(body); }

    @GetMapping("/api/settings/stations")
    public Object stations(@RequestParam Long branchId) { return catalog.stations(branchId); }

    @PostMapping("/api/settings/stations")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object saveStation(@RequestBody com.restosuite.domain.KitchenStation body) { return catalog.saveStation(body); }

    @GetMapping("/api/settings/parameters")
    @PreAuthorize("hasAuthority('settings.operations') or hasAuthority('settings.manage')")
    public Object parameters() { return catalog.parameters(); }

    @PutMapping("/api/settings/parameters")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object saveParameter(@RequestBody Map<String, String> body) {
        return catalog.saveParameter(body.get("key"), body.get("value"));
    }

    @GetMapping("/api/settings/sequences")
    public Object sequences(@RequestParam Long branchId) { return catalog.sequences(branchId); }

    @PutMapping("/api/settings/sequences")
    @PreAuthorize("hasAuthority('settings.manage')")
    public Object saveSequence(@RequestBody com.restosuite.domain.DocumentSequence body) { return catalog.saveSequence(body); }

    @GetMapping("/api/settings/expense-categories")
    public Object expenseCategories() { return catalog.expenseCategories(); }

    @PostMapping("/api/settings/expense-categories")
    @PreAuthorize("hasAuthority('settings.operations')")
    public Object saveExpenseCategory(@RequestBody com.restosuite.domain.ExpenseCategory body) { return catalog.saveExpenseCategory(body); }

    @GetMapping("/api/areas")
    @PreAuthorize("hasAuthority('tables.view')")
    public Object areas(@RequestParam Long branchId) { return catalog.areas(branchId); }

    @PostMapping("/api/areas")
    @PreAuthorize("hasAuthority('tables.manage')")
    public Object saveArea(@RequestBody com.restosuite.domain.DiningArea body) { return catalog.saveArea(body); }

    @GetMapping("/api/tables")
    @PreAuthorize("hasAuthority('tables.view')")
    public Object tables(@RequestParam Long branchId) { return catalog.tables(branchId); }

    @PostMapping("/api/tables")
    @PreAuthorize("hasAuthority('tables.manage')")
    public Object saveTable(@RequestBody DiningTable body) { return catalog.saveTable(body); }

    @PostMapping("/api/tables/{id}/assign")
    @PreAuthorize("hasAuthority('tables.operate')")
    public Object assign(@PathVariable Long id, @RequestBody WaiterAssignment body) {
        return catalog.assignWaiter(id, body.waiterId());
    }

    @GetMapping("/api/tables/{id}/open-orders")
    @PreAuthorize("hasAuthority('orders.view') or hasAuthority('tables.view')")
    public Object openOrders(@PathVariable Long id) {
        return orders.openOnTable(id);
    }

    @PostMapping("/api/tables/{id}/release")
    @PreAuthorize("hasAuthority('tables.operate')")
    public Object release(@PathVariable Long id) { return catalog.release(id); }

    @PostMapping("/api/tables/{id}/reservations")
    @PreAuthorize("hasAuthority('tables.operate')")
    public Object reserve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return catalog.reserve(id, body.get("customerName"), body.get("phone"),
                body.get("partySize") == null ? 1 : Integer.parseInt(body.get("partySize")),
                Instant.parse(body.get("from")), Instant.parse(body.get("until")), body.get("notes"));
    }

    @GetMapping("/api/tables/{id}/history")
    @PreAuthorize("hasAuthority('tables.view')")
    public Object history(@PathVariable Long id) { return catalog.history(id); }

    @GetMapping("/api/menu/categories")
    @PreAuthorize("hasAuthority('menu.view')")
    public Object categories() { return catalog.categories(); }

    @PostMapping("/api/menu/categories")
    @PreAuthorize("hasAuthority('menu.manage')")
    public Object saveCategory(@RequestBody com.restosuite.domain.MenuCategory body) { return catalog.saveCategory(body); }

    @GetMapping("/api/menu/products")
    @PreAuthorize("hasAuthority('menu.view')")
    public Object products(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return includeInactive ? catalog.allProducts() : catalog.products();
    }

    @GetMapping("/api/menu/products/{id}")
    @PreAuthorize("hasAuthority('menu.view')")
    public Object product(@PathVariable Long id) { return catalog.product(id); }

    @PostMapping("/api/menu/products")
    @PreAuthorize("hasAuthority('menu.manage')")
    public Object saveProduct(@RequestBody ProductWrite body) {
        return catalog.saveProduct(body.product(), body.variants(), body.modifierIds(), body.recipe());
    }

    @GetMapping("/api/menu/modifiers")
    @PreAuthorize("hasAuthority('menu.view')")
    public Object modifiers() { return catalog.modifiers(); }

    @PostMapping("/api/menu/modifiers")
    @PreAuthorize("hasAuthority('menu.manage')")
    public Object saveModifier(@RequestBody com.restosuite.domain.Modifier body) { return catalog.saveModifier(body); }

    @GetMapping("/api/menu/products/{id}/variants")
    @PreAuthorize("hasAuthority('menu.view')")
    public Object variants(@PathVariable Long id) { return catalog.variants(id); }

    @PostMapping("/api/orders")
    @PreAuthorize("hasAuthority('orders.create')")
    public Object createOrder(@RequestBody OrderDto.Create body) { return orders.create(body); }

    @GetMapping("/api/orders")
    @PreAuthorize("hasAuthority('orders.view')")
    public Object listOrders(@RequestParam(required = false) Long branchId, @RequestParam(required = false) String accountStatus,
                             @RequestParam(required = false) String orderType, @RequestParam(required = false) String q,
                             @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.list(branchId, accountStatus, orderType, q, from, to, page, size);
    }

    @GetMapping("/api/orders/{id}")
    @PreAuthorize("hasAuthority('orders.view')")
    public Object order(@PathVariable Long id) { return orders.get(id); }

    @PostMapping("/api/orders/{id}/lines")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object addLine(@PathVariable Long id, @RequestBody OrderDto.AddLine body) { return orders.addLine(id, body); }

    @PutMapping("/api/orders/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object updateLine(@PathVariable Long id, @PathVariable Long lineId, @RequestBody OrderDto.UpdateLine body) {
        return orders.updateLine(id, lineId, body);
    }

    @PostMapping("/api/orders/{id}/lines/{lineId}/cancel")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object cancelLine(@PathVariable Long id, @PathVariable Long lineId, @RequestBody OrderDto.Reason body) {
        return orders.cancelLine(id, lineId, body);
    }

    @PostMapping("/api/orders/{id}/discount")
    @PreAuthorize("hasAuthority('orders.discount')")
    public Object discount(@PathVariable Long id, @RequestBody OrderDto.Discount body) { return orders.discount(id, body); }

    @PostMapping("/api/orders/{id}/send")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object send(@PathVariable Long id, @RequestBody OrderDto.Send body) { return orders.send(id, body); }

    @PostMapping("/api/orders/{id}/deliver")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object deliver(@PathVariable Long id, @RequestBody OrderDto.Deliver body) { return orders.deliver(id, body); }

    @PostMapping("/api/orders/{id}/payments")
    @PreAuthorize("hasAuthority('payments.collect')")
    public Object pay(@PathVariable Long id, @RequestBody OrderDto.Pay body, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return orders.pay(id, body, key);
    }

    @PostMapping("/api/orders/{id}/payments/{paymentId}/void")
    @PreAuthorize("hasAuthority('payments.refund')")
    public Object voidPayment(@PathVariable Long id, @PathVariable Long paymentId, @RequestBody OrderDto.VoidPayment body) {
        return orders.voidPayment(id, paymentId, body);
    }

    @PostMapping("/api/orders/{id}/refunds")
    @PreAuthorize("hasAuthority('payments.refund')")
    public Object refund(@PathVariable Long id, @RequestBody OrderDto.Refund body, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return orders.refund(id, body, key);
    }

    @PostMapping("/api/orders/{id}/close")
    @PreAuthorize("hasAuthority('orders.close')")
    public Object close(@PathVariable Long id, @RequestBody OrderDto.Reason body) { return orders.close(id, body.version()); }

    @PostMapping("/api/orders/{id}/cancel")
    @PreAuthorize("hasAuthority('orders.cancel')")
    public Object cancel(@PathVariable Long id, @RequestBody OrderDto.Reason body) { return orders.cancel(id, body); }

    @PostMapping("/api/orders/{id}/transfer")
    @PreAuthorize("hasAuthority('tables.operate')")
    public Object transfer(@PathVariable Long id, @RequestBody OrderDto.Transfer body) { return orders.transfer(id, body); }

    @PostMapping("/api/orders/{id}/split")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object split(@PathVariable Long id, @RequestBody OrderDto.Split body) { return orders.split(id, body); }

    @PostMapping("/api/orders/{id}/merge")
    @PreAuthorize("hasAuthority('orders.update')")
    public Object merge(@PathVariable Long id, @RequestBody OrderDto.Merge body) { return orders.merge(id, body); }

    @GetMapping("/api/orders/{id}/receipt")
    @PreAuthorize("hasAuthority('receipts.view')")
    public Object receipt(@PathVariable Long id) { return receipts.snapshot(id); }

    @GetMapping(value = "/api/orders/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('receipts.view')")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=comprobante-" + id + ".pdf").body(receipts.pdf(id));
    }

    @GetMapping("/api/kitchen/tickets")
    @PreAuthorize("hasAuthority('kitchen.view')")
    public Object tickets(@RequestParam Long branchId, @RequestParam(defaultValue = "false") boolean history) {
        return kitchen.board(branchId, history);
    }

    @PostMapping("/api/kitchen/tickets/{id}/advance")
    @PreAuthorize("hasAuthority('kitchen.update')")
    public Object advance(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return kitchen.advance(id, body == null ? null : body.get("status"));
    }

    @GetMapping(value = "/api/kitchen/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('kitchen.view')")
    public SseEmitter stream(@RequestParam Long branchId) {
        support.access().checkBranch(branchId);
        return hub.subscribe(branchId);
    }

    @GetMapping("/api/inventory/ingredients")
    @PreAuthorize("hasAuthority('inventory.view') or hasAuthority('menu.view')")
    public Object ingredients() { return catalog.ingredients(); }

    @PostMapping("/api/inventory/ingredients")
    @PreAuthorize("hasAuthority('inventory.manage')")
    public Object saveIngredient(@RequestBody com.restosuite.domain.Ingredient body) { return catalog.saveIngredient(body); }

    @GetMapping("/api/inventory/stock")
    @PreAuthorize("hasAuthority('inventory.view')")
    public Object stock(@RequestParam Long branchId) { return inventory.stock(branchId); }

    @GetMapping("/api/inventory/movements")
    @PreAuthorize("hasAuthority('inventory.view')")
    public Object movements(@RequestParam Long branchId) { return inventory.recentMovements(branchId); }

    @PostMapping("/api/inventory/movements")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public Object move(@RequestBody Move body) {
        return inventory.move(body.branchId(), body.warehouseId(), body.ingredientId(), body.type(), body.direction(), body.quantity(), body.unitCost(), body.reason());
    }

    @PostMapping("/api/inventory/transfers")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    public void transfer(@RequestBody Transfer body) {
        inventory.transfer(body.branchId(), body.sourceWarehouseId(), body.targetWarehouseId(), body.ingredientId(), body.quantity(), body.reason());
    }

    @GetMapping("/api/inventory/warehouses")
    @PreAuthorize("hasAuthority('inventory.view')")
    public Object warehouses(@RequestParam Long branchId) { return catalog.warehouses(branchId); }

    @GetMapping("/api/suppliers")
    @PreAuthorize("hasAuthority('suppliers.manage') or hasAuthority('purchases.view')")
    public Object suppliers() { return purchases.suppliers(); }

    @PostMapping("/api/suppliers")
    @PreAuthorize("hasAuthority('suppliers.manage')")
    public Object saveSupplier(@RequestBody com.restosuite.domain.Supplier body) { return purchases.saveSupplier(body); }

    @GetMapping("/api/purchases")
    @PreAuthorize("hasAuthority('purchases.view')")
    public Object purchaseList(@RequestParam(required = false) Long branchId) { return purchases.list(branchId); }

    @GetMapping("/api/purchases/{id}")
    @PreAuthorize("hasAuthority('purchases.view')")
    public Object purchase(@PathVariable Long id) { return purchases.get(id); }

    @PostMapping("/api/purchases")
    @PreAuthorize("hasAuthority('purchases.manage')")
    public Object createPurchase(@RequestBody PurchaseWrite body) {
        return purchases.create(body.branchId(), body.supplierId(), body.warehouseId(), body.expectedDate(), body.notes(), body.lines());
    }

    @PostMapping("/api/purchases/{id}/order")
    @PreAuthorize("hasAuthority('purchases.manage')")
    public Object orderPurchase(@PathVariable Long id, @RequestBody Map<String, Long> body) { return purchases.markOrdered(id, body.get("version")); }

    @PostMapping("/api/purchases/{id}/receive")
    @PreAuthorize("hasAuthority('purchases.receive')")
    public Object receive(@PathVariable Long id, @RequestBody ReceiveWrite body, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return purchases.receive(id, body.version(), key, body.documentRef(), body.lines());
    }

    @PostMapping("/api/purchases/{id}/pay")
    @PreAuthorize("hasAuthority('purchases.manage')")
    public Object payPurchase(@PathVariable Long id, @RequestBody PayPurchase body) {
        return purchases.pay(id, body.version(), body.amount(), body.methodId());
    }

    @PostMapping("/api/purchases/{id}/cancel")
    @PreAuthorize("hasAuthority('purchases.manage')")
    public Object cancelPurchase(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return purchases.cancel(id, Long.valueOf(body.get("version")), body.get("reason"));
    }

    @GetMapping("/api/expenses")
    @PreAuthorize("hasAuthority('expenses.view')")
    public Object expenseList(@RequestParam(required = false) Long branchId, @RequestParam(required = false) String status,
                              @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        return expenses.list(branchId, status, from, to);
    }

    @PostMapping("/api/expenses")
    @PreAuthorize("hasAuthority('expenses.manage')")
    public Object createExpense(@RequestBody ExpenseWrite body) { return expenses.create(body.expense(), body.payNow()); }

    @PostMapping("/api/expenses/{id}/pay")
    @PreAuthorize("hasAuthority('expenses.pay')")
    public Object payExpense(@PathVariable Long id, @RequestBody Map<String, Long> body) { return expenses.pay(id, body.get("methodId")); }

    @PostMapping("/api/expenses/{id}/void")
    @PreAuthorize("hasAuthority('expenses.manage')")
    public Object voidExpense(@PathVariable Long id, @RequestBody Map<String, String> body) { return expenses.voidExpense(id, body.get("reason")); }

    @GetMapping("/api/cash/current")
    @PreAuthorize("hasAuthority('cash.view')")
    public Object currentCash(@RequestParam Long branchId) {
        support.access().checkBranch(branchId);
        var session = cash.findOpen(branchId).orElse(null);
        if (session == null) return Map.of("open", false);
        return Map.of("open", true, "session", session, "expectedCash", cash.expectedCash(session.getId()), "movements", cash.movements(session.getId()));
    }

    @PostMapping("/api/cash/open")
    @PreAuthorize("hasAuthority('cash.operate')")
    public Object openCash(@RequestBody Map<String, String> body) {
        return cash.open(Long.valueOf(body.get("branchId")), new BigDecimal(body.get("openingFloat")), body.get("notes"));
    }

    @PostMapping("/api/cash/{id}/movements")
    @PreAuthorize("hasAuthority('cash.operate')")
    public Object cashMove(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return cash.manual(id, Long.valueOf(body.get("version")), body.get("type"), new BigDecimal(body.get("amount")), body.get("description"));
    }

    @PostMapping("/api/cash/{id}/close")
    @PreAuthorize("hasAuthority('cash.close')")
    public Object closeCash(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return cash.close(id, Long.valueOf(body.get("version")), new BigDecimal(body.get("countedCash")), body.get("notes"));
    }

    @GetMapping("/api/cash/history")
    @PreAuthorize("hasAuthority('cash.view')")
    public Object cashHistory(@RequestParam(required = false) Long branchId, @RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to) {
        return reports.report("cash", branchId, from, to);
    }

    @GetMapping("/api/dashboard")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public Object dashboard(@RequestParam(required = false) Long branchId, @RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to) {
        return reports.dashboard(branchId, from, to);
    }

    @GetMapping("/api/reports/income-statement")
    @PreAuthorize("hasAuthority('reports.financial')")
    public Object income(@RequestParam(required = false) Long branchId, @RequestParam(required = false) LocalDate from,
                         @RequestParam(required = false) LocalDate to) {
        return reports.income(branchId, from, to);
    }

    @GetMapping("/api/reports/{type}")
    @PreAuthorize("hasAuthority('reports.view')")
    public Object report(@PathVariable String type, @RequestParam(required = false) Long branchId,
                         @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        if ("income-statement".equals(type)) {
            if (!financial()) {
                throw com.restosuite.common.ApiException.forbidden("El estado de resultados requiere el permiso financiero.");
            }
            return reports.income(branchId, from, to);
        }
        return reports.report(type, branchId, from, to);
    }

    @GetMapping("/api/reports/{type}/export")
    @PreAuthorize("hasAuthority('reports.view')")
    public ResponseEntity<byte[]> export(@PathVariable String type, @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) Long branchId, @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to) {
        if ("income-statement".equals(type) && !financial()) {
            throw com.restosuite.common.ApiException.forbidden("El estado de resultados requiere el permiso financiero.");
        }
        var table = "income-statement".equals(type) ? incomeTable(branchId, from, to) : reports.report(type, branchId, from, to);
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = reports.pdf(table);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        }
        byte[] body = reports.csv(table);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(body);
    }

    @GetMapping("/api/notifications")
    @PreAuthorize("hasAuthority('dashboard.view')")
    public Object notifications(@RequestParam(required = false) Long branchId) {
        var board = reports.dashboard(branchId, null, null);
        List<Map<String, Object>> notes = new java.util.ArrayList<>();
        long pending = ((Number) board.get("pendingKitchen")).longValue();
        if (pending > 0) {
            notes.add(Map.of("level", "warning", "title", "Cocina", "message", pending + " comandas pendientes de entrega"));
        }
        Object low = board.get("lowStock");
        if (low instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> row) {
                    notes.add(Map.of("level", "warning", "title", "Existencia baja",
                            "message", row.get("name") + " · " + row.get("amount")));
                }
            }
        }
        return notes;
    }

    private boolean financial() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(authority -> "reports.financial".equals(authority.getAuthority()));
    }

    private ReportService.ReportTable incomeTable(Long branchId, LocalDate from, LocalDate to) {
        var row = reports.income(branchId, from, to);
        return new ReportService.ReportTable("Estado de resultados", List.of("Concepto", "Importe"), List.of(
                List.of("Ventas brutas", row.grossSales().toPlainString()),
                List.of("Descuentos", row.discounts().toPlainString()),
                List.of("Devoluciones", row.returns().toPlainString()),
                List.of("Ingresos netos", row.netSales().toPlainString()),
                List.of("Costo de productos vendidos", row.cogs().toPlainString()),
                List.of("Ganancia bruta", row.grossProfit().toPlainString()),
                List.of("Gastos operativos", row.operatingExpenses().toPlainString()),
                List.of("Resultado operativo", row.operatingResult().toPlainString()),
                List.of("Otros ingresos", row.otherIncome().toPlainString()),
                List.of("Otros gastos", row.otherExpenses().toPlainString()),
                List.of("Resultado neto", row.netResult().toPlainString())
        ), row.methodology());
    }

    public record ProductWrite(Product product, List<ProductVariant> variants, List<Long> modifierIds, List<RecipeLine> recipe) {}

    public record WaiterAssignment(Long waiterId) {}
    public record Move(Long branchId, Long warehouseId, Long ingredientId, String type, String direction, BigDecimal quantity, BigDecimal unitCost, String reason) {}
    public record Transfer(Long branchId, Long sourceWarehouseId, Long targetWarehouseId, Long ingredientId, BigDecimal quantity, String reason) {}
    public record PurchaseWrite(Long branchId, Long supplierId, Long warehouseId, LocalDate expectedDate, String notes, List<PurchaseService.LineInput> lines) {}
    public record ReceiveWrite(Long version, String documentRef, List<PurchaseService.ReceiveInput> lines) {}
    public record PayPurchase(Long version, BigDecimal amount, Long methodId) {}
    public record ExpenseWrite(Expense expense, boolean payNow) {}
}
