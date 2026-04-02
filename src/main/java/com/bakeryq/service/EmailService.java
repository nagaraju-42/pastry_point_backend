package com.bakeryq.service;

import com.bakeryq.entity.Order;
import com.bakeryq.util.PdfReceiptGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfReceiptGenerator pdfGenerator;

    @Value("${spring.mail.username:noreply@bakeryq.com}")
    private String fromEmail;

    @Value("${app.shop-name:BakeryQ}")
    private String shopName;

    // ── Helper: skip sending if email is the placeholder dummy ────────────
    private boolean isEmailConfigured() {
        return fromEmail != null
                && !fromEmail.isBlank()
                && !fromEmail.equals("noreply@bakeryq.com")
                && !fromEmail.equals("your-gmail@gmail.com")
                && fromEmail.contains("@");
    }

    @Async
    public void sendOrderConfirmation(Order order) {
        if (!isEmailConfigured()) {
            log.info("Email not configured — skipping confirmation email for order {}",
                    order.getOrderNumber());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("✅ Order Confirmed! " + order.getOrderNumber() + " - " + shopName);

            String htmlBody = buildConfirmationEmail(order);
            helper.setText(htmlBody, true);

            // Attach PDF receipt — wrapped separately so PDF failure doesn't kill email
            try {
                byte[] pdfBytes = pdfGenerator.generateReceipt(order);
                helper.addAttachment(
                        "Receipt-" + order.getOrderNumber() + ".pdf",
                        () -> new java.io.ByteArrayInputStream(pdfBytes),
                        "application/pdf");
            } catch (Exception pdfEx) {
                log.warn("PDF generation failed for order {} — sending email without attachment: {}",
                        order.getOrderNumber(), pdfEx.getMessage());
            }

            mailSender.send(message);
            log.info("Confirmation email sent to {}", order.getUser().getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send confirmation email for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        } catch (Exception e) {
            // Catch ALL exceptions so email failure NEVER crashes the payment flow
            log.error("Unexpected email error for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    @Async
    public void sendOrderReadyNotification(Order order) {
        if (!isEmailConfigured()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());
            helper.setSubject("🔔 Your Order is Ready! " + order.getOrderNumber());

            String body = "<html><body style='font-family:Arial,sans-serif;max-width:500px'>"
                    + "<h2 style='color:#2d6a4f'>Your order is ready! 🥐</h2>"
                    + "<p>Hi <strong>" + order.getUser().getName() + "</strong>,</p>"
                    + "<p>Order <strong>" + order.getOrderNumber() + "</strong> is ready for "
                    + (order.getOrderType() == Order.OrderType.PICKUP
                            ? "pickup at the counter."
                            : "delivery.")
                    + "</p>"
                    + "<p>Thank you for choosing <strong>" + shopName + "</strong>!</p>"
                    + "</body></html>";

            helper.setText(body, true);
            mailSender.send(message);
            log.info("Ready notification sent to {}", order.getUser().getEmail());

        } catch (Exception e) {
            log.error("Failed to send ready notification for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    private String buildConfirmationEmail(Order order) {
        StringBuilder items = new StringBuilder();
        order.getItems().forEach(item ->
            items.append("<tr>")
                .append("<td style='padding:6px'>").append(item.getItemName()).append("</td>")
                .append("<td style='padding:6px;text-align:center'>").append(item.getQuantity()).append("</td>")
                .append("<td style='padding:6px;text-align:right'>₹").append(item.getLineTotal()).append("</td>")
                .append("</tr>")
        );

        return "<html><body style='font-family:Arial,sans-serif;max-width:600px;margin:auto'>"
                + "<div style='background:#2d6a4f;padding:20px;text-align:center'>"
                + "<h1 style='color:white;margin:0'>" + shopName + "</h1></div>"
                + "<div style='padding:24px'>"
                + "<h2 style='color:#2d6a4f'>Order Confirmed! ✅</h2>"
                + "<p>Hi <strong>" + order.getUser().getName()
                + "</strong>, your order has been placed successfully.</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:16px 0'>"
                + "<tr style='background:#f0f0f0'>"
                + "<th style='padding:8px;text-align:left'>Item</th>"
                + "<th style='padding:8px'>Qty</th>"
                + "<th style='padding:8px;text-align:right'>Amount</th></tr>"
                + items
                + "</table><hr/>"
                + "<table style='width:100%;margin-top:8px'>"
                + "<tr><td>Subtotal</td><td style='text-align:right'>₹" + order.getSubtotal() + "</td></tr>"
                + "<tr><td>Delivery</td><td style='text-align:right'>₹" + order.getDeliveryCharge() + "</td></tr>"
                + (order.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0
                        ? "<tr><td>Discount</td><td style='text-align:right;color:green'>-₹"
                                + order.getDiscount() + "</td></tr>" : "")
                + "<tr style='font-weight:bold;font-size:16px'>"
                + "<td>Total</td><td style='text-align:right'>₹" + order.getTotalAmount() + "</td></tr>"
                + "</table>"
                + "<p style='margin-top:16px;color:#555'>Order Type: <strong>"
                + order.getOrderType() + "</strong></p>"
                + "<p style='color:#555'>Estimated Wait: <strong>"
                + order.getEstimatedWaitMinutes() + " minutes</strong></p>"
                + "</div></body></html>";
    }
}