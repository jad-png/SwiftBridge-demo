package com.swiftbridge.orchestrator.repository;

import com.swiftbridge.orchestrator.entity.ConversionStatus;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistorySpecifications {
    public static Specification<TransactionHistory> userFilters(
            Long userId, LocalDateTime startTime, LocalDateTime endTime, ConversionStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestTimestamp"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestTimestamp"), endTime));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("conversionStatus"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}