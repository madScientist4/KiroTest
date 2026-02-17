package com.fnb.apierrorlogger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorRequestCreateRequest {
    
    @NotBlank(message = "API endpoint is required")
    private String apiEndpoint;
    
    @NotBlank(message = "HTTP method is required")
    private String httpMethod;
    
    @NotBlank(message = "Request payload is required")
    private String requestPayload;
    
    @NotNull(message = "Response status is required")
    private Integer responseStatus;
    
    @NotBlank(message = "Response body is required")
    private String responseBody;
    
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;
    
    @NotBlank(message = "Environment is required")
    private String environment;
    
    private String apiIdentifier;
}
