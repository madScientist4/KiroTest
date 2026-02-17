package com.fnb.apierrorlogger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.apierrorlogger.dto.ErrorRequestCreateRequest;
import com.fnb.apierrorlogger.dto.ErrorRequestListResponse;
import com.fnb.apierrorlogger.dto.ErrorRequestResponse;
import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.repository.ErrorRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for orchestrating error request workflow.
 * Handles creation, validation, email notification, and retrieval of error requests.
 * 
 * Requirements: 1.1, 1.2, 1.4, 2.5, 3.1, 3.3, 6.1, 6.3, 6.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorService {
    
    private final ErrorRequestRepository errorRequestRepository;
    private final OpenAPIManager openAPIManager;
    private final ValidationEngine validationEngine;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new error request and orchestrate the validation and email workflow.
     * 
     * Steps:
     * 1. Validate required fields are present
     * 2. Associate request with OpenAPI specification
     * 3. Trigger validation engine
     * 4. Conditionally trigger email service based on validation result
     * 5. Update error request with validation and email status
     * 6. Return appropriate response to user
     * 
     * @param request The error request creation request
     * @return ErrorRequestResponse with complete details
     * @throws IllegalArgumentException if required fields are missing
     * 
     * Requirements: 1.1, 1.2, 1.4, 2.5, 3.1, 3.3
     */
    @Transactional
    public ErrorRequestResponse createErrorRequest(ErrorRequestCreateRequest request) {
        log.info("Creating error request for endpoint: {}", request.getApiEndpoint());
        
        // Validate required fields (Requirement 1.4)
        validateRequiredFields(request);
        
        // Associate request with OpenAPI specification (Requirement 1.2)
        OpenAPISpecification specification = null;
        UUID specId = null;
        
        if (request.getApiIdentifier() != null && !request.getApiIdentifier().trim().isEmpty()) {
            specification = openAPIManager.getSpecificationByApiIdentifier(request.getApiIdentifier())
                    .orElse(null);
            
            if (specification != null) {
                specId = specification.getId();
                log.info("Associated error request with OpenAPI specification: {}", request.getApiIdentifier());
            } else {
                log.warn("OpenAPI specification not found for API identifier: {}", request.getApiIdentifier());
            }
        }
        
        // Create error request entity (Requirement 1.1)
        ErrorRequest errorRequest = ErrorRequest.builder()
                .apiEndpoint(request.getApiEndpoint())
                .httpMethod(request.getHttpMethod())
                .requestPayload(request.getRequestPayload())
                .responseStatus(request.getResponseStatus())
                .responseBody(request.getResponseBody())
                .timestamp(request.getTimestamp())
                .environment(request.getEnvironment())
                .openApiSpecId(specId)
                .emailSent(false)
                .emailDeliveryStatus("not_sent")
                .build();
        
        // Save initial error request
        errorRequest = errorRequestRepository.save(errorRequest);
        log.info("Saved error request with ID: {}", errorRequest.getId());
        
        // Trigger validation engine (Requirement 2.5)
        ValidationResult validationResult = validationEngine.validateRequest(errorRequest, specification);
        
        // Update error request with validation status and details
        errorRequest.setValidationStatus(validationResult.isValid() ? "passed" : "failed");
        
        try {
            String validationDetailsJson = objectMapper.writeValueAsString(validationResult);
            errorRequest.setValidationDetails(validationDetailsJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize validation details", e);
            errorRequest.setValidationDetails("{\"error\": \"Failed to serialize validation details\"}");
        }
        
        // Conditionally trigger email service (Requirements 3.1, 3.3)
        if (validationResult.isValid()) {
            log.info("Validation passed, sending email notification");
            boolean emailSent = emailService.sendErrorNotification(errorRequest, validationResult);
            
            errorRequest.setEmailSent(emailSent);
            errorRequest.setEmailDeliveryStatus(emailSent ? "sent" : "failed");
        } else {
            log.info("Validation failed, skipping email notification");
            errorRequest.setEmailSent(false);
            errorRequest.setEmailDeliveryStatus("not_sent");
        }
        
        // Save updated error request
        errorRequest = errorRequestRepository.save(errorRequest);
        
        log.info("Completed error request processing. ID: {}, Validation: {}, Email: {}",
                errorRequest.getId(), errorRequest.getValidationStatus(), errorRequest.getEmailDeliveryStatus());
        
        return ErrorRequestResponse.fromEntity(errorRequest);
    }
    
    /**
     * List error requests with optional filtering.
     * 
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param endpoint Optional endpoint filter
     * @param validationStatus Optional validation status filter
     * @param environment Optional environment filter
     * @return List of error request summaries
     * 
     * Requirements: 6.1, 6.3
     */
    public List<ErrorRequestListResponse> listErrorRequests(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String endpoint,
            String validationStatus,
            String environment) {
        
        log.debug("Listing error requests with filters");
        
        List<ErrorRequest> errorRequests = errorRequestRepository.findByFilters(
                startDate, endDate, endpoint, validationStatus, environment);
        
        return errorRequests.stream()
                .map(ErrorRequestListResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get error request details by ID.
     * 
     * @param id The error request ID
     * @return ErrorRequestResponse with complete details
     * @throws IllegalArgumentException if error request not found
     * 
     * Requirement: 6.4
     */
    public ErrorRequestResponse getErrorRequest(UUID id) {
        log.debug("Retrieving error request: {}", id);
        
        ErrorRequest errorRequest = errorRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Error request with ID '" + id + "' not found"));
        
        return ErrorRequestResponse.fromEntity(errorRequest);
    }
    
    /**
     * Validate that all required fields are present in the request.
     * 
     * @param request The error request creation request
     * @throws IllegalArgumentException if any required field is missing
     * 
     * Requirement: 1.4
     */
    private void validateRequiredFields(ErrorRequestCreateRequest request) {
        List<String> missingFields = new java.util.ArrayList<>();
        
        if (request.getApiEndpoint() == null || request.getApiEndpoint().trim().isEmpty()) {
            missingFields.add("apiEndpoint");
        }
        if (request.getHttpMethod() == null || request.getHttpMethod().trim().isEmpty()) {
            missingFields.add("httpMethod");
        }
        if (request.getRequestPayload() == null || request.getRequestPayload().trim().isEmpty()) {
            missingFields.add("requestPayload");
        }
        if (request.getResponseStatus() == null) {
            missingFields.add("responseStatus");
        }
        if (request.getResponseBody() == null || request.getResponseBody().trim().isEmpty()) {
            missingFields.add("responseBody");
        }
        if (request.getTimestamp() == null) {
            missingFields.add("timestamp");
        }
        if (request.getEnvironment() == null || request.getEnvironment().trim().isEmpty()) {
            missingFields.add("environment");
        }
        
        if (!missingFields.isEmpty()) {
            String message = "Missing required fields: " + String.join(", ", missingFields);
            log.error("Validation failed: {}", message);
            throw new IllegalArgumentException(message);
        }
    }
}
