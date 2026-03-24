package com.swiftbridge.orchestrator.repository;

import com.swiftbridge.orchestrator.entity.TransactionHistory;
import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.repository.projection.DailyConversionStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    long countByConversionStatus(ConversionStatus conversionStatus);

    long countByUser_Id(Long userId);

    long countByUser_IdAndConversionStatus(Long userId, ConversionStatus conversionStatus);

    List<TransactionHistory> findTop5ByUser_IdOrderByRequestTimestampDesc(Long userId);

    Optional<TransactionHistory> findByTransactionId(String transactionId);

    @Query("""
      SELECT t FROM TransactionHistory t
      WHERE t.user.id = :userId
        AND (:startTime IS NULL OR t.requestTimestamp >= :startTime)
        AND (:endTime IS NULL OR t.requestTimestamp <= :endTime)
        AND (:status IS NULL OR t.conversionStatus = :status)
      ORDER BY t.requestTimestamp DESC
      """)
    Page<TransactionHistory> findByUserFilters(@Param("userId") Long userId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime,
                           @Param("status") ConversionStatus status,
                           Pageable pageable);

    @Query(value = """
      SELECT DATE(t.request_timestamp) AS day,
           COUNT(*) AS total,
           SUM(CASE WHEN t.conversion_status = 'SUCCESS' THEN 1 ELSE 0 END) AS success
      FROM transaction_history t
      WHERE t.user_id = :userId
      GROUP BY DATE(t.request_timestamp)
      ORDER BY DATE(t.request_timestamp) DESC
      """, nativeQuery = true)
    List<DailyConversionStatsProjection> findDailyStatsByUserId(@Param("userId") Long userId);

    @Query(value = """
      SELECT DATE(t.request_timestamp) AS day,
           COUNT(*) AS total,
           SUM(CASE WHEN t.conversion_status = 'SUCCESS' THEN 1 ELSE 0 END) AS success
      FROM transaction_history t
      GROUP BY DATE(t.request_timestamp)
      ORDER BY DATE(t.request_timestamp) DESC
      """, nativeQuery = true)
    List<DailyConversionStatsProjection> findDailyStatsGlobal();
}
