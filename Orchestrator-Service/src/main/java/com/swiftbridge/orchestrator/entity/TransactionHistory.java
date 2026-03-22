package com.swiftbridge.orchestrator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history", indexes = {
    @Index(name = "idx_conversion_status", columnList = "conversion_status"),
    @Index(name = "idx_request_timestamp", columnList = "request_timestamp"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_status", nullable = false, length = 20)
    private ConversionStatus conversionStatus;

    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;

    @Column(name = "message_reference", length = 64)
    private String messageReference;

    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    @Column(name = "processing_duration_ms", nullable = false)
    private Long processingDurationMs;
}
