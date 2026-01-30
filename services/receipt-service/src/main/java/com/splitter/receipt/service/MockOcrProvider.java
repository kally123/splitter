package com.splitter.receipt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Mock OCR provider for development and testing.
 * Generates sample receipt data without requiring external OCR service.
 */
@Component
@Slf4j
public class MockOcrProvider implements OcrProvider {
    
    @Value("${ocr.mock.enabled:true}")
    private boolean enabled;
    
    private final Random random = new Random();
    
    private static final String[] MERCHANTS = {
        "STARBUCKS COFFEE",
        "WHOLE FOODS MARKET",
        "TARGET STORE #1234",
        "AMAZON FRESH",
        "COSTCO WHOLESALE",
        "TRADER JOE'S",
        "CHIPOTLE MEXICAN GRILL",
        "SUBWAY #42567",
        "MCDONALD'S",
        "UBER EATS DELIVERY"
    };
    
    private static final String[][] ITEMS = {
        {"Coffee Latte", "4.95"},
        {"Croissant", "3.50"},
        {"Avocado Toast", "8.99"},
        {"Caesar Salad", "12.50"},
        {"Chicken Burrito", "11.25"},
        {"Diet Coke", "2.49"},
        {"French Fries", "3.99"},
        {"Ice Cream", "5.50"},
        {"Sandwich", "9.99"},
        {"Bottled Water", "1.99"}
    };
    
    @Override
    public Mono<String> extractText(byte[] imageData, String contentType) {
        return Mono.fromCallable(() -> {
            log.info("Mock OCR provider processing {} bytes", imageData.length);
            
            // Simulate processing delay
            Thread.sleep(500 + random.nextInt(1000));
            
            // Generate mock receipt text
            StringBuilder sb = new StringBuilder();
            
            // Merchant
            String merchant = MERCHANTS[random.nextInt(MERCHANTS.length)];
            sb.append(merchant).append("\n");
            sb.append("123 Main Street\n");
            sb.append("City, ST 12345\n");
            sb.append("Tel: (555) 123-4567\n\n");
            
            // Date and time
            LocalDate date = LocalDate.now().minusDays(random.nextInt(7));
            sb.append("Date: ").append(date.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"))).append("\n");
            sb.append("Time: ").append(String.format("%02d:%02d", 8 + random.nextInt(12), random.nextInt(60))).append("\n\n");
            
            // Items
            BigDecimal subtotal = BigDecimal.ZERO;
            int numItems = 2 + random.nextInt(4);
            for (int i = 0; i < numItems; i++) {
                String[] item = ITEMS[random.nextInt(ITEMS.length)];
                int qty = 1 + random.nextInt(2);
                BigDecimal price = new BigDecimal(item[1]).multiply(BigDecimal.valueOf(qty));
                subtotal = subtotal.add(price);
                sb.append(item[0]).append(" x").append(qty).append(" $").append(price).append("\n");
            }
            
            sb.append("\n");
            sb.append("Subtotal: $").append(subtotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("\n");
            
            BigDecimal tax = subtotal.multiply(new BigDecimal("0.0825")).setScale(2, java.math.RoundingMode.HALF_UP);
            sb.append("Tax: $").append(tax).append("\n");
            
            BigDecimal total = subtotal.add(tax);
            sb.append("Total: $").append(total.setScale(2, java.math.RoundingMode.HALF_UP)).append("\n");
            sb.append("\n");
            sb.append("Payment: VISA ****1234\n");
            sb.append("\nThank you for your purchase!\n");
            
            log.info("Generated mock receipt for merchant: {}", merchant);
            return sb.toString();
        });
    }
    
    @Override
    public String getProviderName() {
        return "mock-ocr";
    }
    
    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
