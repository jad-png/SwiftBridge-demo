package com.swiftbridge.orchestrator.repository;

import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

  List<TransactionHistory> findAllByConversionStatus(ConversionStatus conversionStatus);

  @Query("SELECT t FROM TransactionHistory t WHERE t.requestTimestamp BETWEEN :startTime AND :endTime ORDER BY t.requestTimestamp DESC")
    List<TransactionHistory> findTransactionsByDateRange(LocalDateTime startTime, LocalDateTime endTime);

    @Query("""
        SELECT t FROM TransactionHistory t
        WHERE (:startTime IS NULL OR t.requestTimestamp >= :startTime)
          AND (:endTime IS NULL OR t.requestTimestamp <= :endTime)
          AND (:status IS NULL OR t.conversionStatus = :status)
        ORDER BY t.requestTimestamp DESC
        """)
    Page<TransactionHistory> findByFilters(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("status") ConversionStatus status,
                                           Pageable pageable);

    @Query("SELECT COUNT(t) FROM TransactionHistory t WHERE t.conversionStatus = SUCCESS")
    long countSuccessfulConversions();
}
