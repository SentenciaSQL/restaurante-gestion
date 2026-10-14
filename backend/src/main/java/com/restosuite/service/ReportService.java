package com.restosuite.service;

import com.restosuite.common.Money;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    public static final String METHODOLOGY = """
            Los ingresos se reconocen al cerrar la cuenta, no al cobrar. El impuesto trasladado (ITBIS) no forma parte de la venta ni del resultado.
            Ingresos netos = ventas brutas sin impuesto − descuentos − devoluciones del período.
            El costo de productos vendidos usa el costo promedio del inventario al descontar la receta, más las mermas del período, menos el costo reingresado por devoluciones.
            Las compras de inventario son activo, no gasto operativo. Los movimientos de caja no son ingresos contables.
            Resultado operativo = ganancia bruta − gastos operativos. Resultado neto = resultado operativo + otros ingresos − otros gastos.
            """.trim();

    private final EntityManager em;
    private final Support support;

    public ReportService(EntityManager em, Support support) {
        this.em = em;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public IncomeStatement income(Long branchId, LocalDate from, LocalDate to) {
        Scope scope = scope(branchId, from, to);
        BigDecimal gross = money("""
                select coalesce(sum(l.gross_amount),0) from sales_order_lines l
                join sales_orders o on o.id = l.order_id
                where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
        BigDecimal discounts = money("""
                select coalesce(sum(l.discount_amount),0) from sales_order_lines l
                join sales_orders o on o.id = l.order_id
                where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
        BigDecimal returns = money("""
                select coalesce(sum(r.net_amount),0) from refunds r
                join sales_orders o on o.id = r.order_id
                where r.created_at >= :start and r.created_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
        BigDecimal recipeCost = money("""
                select coalesce(sum(l.cost_amount),0) from sales_order_lines l
                join sales_orders o on o.id = l.order_id
                where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
        BigDecimal restock = money("""
                select coalesce(sum(r.cost_amount),0) from refunds r
                join sales_orders o on o.id = r.order_id
                where r.restock = true and r.created_at >= :start and r.created_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
        BigDecimal waste = money("""
                select coalesce(sum(m.quantity * m.unit_cost),0) from stock_movements m
                join warehouses w on w.id = m.warehouse_id
                where m.movement_type = 'WASTE' and m.direction = 'OUT'
                and m.created_at >= :start and m.created_at < :end
                """ + scope.filter("w.branch_id"), scope.params());
        BigDecimal operating = expenses("OPERATING", scope);
        BigDecimal otherIncome = expenses("OTHER_INCOME", scope);
        BigDecimal otherExpense = expenses("OTHER_EXPENSE", scope);
        BigDecimal tax = money("""
                select coalesce(sum(l.tax_amount),0) from sales_order_lines l
                join sales_orders o on o.id = l.order_id
                where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id"), scope.params())
                .subtract(money("""
                        select coalesce(sum(r.tax_amount),0) from refunds r
                        join sales_orders o on o.id = r.order_id
                        where r.created_at >= :start and r.created_at < :end
                        """ + scope.filter("o.branch_id"), scope.params()));
        BigDecimal netSales = gross.subtract(discounts).subtract(returns);
        BigDecimal cogs = recipeCost.subtract(restock).add(waste);
        BigDecimal grossProfit = netSales.subtract(cogs);
        BigDecimal operatingResult = grossProfit.subtract(operating);
        BigDecimal net = operatingResult.add(otherIncome).subtract(otherExpense);
        return new IncomeStatement(scope.from(), scope.to(), support.restaurant().getCurrencyCode(), gross, discounts, returns,
                netSales, recipeCost, restock, waste, cogs, grossProfit, operating, operatingResult, otherIncome, otherExpense, net, tax, METHODOLOGY);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(Long branchId, LocalDate from, LocalDate to) {
        Scope scope = scope(branchId, from, to);
        Support.Period today = support.period(LocalDate.now(support.zone()), LocalDate.now(support.zone()));
        LocalDate monthStart = LocalDate.now(support.zone()).withDayOfMonth(1);
        Support.Period month = support.period(monthStart, LocalDate.now(support.zone()));
        IncomeStatement statement = income(branchId, scope.from(), scope.to());
        long branch = scope.branchId();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("salesToday", billed(today, branch));
        body.put("salesMonth", billed(month, branch));
        body.put("orders", countOrders(scope));
        body.put("averageTicket", averageTicket(scope));
        body.put("tables", tableCounts(branch));
        body.put("pendingKitchen", pendingKitchen(branch));
        body.put("lowStock", lowStock(branch));
        body.put("expenses", statement.operatingExpenses().add(statement.otherExpenses()));
        body.put("grossProfit", statement.grossProfit());
        body.put("netResult", statement.netResult());
        body.put("collections", collections(scope));
        body.put("salesByDay", salesByDay(scope));
        body.put("paymentMethods", paymentMix(scope));
        body.put("topProducts", topProducts(scope));
        body.put("recent", recent());
        body.put("currency", support.restaurant().getCurrencyCode());
        body.put("from", scope.from());
        body.put("to", scope.to());
        return body;
    }

    @Transactional(readOnly = true)
    public ReportTable report(String type, Long branchId, LocalDate from, LocalDate to) {
        Scope scope = scope(branchId, from, to);
        return switch (type) {
            case "sales-day" -> table("Ventas por día", List.of("Día", "Cuentas", "Total con impuestos"), salesByDay(scope).stream()
                    .map(row -> List.of(String.valueOf(row.get("day")), String.valueOf(row.get("orders")), String.valueOf(row.get("total")))).toList());
            case "sales-product" -> named("Ventas por producto", topProducts(scope));
            case "sales-payment" -> named("Cobros por método", paymentMix(scope));
            case "expenses" -> named("Gastos por categoría", rows("""
                    select c.name, coalesce(sum(e.amount),0) from expenses e
                    join expense_categories c on c.id = e.category_id
                    where e.status <> 'VOID' and e.expense_date >= :fromDate and e.expense_date <= :toDate
                    """ + scope.filter("e.branch_id") + " group by c.name order by 2 desc", scope.params()));
            case "waste" -> named("Mermas", rows("""
                    select i.name, coalesce(sum(m.quantity * m.unit_cost),0) from stock_movements m
                    join ingredients i on i.id = m.ingredient_id
                    join warehouses w on w.id = m.warehouse_id
                    where m.movement_type = 'WASTE' and m.created_at >= :start and m.created_at < :end
                    """ + scope.filter("w.branch_id") + " group by i.name order by 2 desc", scope.params()));
            case "purchases" -> named("Compras por proveedor", rows("""
                    select s.name, coalesce(sum(p.total_amount),0) from purchase_orders p
                    join suppliers s on s.id = p.supplier_id
                    where p.status <> 'CANCELLED' and p.order_date >= :fromDate and p.order_date <= :toDate
                    """ + scope.filter("p.branch_id") + " group by s.name order by 2 desc", scope.params()));
            case "sales-category" -> named("Ventas por categoría", rows("""
                    select c.name, coalesce(sum(l.net_amount),0) from sales_order_lines l
                    join sales_orders o on o.id = l.order_id
                    join products p on p.id = l.product_id
                    join menu_categories c on c.id = p.category_id
                    where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                    and o.closed_at >= :start and o.closed_at < :end
                    """ + scope.filter("o.branch_id") + " group by c.name order by 2 desc", scope.params()));
            case "sales-waiter" -> named("Ventas por mesero", rows("""
                    select u.full_name, coalesce(sum(o.total_amount),0) from sales_orders o
                    join users u on u.id = o.waiter_id
                    where o.account_status = 'CLOSED' and o.closed_at >= :start and o.closed_at < :end
                    """ + scope.filter("o.branch_id") + " group by u.full_name order by 2 desc", scope.params()));
            case "sales-cashier" -> named("Cobros por cajero", rows("""
                    select u.full_name, coalesce(sum(p.amount),0) from payments p
                    join sales_orders o on o.id = p.order_id
                    join users u on u.id = p.user_id
                    where p.status = 'COMPLETED' and p.paid_at >= :start and p.paid_at < :end
                    """ + scope.filter("o.branch_id") + " group by u.full_name order by 2 desc", scope.params()));
            case "sales-table" -> named("Ventas por mesa", rows("""
                    select coalesce(t.number, 'Sin mesa'), coalesce(sum(o.total_amount),0) from sales_orders o
                    left join dining_tables t on t.id = o.table_id
                    where o.account_status = 'CLOSED' and o.closed_at >= :start and o.closed_at < :end
                    """ + scope.filter("o.branch_id") + " group by t.number order by 2 desc", scope.params()));
            case "discounts" -> named("Descuentos", rows("""
                    select d.reason, coalesce(sum(d.amount),0) from sales_order_discounts d
                    join sales_orders o on o.id = d.order_id
                    where o.account_status = 'CLOSED' and o.closed_at >= :start and o.closed_at < :end
                    """ + scope.filter("o.branch_id") + " group by d.reason order by 2 desc", scope.params()));
            case "cancellations" -> named("Cancelaciones", rows("""
                    select coalesce(o.cancel_reason, 'Sin motivo'), count(*) from sales_orders o
                    where o.account_status = 'CANCELLED' and o.cancelled_at >= :start and o.cancelled_at < :end
                    """ + scope.filter("o.branch_id") + " group by o.cancel_reason order by 2 desc", scope.params()));
            case "prep-times" -> table("Tiempos de preparación", List.of("Estación", "Comandas", "Minutos promedio"), cells("""
                    select station_name, count(*),
                           coalesce(round(avg(extract(epoch from (ready_at - received_at)) / 60), 1), 0)
                    from kitchen_tickets
                    where ready_at is not null and received_at >= :start and received_at < :end
                    """ + scope.filter("branch_id") + " group by station_name order by 1", scope.params()));
            case "occupancy" -> table("Ocupación de mesas", List.of("Mesa", "Cuentas cerradas", "Minutos ocupados"), cells("""
                    select t.number, count(o.id),
                           coalesce(round(sum(extract(epoch from (o.closed_at - o.opened_at)) / 60), 0), 0)
                    from dining_tables t
                    join dining_areas a on a.id = t.area_id
                    left join sales_orders o on o.table_id = t.id and o.account_status = 'CLOSED'
                        and o.closed_at >= :start and o.closed_at < :end
                    where (:branchId = -1 or a.branch_id = :branchId)
                    and a.branch_id in (select id from branches where restaurant_id = :restaurantId)
                    group by t.number order by t.number
                    """, scope.params()));
            case "stock" -> table("Existencias", List.of("Ingrediente", "Cantidad", "Costo promedio"), cells("""
                    select i.name, coalesce(sum(s.quantity),0), coalesce(max(s.average_cost),0)
                    from ingredients i
                    left join stock_levels s on s.ingredient_id = i.id
                    left join warehouses w on w.id = s.warehouse_id
                    where i.restaurant_id = :restaurantId and i.active = true
                    and (:branchId = -1 or w.branch_id = :branchId or w.id is null)
                    group by i.name order by i.name
                    """, Map.of("branchId", scope.branchId(), "restaurantId", scope.restaurantId())));
            case "movements" -> table("Movimientos de inventario", List.of("Tipo", "Cantidad", "Valor"), cells("""
                    select m.movement_type, coalesce(sum(m.quantity),0), coalesce(sum(m.quantity * m.unit_cost),0)
                    from stock_movements m
                    join warehouses w on w.id = m.warehouse_id
                    where m.created_at >= :start and m.created_at < :end
                    """ + scope.filter("w.branch_id") + " group by m.movement_type order by 1", scope.params()));
            case "margins" -> table("Márgenes por producto", List.of("Producto", "Venta neta", "Costo", "Margen"), cells("""
                    select l.product_name, coalesce(sum(l.net_amount),0), coalesce(sum(l.cost_amount),0),
                           coalesce(sum(l.net_amount),0) - coalesce(sum(l.cost_amount),0)
                    from sales_order_lines l
                    join sales_orders o on o.id = l.order_id
                    where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                    and o.closed_at >= :start and o.closed_at < :end
                    """ + scope.filter("o.branch_id") + " group by l.product_name order by 4 desc", scope.params()));
            case "cash" -> table("Cierres de caja", List.of("Apertura", "Esperado", "Contado", "Diferencia"), cells("""
                    select to_char(opened_at at time zone :tz, 'YYYY-MM-DD HH24:MI'),
                           coalesce(expected_cash,0), coalesce(counted_cash,0), coalesce(difference,0)
                    from cash_sessions where status = 'CLOSED' and closed_at >= :start and closed_at < :end
                    """ + scope.filter("branch_id") + " order by closed_at desc", scope.params()));
            default -> table(type, List.of("Detalle", "Importe"), List.of());
        };
    }

    private List<List<String>> cells(String sql, Map<String, Object> params) {
        List<List<String>> result = new ArrayList<>();
        for (Object item : query(sql, params).getResultList()) {
            Object[] row = (Object[]) item;
            List<String> line = new ArrayList<>();
            for (Object cell : row) {
                line.add(cell == null ? "" : cell.toString());
            }
            result.add(line);
        }
        return result;
    }

    public byte[] csv(ReportTable table) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(String.join(",", table.columns().stream().map(this::csv).toList())).append('\n');
        for (List<String> row : table.rows()) {
            builder.append(String.join(",", row.stream().map(this::csv).toList())).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] pdf(ReportTable table) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph(table.title()));
            int columns = Math.max(table.columns().size(), 1);
            PdfPTable grid = new PdfPTable(columns);
            grid.setWidthPercentage(100);
            for (String column : table.columns()) {
                grid.addCell(column);
            }
            for (List<String> row : table.rows()) {
                for (String cell : row) {
                    grid.addCell(cell == null ? "" : cell);
                }
            }
            document.add(grid);
            document.add(new Paragraph(table.note()));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw com.restosuite.common.ApiException.badRequest("No se pudo generar el PDF del reporte.");
        }
    }

    private ReportTable named(String title, List<Map<String, Object>> rows) {
        return table(title, List.of("Concepto", "Importe"), rows.stream()
                .map(row -> List.of(String.valueOf(row.get("name")), String.valueOf(row.get("amount")))).toList());
    }

    private ReportTable table(String title, List<String> columns, List<List<String>> rows) {
        return new ReportTable(title, columns, rows, METHODOLOGY);
    }

    private BigDecimal expenses(String classification, Scope scope) {
        Map<String, Object> params = scope.params();
        params.put("classification", classification);
        return money("""
                select coalesce(sum(e.amount),0) from expenses e
                join expense_categories c on c.id = e.category_id
                where e.status <> 'VOID' and c.classification = :classification
                and e.expense_date >= :fromDate and e.expense_date <= :toDate
                """ + scope.filter("e.branch_id"), params);
    }

    private BigDecimal billed(Support.Period period, long branch) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start", period.start());
        params.put("end", period.end());
        params.put("branchId", branch);
        params.put("restaurantId", support.user().getRestaurantId());
        return money("""
                select coalesce(sum(total_amount),0) from sales_orders o
                where account_status = 'CLOSED' and closed_at >= :start and closed_at < :end
                and (:branchId = -1 or branch_id = :branchId)
                and branch_id in (select id from branches where restaurant_id = :restaurantId)
                """, params);
    }

    private long countOrders(Scope scope) {
        Object value = query("""
                select count(*) from sales_orders o
                where account_status = 'CLOSED' and closed_at >= :start and closed_at < :end
                """ + scope.filter("o.branch_id"), scope.params()).getSingleResult();
        return value == null ? 0 : Long.parseLong(value.toString());
    }

    private BigDecimal averageTicket(Scope scope) {
        long count = countOrders(scope);
        if (count == 0) {
            return Money.ZERO;
        }
        return Money.money(billed(scope.zone(), scope.branchId())
                .divide(BigDecimal.valueOf(count), Money.SCALE, Money.ROUND));
    }

    private Map<String, Long> tableCounts(long branch) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : List.of("AVAILABLE", "OCCUPIED", "RESERVED", "CLEANING")) {
            counts.put(status.toLowerCase(), ((Number) query("""
                    select count(*) from dining_tables t join dining_areas a on a.id = t.area_id
                    where t.status = :status and (:branchId = -1 or a.branch_id = :branchId)
                    and a.branch_id in (select id from branches where restaurant_id = :restaurantId)
                    """, Map.of("status", status, "branchId", branch, "restaurantId", support.user().getRestaurantId())).getSingleResult()).longValue());
        }
        return counts;
    }

    private long pendingKitchen(long branch) {
        return ((Number) query("""
                select count(*) from kitchen_tickets where status in ('QUEUED','IN_PROGRESS')
                and (:branchId = -1 or branch_id = :branchId)
                and branch_id in (select id from branches where restaurant_id = :restaurantId)
                """, Map.of("branchId", branch, "restaurantId", support.user().getRestaurantId())).getSingleResult()).longValue();
    }

    private List<Map<String, Object>> lowStock(long branch) {
        return rows("""
                select i.name, coalesce(sum(s.quantity),0) from ingredients i
                left join stock_levels s on s.ingredient_id = i.id
                left join warehouses w on w.id = s.warehouse_id
                where i.active = true and i.restaurant_id = :restaurantId
                and (:branchId = -1 or w.branch_id = :branchId or w.id is null)
                group by i.id, i.name, i.min_stock
                having coalesce(sum(s.quantity),0) <= i.min_stock
                order by i.name
                """, Map.of("branchId", branch, "restaurantId", support.user().getRestaurantId()));
    }

    private BigDecimal collections(Scope scope) {
        return money("""
                select coalesce(sum(p.amount),0) from payments p
                join sales_orders o on o.id = p.order_id
                where p.status = 'COMPLETED' and p.paid_at >= :start and p.paid_at < :end
                """ + scope.filter("o.branch_id"), scope.params());
    }

    private List<Map<String, Object>> salesByDay(Scope scope) {
        Query query = query("""
                select to_char(o.closed_at at time zone :tz, 'YYYY-MM-DD') as day, count(*), coalesce(sum(o.total_amount),0)
                from sales_orders o
                where o.account_status = 'CLOSED' and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id") + " group by 1 order by 1", scope.params());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : query.getResultList()) {
            Object[] row = (Object[]) item;
            rows.add(Map.of("day", row[0], "orders", row[1], "total", row[2], "name", row[0], "amount", row[2]));
        }
        return rows;
    }

    private List<Map<String, Object>> paymentMix(Scope scope) {
        return rows("""
                select m.name, coalesce(sum(p.amount),0) from payments p
                join payment_methods m on m.id = p.method_id
                join sales_orders o on o.id = p.order_id
                where p.status = 'COMPLETED' and p.paid_at >= :start and p.paid_at < :end
                """ + scope.filter("o.branch_id") + " group by m.name order by 2 desc", scope.params());
    }

    private List<Map<String, Object>> topProducts(Scope scope) {
        return rows("""
                select l.product_name, coalesce(sum(l.net_amount),0) from sales_order_lines l
                join sales_orders o on o.id = l.order_id
                where o.account_status = 'CLOSED' and l.status <> 'CANCELLED'
                and o.closed_at >= :start and o.closed_at < :end
                """ + scope.filter("o.branch_id") + " group by l.product_name order by 2 desc limit 10", scope.params());
    }

    private List<Map<String, Object>> recent() {
        Query query = em.createNativeQuery("""
                select action, entity_type, coalesce(details,''), created_at from audit_logs
                where restaurant_id = :restaurantId order by created_at desc limit 12
                """);
        query.setParameter("restaurantId", support.user().getRestaurantId());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : query.getResultList()) {
            Object[] row = (Object[]) item;
            rows.add(Map.of("action", row[0], "entity", row[1], "details", row[2], "at", row[3]));
        }
        return rows;
    }

    private List<Map<String, Object>> rows(String sql, Map<String, Object> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : query(sql, params).getResultList()) {
            Object[] row = (Object[]) item;
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("name", row[0]);
            mapped.put("amount", row.length > 1 ? row[1] : "");
            if (row.length > 2) {
                mapped.put("extra", row[2]);
            }
            rows.add(mapped);
        }
        return rows;
    }

    private BigDecimal money(String sql, Map<String, Object> params) {
        Object value = query(sql, params).getSingleResult();
        return value == null ? Money.ZERO : new BigDecimal(value.toString());
    }

    private Query query(String sql, Map<String, Object> params) {
        Query query = em.createNativeQuery(sql);
        params.forEach((name, value) -> {
            if (sql.matches("(?s).*:\\Q" + name + "\\E(?![A-Za-z0-9_]).*")) {
                query.setParameter(name, value);
            }
        });
        return query;
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private Scope scope(Long branchId, LocalDate from, LocalDate to) {
        Long resolved = support.access().resolveBranch(branchId);
        Support.Period period = support.period(from, to);
        return new Scope(resolved == null ? -1L : resolved, period.from(), period.to(), period.start(), period.end(),
                support.user().getRestaurantId(), support.zone().getId(), period);
    }

    private record Scope(long branchId, LocalDate from, LocalDate to, java.time.Instant start, java.time.Instant end,
                         Long restaurantId, String tz, Support.Period zone) {
        String filter(String column) {
            return " and (:" + "branchId = -1 or " + column + " = :branchId) and " + column
                    + " in (select id from branches where restaurant_id = :restaurantId) ";
        }

        Map<String, Object> params() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("branchId", branchId);
            params.put("restaurantId", restaurantId);
            params.put("start", start);
            params.put("end", end);
            params.put("fromDate", from);
            params.put("toDate", to);
            params.put("tz", tz);
            return params;
        }
    }

    public record IncomeStatement(LocalDate from, LocalDate to, String currency, BigDecimal grossSales, BigDecimal discounts,
                                  BigDecimal returns, BigDecimal netSales, BigDecimal recipeCost, BigDecimal restockReversal,
                                  BigDecimal waste, BigDecimal cogs, BigDecimal grossProfit, BigDecimal operatingExpenses,
                                  BigDecimal operatingResult, BigDecimal otherIncome, BigDecimal otherExpenses,
                                  BigDecimal netResult, BigDecimal taxCollected, String methodology) {
    }

    public record ReportTable(String title, List<String> columns, List<List<String>> rows, String note) {
    }
}
