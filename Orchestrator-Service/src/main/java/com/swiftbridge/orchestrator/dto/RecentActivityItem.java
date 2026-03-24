package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentActivityItem {

    private String id;
    private String status;
    private LocalDateTime createdAt;
}
