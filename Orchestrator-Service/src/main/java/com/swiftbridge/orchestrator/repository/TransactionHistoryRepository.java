package com.swiftbridge.orchestrator.repository;

import com.swiftbridge.orchestrator.entity.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TransactionHistoryRepository
 * 
 * Spring Data JPA repository for TransactionHistory entity.
 * Provides CRUD operations and custom queries for transaction auditing.
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    /**
     * Find all transactions with a specific status
     * 
     * @param status the transaction status (SUCCESS, FAILED, PENDING)
     * @return list of matching transactions
     */
    List<TransactionHistory> findAllByStatus(String status);

    /**
     * Find all transactions within a date range
     * 
     * @param startTime the start timestamp
     * @param endTime the end timestamp
     * @return list of transactions within the range
     */
    @Query("SELECT t FROM TransactionHistory t WHERE t.timestamp BETWEEN :startTime AND :endTime ORDER BY t.timestamp DESC")
    List<TransactionHistory> findTransactionsByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count successful conversions
     * 
     * @return number of successful conversions
     */
    @Query("SELECT COUNT(t) FROM TransactionHistory t WHERE t.status = 'SUCCESS'")
    long countSuccessfulConversions();
}
