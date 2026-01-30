package com.splitter.receipt.service;

import com.splitter.receipt.model.ParsedReceipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ReceiptParserService {
    
    // Common patterns for receipt parsing
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
        "(?i)(?:total|amount|sum|grand\\s*total|balance\\s*due)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    private static final Pattern SUBTOTAL_PATTERN = Pattern.compile(
        "(?i)(?:subtotal|sub\\s*total)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    private static final Pattern TAX_PATTERN = Pattern.compile(
        "(?i)(?:tax|vat|gst|hst)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    private static final Pattern TIP_PATTERN = Pattern.compile(
        "(?i)(?:tip|gratuity)[:\\s]*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    private static final Pattern DATE_PATTERNS[] = {
        Pattern.compile("(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})"),  // MM/DD/YYYY or DD/MM/YYYY
        Pattern.compile("(\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2})"),    // YYYY/MM/DD
        Pattern.compile("([A-Za-z]{3}\\s+\\d{1,2},?\\s+\\d{4})"),   // Jan 15, 2024
    };
    
    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
        "(?i)(USD|EUR|GBP|CAD|AUD|\\$|€|£)"
    );
    
    private static final Pattern LINE_ITEM_PATTERN = Pattern.compile(
        "(?i)(.+?)\\s+(\\d+)?\\s*[xX@]?\\s*\\$?([0-9]+[.,][0-9]{2})"
    );
    
    public Mono<ParsedReceipt> parse(String rawText) {
        return Mono.fromCallable(() -> {
            log.debug("Parsing receipt text: {}", rawText.substring(0, Math.min(200, rawText.length())));
            
            ParsedReceipt.ParsedReceiptBuilder builder = ParsedReceipt.builder();
            int fieldsFound = 0;
            int totalFields = 5; // merchant, date, total, items, currency
            
            // Extract total
            Optional<BigDecimal> total = extractAmount(rawText, TOTAL_PATTERN);
            if (total.isPresent()) {
                builder.total(total.get());
                fieldsFound++;
            }
            
            // Extract subtotal
            extractAmount(rawText, SUBTOTAL_PATTERN).ifPresent(builder::subtotal);
            
            // Extract tax
            extractAmount(rawText, TAX_PATTERN).ifPresent(builder::tax);
            
            // Extract tip
            extractAmount(rawText, TIP_PATTERN).ifPresent(builder::tip);
            
            // Extract date
            Optional<LocalDate> date = extractDate(rawText);
            if (date.isPresent()) {
                builder.date(date.get());
                fieldsFound++;
            }
            
            // Extract merchant (usually first non-empty line)
            Optional<String> merchant = extractMerchant(rawText);
            if (merchant.isPresent()) {
                builder.merchantName(merchant.get());
                fieldsFound++;
            }
            
            // Extract currency
            Optional<String> currency = extractCurrency(rawText);
            if (currency.isPresent()) {
                builder.currency(currency.get());
                fieldsFound++;
            } else {
                builder.currency("USD"); // Default
            }
            
            // Extract line items
            List<ParsedReceipt.LineItem> items = extractLineItems(rawText);
            if (!items.isEmpty()) {
                builder.items(items);
                fieldsFound++;
            }
            
            // Calculate confidence based on fields extracted
            double confidence = (double) fieldsFound / totalFields;
            builder.confidence(confidence);
            
            ParsedReceipt result = builder.build();
            log.info("Parsed receipt: merchant={}, total={}, confidence={}", 
                result.getMerchantName(), result.getTotal(), result.getConfidence());
            
            return result;
        });
    }
    
    private Optional<BigDecimal> extractAmount(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", ".");
                return Optional.of(new BigDecimal(amountStr));
            } catch (NumberFormatException e) {
                log.warn("Could not parse amount: {}", matcher.group(1));
            }
        }
        return Optional.empty();
    }
    
    private Optional<LocalDate> extractDate(String text) {
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String dateStr = matcher.group(1);
                LocalDate date = parseFlexibleDate(dateStr);
                if (date != null) {
                    return Optional.of(date);
                }
            }
        }
        return Optional.empty();
    }
    
    private LocalDate parseFlexibleDate(String dateStr) {
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MMM d, yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        log.warn("Could not parse date: {}", dateStr);
        return null;
    }
    
    private Optional<String> extractMerchant(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip empty lines, lines with just numbers, or common header text
            if (trimmed.isEmpty() || 
                trimmed.matches("^[\\d\\s.,/$]+$") ||
                trimmed.toLowerCase().matches("^(receipt|invoice|order|date|time|tel|phone|fax).*")) {
                continue;
            }
            // First substantial line is likely the merchant name
            if (trimmed.length() > 2 && trimmed.length() < 100) {
                return Optional.of(trimmed);
            }
        }
        return Optional.empty();
    }
    
    private Optional<String> extractCurrency(String text) {
        Matcher matcher = CURRENCY_PATTERN.matcher(text);
        if (matcher.find()) {
            String currency = matcher.group(1).toUpperCase();
            return switch (currency) {
                case "$" -> Optional.of("USD");
                case "€" -> Optional.of("EUR");
                case "£" -> Optional.of("GBP");
                default -> Optional.of(currency);
            };
        }
        return Optional.empty();
    }
    
    private List<ParsedReceipt.LineItem> extractLineItems(String text) {
        List<ParsedReceipt.LineItem> items = new ArrayList<>();
        String[] lines = text.split("\\n");
        
        for (String line : lines) {
            Matcher matcher = LINE_ITEM_PATTERN.matcher(line);
            if (matcher.find()) {
                try {
                    String description = matcher.group(1).trim();
                    String qtyStr = matcher.group(2);
                    String priceStr = matcher.group(3).replace(",", ".");
                    
                    // Skip if this looks like a total/subtotal line
                    if (description.toLowerCase().matches(".*(total|tax|subtotal|balance|change|cash|credit).*")) {
                        continue;
                    }
                    
                    Integer quantity = qtyStr != null ? Integer.parseInt(qtyStr) : 1;
                    BigDecimal price = new BigDecimal(priceStr);
                    
                    items.add(ParsedReceipt.LineItem.builder()
                        .description(description)
                        .quantity(quantity)
                        .totalPrice(price)
                        .unitPrice(price.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP))
                        .build());
                } catch (Exception e) {
                    // Skip malformed line items
                }
            }
        }
        
        return items;
    }
}
