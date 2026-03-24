package com.swiftbridge.orchestrator.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HistoryListResponse {

    private List<HistoryItemDTO> items;
    private PaginationDTO pagination;

    @Getter
    @Builder
    public static class PaginationDTO {
        private long total;
    }
}
