package com.fnb.apierrorlogger.dto;

import com.fnb.apierrorlogger.model.ErrorRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorRequestListResponse {
    
    private UUID id;
    private String apiEndpoint;
    private String httpMethod;
    private LocalDateTime timestamp;
    private String environment;
    private String validationStatus;
    private Boolean emailSent;
    private String emailDeliveryStatus;
    
    public static ErrorRequestListResponse fromEntity(ErrorRequest entity) {
        return ErrorRequestListResponse.builder()
                .id(entity.getId())
                .apiEndpoint(entity.getApiEndpoint())
                .httpMethod(entity.getHttpMethod())
                .timestamp(entity.getTimestamp())
                .environment(entity.getEnvironment())
                .validationStatus(entity.getValidationStatus())
                .emailSent(entity.getEmailSent())
                .emailDeliveryStatus(entity.getEmailDeliveryStatus())
                .build();
    }
}
