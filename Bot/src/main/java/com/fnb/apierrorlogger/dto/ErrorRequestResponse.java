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
public class ErrorRequestResponse {
    
    private UUID id;
    private String apiEndpoint;
    private String httpMethod;
    private String requestPayload;
    private Integer responseStatus;
    private String responseBody;
    private LocalDateTime timestamp;
    private String environment;
    private UUID openApiSpecId;
    private String validationStatus;
    private String validationDetails;
    private Boolean emailSent;
    private String emailDeliveryStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ErrorRequestResponse fromEntity(ErrorRequest entity) {
        return ErrorRequestResponse.builder()
                .id(entity.getId())
                .apiEndpoint(entity.getApiEndpoint())
                .httpMethod(entity.getHttpMethod())
                .requestPayload(entity.getRequestPayload())
                .responseStatus(entity.getResponseStatus())
                .responseBody(entity.getResponseBody())
                .timestamp(entity.getTimestamp())
                .environment(entity.getEnvironment())
                .openApiSpecId(entity.getOpenApiSpecId())
                .validationStatus(entity.getValidationStatus())
                .validationDetails(entity.getValidationDetails())
                .emailSent(entity.getEmailSent())
                .emailDeliveryStatus(entity.getEmailDeliveryStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
