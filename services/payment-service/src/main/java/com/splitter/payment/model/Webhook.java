package com.splitter.payment.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("webhooks")
public class Webhook {
    
    @Id
    private UUID id;
    
    @Column("provider")
    private PaymentProvider provider;
    
    @Column("event_id")
    private String eventId;
    
    @Column("event_type")
    private String eventType;
    
    @Column("payload")
    private String payloadJson;
    
    @Column("processed")
    private boolean processed;
    
    @Column("process_error")
    private String processError;
    
    @CreatedDate
    @Column("received_at")
    private Instant receivedAt;
    
    @Column("processed_at")
    private Instant processedAt;
}
