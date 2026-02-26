package com.swiftbridge.orchestrator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TransactionHistory JPA Entity
 * 
 * Stores metadata about XML to MT103 conversion requests.
 * Maps to the PostgreSQL 'transaction_history' table.
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@Entity
@Table(name = "transaction_history", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistory {

    /**
     * Unique transaction identifier (auto-generated UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The original XML filename uploaded by the user
     */
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /**
     * Conversion status: SUCCESS, FAILED, PENDING
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Timestamp when the conversion was processed
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * The converted MT103 output (nullable for failed conversions)
     */
    @Column(name = "mt103_output", columnDefinition = "TEXT")
    private String mt103Output;

    /**
     * Error message if conversion failed (nullable for successful conversions)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
