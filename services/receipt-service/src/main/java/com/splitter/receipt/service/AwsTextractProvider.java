package com.splitter.receipt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AwsTextractProvider implements OcrProvider {
    
    private final TextractClient textractClient;
    
    @Value("${ocr.aws.enabled:false}")
    private boolean enabled;
    
    @Override
    public Mono<String> extractText(byte[] imageData, String contentType) {
        if (!isAvailable()) {
            return Mono.error(new IllegalStateException("AWS Textract is not available"));
        }
        
        return Mono.fromCallable(() -> {
            Document document = Document.builder()
                .bytes(SdkBytes.fromByteArray(imageData))
                .build();
            
            AnalyzeExpenseRequest request = AnalyzeExpenseRequest.builder()
                .document(document)
                .build();
            
            AnalyzeExpenseResponse response = textractClient.analyzeExpense(request);
            
            // Extract all text from expense documents
            StringBuilder textBuilder = new StringBuilder();
            
            for (ExpenseDocument expenseDoc : response.expenseDocuments()) {
                // Summary fields (merchant, total, date, etc.)
                for (ExpenseField field : expenseDoc.summaryFields()) {
                    String type = field.type().text();
                    String value = field.valueDetection() != null ? 
                        field.valueDetection().text() : "";
                    textBuilder.append(type).append(": ").append(value).append("\n");
                }
                
                // Line items
                for (LineItemGroup lineItemGroup : expenseDoc.lineItemGroups()) {
                    for (LineItemFields lineItem : lineItemGroup.lineItems()) {
                        for (ExpenseField field : lineItem.lineItemExpenseFields()) {
                            textBuilder.append(field.type().text())
                                .append(": ")
                                .append(field.valueDetection() != null ? 
                                    field.valueDetection().text() : "")
                                .append(" ");
                        }
                        textBuilder.append("\n");
                    }
                }
            }
            
            return textBuilder.toString();
        }).subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("AWS Textract processing failed", e));
    }
    
    @Override
    public String getProviderName() {
        return "aws-textract";
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && textractClient != null;
    }
}
