package com.restosuite.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.domain.Branch;
import com.restosuite.domain.Payment;
import com.restosuite.domain.Receipt;
import com.restosuite.domain.Restaurant;
import com.restosuite.domain.SalesOrder;
import com.restosuite.domain.SalesOrderLine;
import com.restosuite.repo.BranchRepository;
import com.restosuite.repo.PaymentRepository;
import com.restosuite.repo.ReceiptRepository;
import com.restosuite.repo.SalesOrderLineModifierRepository;
import com.restosuite.repo.SalesOrderLineRepository;
import com.restosuite.repo.SalesOrderRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptService {

    private final ReceiptRepository receipts;
    private final SalesOrderRepository orders;
    private final SalesOrderLineRepository lines;
    private final SalesOrderLineModifierRepository modifiers;
    private final PaymentRepository payments;
    private final BranchRepository branches;
    private final SequenceService sequences;
    private final Support support;
    private final CashService cash;
    private final ObjectMapper json;

    public ReceiptService(ReceiptRepository receipts, SalesOrderRepository orders, SalesOrderLineRepository lines,
                          SalesOrderLineModifierRepository modifiers, PaymentRepository payments, BranchRepository branches,
                          SequenceService sequences, Support support, CashService cash, ObjectMapper json) {
        this.receipts = receipts;
        this.orders = orders;
        this.lines = lines;
        this.modifiers = modifiers;
        this.payments = payments;
        this.branches = branches;
        this.sequences = sequences;
        this.support = support;
        this.cash = cash;
        this.json = json;
    }

    @Transactional
    public Receipt issue(SalesOrder order) {
        return receipts.findByOrderId(order.getId()).orElseGet(() -> store(order));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Long orderId) {
        SalesOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("El pedido"));
        support.access().checkBranch(order.getBranchId());
        Receipt receipt = receipts.findByOrderId(orderId).orElseThrow(() -> ApiException.badRequest(
                "El comprobante se genera al cerrar la cuenta. Cierre la cuenta cuando el saldo esté en cero."));
        try {
            return json.readValue(receipt.getSnapshot(), Map.class);
        } catch (JacksonException ex) {
            throw ApiException.badRequest("No se pudo leer el comprobante guardado.");
        }
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long orderId) {
        Map<String, Object> data = snapshot(orderId);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10);
            document.add(new Paragraph(String.valueOf(data.getOrDefault("restaurant", "")), title));
            document.add(new Paragraph(String.valueOf(data.getOrDefault("address", "")), body));
            document.add(new Paragraph("Comprobante interno " + data.get("number"), title));
            document.add(new Paragraph("Pedido " + data.get("orderNumber") + " · " + data.get("issuedAt"), body));
            document.add(new Paragraph("Este documento no es un comprobante fiscal electrónico.", body));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(new float[]{4, 1, 1.4f});
            table.setWidthPercentage(100);
            header(table, "Descripción");
            header(table, "Cant.");
            header(table, "Importe");
            Object rawLines = data.get("lines");
            if (rawLines instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> row) {
                        cell(table, String.valueOf(row.get("name")), body);
                        cell(table, String.valueOf(row.get("quantity")), body);
                        cell(table, String.valueOf(row.get("total")), body);
                    }
                }
            }
            document.add(table);
            document.add(new Paragraph("Subtotal " + data.get("net"), body));
            document.add(new Paragraph("Descuentos " + data.get("discount"), body));
            document.add(new Paragraph("Impuestos " + data.get("tax"), body));
            document.add(new Paragraph("Total " + data.get("total"), title));
            document.add(new Paragraph("Pagos: " + data.get("payments"), body));
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw ApiException.badRequest("No se pudo generar el PDF del comprobante.");
        }
    }

    private Receipt store(SalesOrder order) {
        Restaurant restaurant = support.restaurant();
        Branch branch = branches.findById(order.getBranchId()).orElseThrow();
        List<Map<String, Object>> lineRows = lines.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(line -> !Codes.CANCELLED.equals(line.getStatus()))
                .map(this::lineRow).toList();
        List<String> paymentRows = payments.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(payment -> Codes.COMPLETED.equals(payment.getStatus()))
                .map(this::paymentRow).toList();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("restaurant", restaurant.getLegalName() == null ? restaurant.getName() : restaurant.getLegalName());
        snapshot.put("tradeName", restaurant.getName());
        snapshot.put("taxId", restaurant.getTaxId());
        snapshot.put("address", branch.getAddress());
        snapshot.put("phone", branch.getPhone());
        snapshot.put("number", sequences.next(order.getBranchId(), Codes.DOC_RECEIPT));
        snapshot.put("orderNumber", order.getOrderNumber());
        snapshot.put("issuedAt", java.time.Instant.now().toString());
        snapshot.put("currency", restaurant.getCurrencyCode());
        snapshot.put("lines", lineRows);
        snapshot.put("gross", order.getGrossAmount());
        snapshot.put("discount", order.getDiscountAmount());
        snapshot.put("net", order.getNetAmount());
        snapshot.put("tax", order.getTaxAmount());
        snapshot.put("total", order.getTotalAmount());
        snapshot.put("payments", paymentRows);
        snapshot.put("fiscal", "INTERNAL_ONLY");
        Receipt receipt = new Receipt();
        receipt.setOrderId(order.getId());
        receipt.setNumber(String.valueOf(snapshot.get("number")));
        try {
            receipt.setSnapshot(json.writeValueAsString(snapshot));
        } catch (JacksonException ex) {
            throw ApiException.badRequest("No se pudo guardar el comprobante.");
        }
        receipt.setIssuedAt(java.time.Instant.now());
        return receipts.save(receipt);
    }

    private Map<String, Object> lineRow(SalesOrderLine line) {
        String extras = modifiers.findByOrderLineId(line.getId()).stream()
                .map(mod -> mod.getName()).reduce((a, b) -> a + ", " + b).orElse("");
        String name = line.getProductName() + (line.getVariantName() == null ? "" : " " + line.getVariantName());
        if (!extras.isBlank()) {
            name = name + " (" + extras + ")";
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("quantity", line.getQuantity());
        row.put("total", line.getTotalAmount());
        row.put("notes", line.getNotes());
        return row;
    }

    private String paymentRow(Payment payment) {
        return cash.methodLabel(payment.getMethodId()) + " " + payment.getAmount();
    }

    private void header(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void cell(PdfPTable table, String text, Font font) {
        table.addCell(new Phrase(text, font));
    }
}
