package com.bakeryq.util;

import com.bakeryq.entity.Order;
import com.bakeryq.entity.OrderItem;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
public class PdfReceiptGenerator {

    @Value("${app.shop-name:BakeryQ}")
    private String shopName;

    @Value("${app.shop-address:Near College Gate}")
    private String shopAddress;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Generates a PDF receipt for the given order and returns it as a byte array.
     */
    public byte[] generateReceipt(Order order) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A5);
            doc.setMargins(20, 30, 20, 30);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ── Header ───────────────────────────────────────────────────
            doc.add(new Paragraph(shopName)
                    .setFont(bold).setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY));

            doc.add(new Paragraph(shopAddress)
                    .setFont(regular).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()));

            // ── Order Info ───────────────────────────────────────────────
            doc.add(new Paragraph("Order Receipt")
                    .setFont(bold).setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth().setMarginTop(8);

            addInfoRow(infoTable, "Order No:", order.getOrderNumber(), bold, regular);
            addInfoRow(infoTable, "Date:", order.getCreatedAt().format(FORMATTER), bold, regular);
            addInfoRow(infoTable, "Customer:", order.getUser().getName(), bold, regular);
            addInfoRow(infoTable, "Order Type:", order.getOrderType().toString(), bold, regular);
            addInfoRow(infoTable, "Payment:", order.getPaymentStatus().toString(), bold, regular);

            doc.add(infoTable);
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginTop(8));

            // ── Items Table ──────────────────────────────────────────────
            doc.add(new Paragraph("Items").setFont(bold).setFontSize(11).setMarginTop(8));

            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 2}))
                    .useAllAvailableWidth().setMarginTop(4);

            // Table header
            for (String header : new String[]{"Item", "Qty", "Rate", "Amount"}) {
                itemsTable.addHeaderCell(new Cell()
                        .add(new Paragraph(header).setFont(bold).setFontSize(9))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }

            // Table rows
            for (OrderItem item : order.getItems()) {
                itemsTable.addCell(cell(item.getItemName(), regular, 9));
                itemsTable.addCell(cell(String.valueOf(item.getQuantity()), regular, 9));
                itemsTable.addCell(cell("₹" + item.getItemPrice(), regular, 9));
                itemsTable.addCell(cell("₹" + item.getLineTotal(), regular, 9));
            }

            doc.add(itemsTable);
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginTop(8));

            // ── Totals ───────────────────────────────────────────────────
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                    .useAllAvailableWidth().setMarginTop(6);

            addTotalRow(totalsTable, "Subtotal:", "₹" + order.getSubtotal(), regular, 10);
            addTotalRow(totalsTable, "Delivery:", "₹" + order.getDeliveryCharge(), regular, 10);

            if (order.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Discount:", "-₹" + order.getDiscount(), regular, 10);
            }

            addTotalRow(totalsTable, "TOTAL:", "₹" + order.getTotalAmount(), bold, 13);
            doc.add(totalsTable);

            // ── Footer ───────────────────────────────────────────────────
            doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine()).setMarginTop(10));
            doc.add(new Paragraph("Thank you for ordering with " + shopName + "! 🥐")
                    .setFont(regular).setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(6).setFontColor(ColorConstants.GRAY));

            doc.close();

        } catch (IOException e) {
            log.error("Failed to generate PDF receipt for order {}", order.getOrderNumber(), e);
            throw new RuntimeException("PDF generation failed", e);
        }

        return baos.toByteArray();
    }

    private void addInfoRow(Table table, String label, String value,
                            PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold).setFontSize(9)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular).setFontSize(9)).setBorder(null));
    }

    private void addTotalRow(Table table, String label, String value,
                             PdfFont font, float size) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(font).setFontSize(size))
                .setBorder(null).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(size))
                .setBorder(null).setTextAlignment(TextAlignment.RIGHT));
    }

    private Cell cell(String text, PdfFont font, float size) {
        return new Cell().add(new Paragraph(text).setFont(font).setFontSize(size));
    }
}
