package com.swiftbridge.orchestrator.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftbridge.orchestrator.dto.history.HistoryItemDTO;
import com.swiftbridge.orchestrator.dto.history.ConversionAuditDTO;
import com.swiftbridge.orchestrator.entity.TransactionHistory;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionHistoryMapper {
    ObjectMapper objectMapper = new ObjectMapper();

     @Mapping(target = "username", source = "user.username")
    HistoryItemDTO toHistoryItemDTO(TransactionHistory entity);

     @Mapping(target = "username", source = "user.username")
     @Mapping(target = "conversionStatus", expression = "java(entity.getConversionStatus().name())")
     @Mapping(target = "requestTimestamp", expression = "java(entity.getRequestTimestamp() != null ? entity.getRequestTimestamp().toString() : null)")
    ConversionAuditDTO toAuditDTO(TransactionHistory entity);

    List<HistoryItemDTO> toHistoryItemDTOList(List<TransactionHistory> entities);

    default List<ConversionAuditDTO.ValidationError> map(String value) {
        try {
            if (value == null || value.isEmpty()) {
                return List.of();
            }

            return objectMapper.readValue(
                value,
                objectMapper.getTypeFactory().constructCollectionType(
                    List.class,
                    ConversionAuditDTO.ValidationError.class
                )
            );

        } catch (Exception e) {
            return List.of(); // أو throw exception
        }
    }
}
