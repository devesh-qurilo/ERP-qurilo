package com.erp.finance_servic.service.receipt;

import com.erp.finance_servic.entity.receipt.InvoiceReceipt;
import com.erp.finance_servic.repository.receipt.InvoiceReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceReceiptRepository repo;
    private final SpringTemplateEngine templateEngine;

    public byte[] render(Long id) {
        InvoiceReceipt inv = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));

        Context ctx = new Context();
        ctx.setVariable("invoice", inv);
        ctx.setVariable("issueDateStr",
                inv.getIssueDate() == null ? "" :
                        inv.getIssueDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        // Logo ko base64 mein embed karein
        ctx.setVariable("logoBase64", getLogoAsBase64());

        // Money formatter bound to invoice currency
        ctx.setVariable("fmt", (MoneyFormatter) (BigDecimal amt) ->
                formatCurrency(amt, inv.getCurrency()));

        String html = templateEngine.process("invoice-pdf", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.withHtmlContent(html, null);
            b.useFastMode();
            b.toStream(out);
            b.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String getLogoAsBase64() {
        try {
            // Logo file ko resources folder se read karein
            // Pehle static/ folder mein check karein (Spring Boot default)
            ClassPathResource logoResource = new ClassPathResource("static/fulllogo_transparent.png");

            if (!logoResource.exists()) {
                // Agar static/ mein nahi mila, toh direct resources mein check karein
                logoResource = new ClassPathResource("fulllogo_transparent.png");
            }

            if (!logoResource.exists()) {
                // Agar dono jagah nahi mila, toh placeholder use karein
                return getDefaultPlaceholderLogo();
            }

            byte[] logoBytes = StreamUtils.copyToByteArray(logoResource.getInputStream());
            String base64 = Base64.getEncoder().encodeToString(logoBytes);
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            // Fallback agar koi error aaye
            return getDefaultPlaceholderLogo();
        }
    }

    private String getDefaultPlaceholderLogo() {
        // Simple transparent 1x1 pixel as fallback
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
    }

    @FunctionalInterface
    public interface MoneyFormatter {
        String apply(BigDecimal amount);
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "";
        String code = (currency == null) ? "INR" : currency.toUpperCase();
        Locale locale = switch (code) {
            case "INR" -> new Locale("en", "IN");
            case "USD" -> Locale.US;
            case "EUR" -> Locale.GERMANY;
            case "GBP" -> Locale.UK;
            default -> Locale.US;
        };

        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        String symbol = currencySymbol(code);
        DecimalFormatSymbols dfs = ((DecimalFormat) nf).getDecimalFormatSymbols();
        dfs.setCurrencySymbol(symbol); // force your own
        ((DecimalFormat) nf).setDecimalFormatSymbols(dfs);

        return nf.format(amount);
    }

    private String currencySymbol(String code) {
        Map<String,String> m = new HashMap<>();
        m.put("INR", "₹");
        m.put("USD", "$");
        m.put("EUR", "€");
        m.put("GBP", "£");
        return m.getOrDefault(code, code);
    }
}