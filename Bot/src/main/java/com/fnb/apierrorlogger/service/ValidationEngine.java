package com.fnb.apierrorlogger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.model.ValidationError;
import com.fnb.apierrorlogger.model.ValidationResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for validating error requests against OpenAPI specifications.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    /**
     * Validate an error request against its OpenAPI specification.
     * 
     * @param errorRequest The error request to validate
     * @param specification The OpenAPI specification to validate against (can be null)
     * @return ValidationResult with detailed errors
     * 
     * Requirements: 2.1, 2.2, 2.3, 2.4, 2.6
     */
    public ValidationResult validateRequest(ErrorRequest errorRequest, OpenAPISpecification specification) {
        log.info("Validating error request for endpoint: {}", errorRequest.getApiEndpoint());
        
        // Handle missing OpenAPI specification case (Requirement 2.6)
        if (specification == null) {
            log.warn("OpenAPI specification not found for error request");
            ValidationError error = ValidationError.builder()
                    .field("openApiSpecification")
                    .message("OpenAPI specification not found - unable to validate")
                    .build();
            return ValidationResult.failure(List.of(error));
        }
        
        try {
            // Parse the OpenAPI specification
            SwaggerParseResult parseResult = parser.readContents(specification.getSpecContent(), null, null);
            OpenAPI openAPI = parseResult.getOpenAPI();
            
            if (openAPI == null) {
                log.error("Failed to parse OpenAPI specification");
                ValidationError error = ValidationError.builder()
                        .field("openApiSpecification")
                        .message("Failed to parse OpenAPI specification")
                        .build();
                return ValidationResult.failure(List.of(error));
            }
            
            ValidationResult result = ValidationResult.builder()
                    .isValid(true)
                    .build();
            
            // Validate HTTP method is allowed for endpoint (Requirement 2.3)
            validateHttpMethod(errorRequest, openAPI, result);
            
            // Validate required parameters are present (Requirement 2.4)
            validateRequiredParameters(errorRequest, openAPI, result);
            
            // Validate request payload against schema (Requirement 2.2)
            validateRequestPayload(errorRequest, openAPI, result);
            
            log.info("Validation completed. Valid: {}, Errors: {}", result.isValid(), result.getErrors().size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during validation: {}", e.getMessage(), e);
            ValidationError error = ValidationError.builder()
                    .field("validation")
                    .message("Validation error: " + e.getMessage())
                    .build();
            return ValidationResult.failure(List.of(error));
        }
    }

    /**
     * Validate that the HTTP method is valid for the endpoint.
     * Requirement 2.3
     */
    private void validateHttpMethod(ErrorRequest errorRequest, OpenAPI openAPI, ValidationResult result) {
        String endpoint = errorRequest.getApiEndpoint();
        String method = errorRequest.getHttpMethod().toLowerCase();
        
        PathItem pathItem = findPathItem(openAPI, endpoint);
        
        if (pathItem == null) {
            result.addError(ValidationError.builder()
                    .field("apiEndpoint")
                    .message("Endpoint not found in OpenAPI specification")
                    .expectedType("valid endpoint")
                    .actualValue(endpoint)
                    .build());
            return;
        }
        
        Operation operation = getOperationForMethod(pathItem, method);
        
        if (operation == null) {
            List<String> allowedMethods = getAllowedMethods(pathItem);
            result.addError(ValidationError.builder()
                    .field("httpMethod")
                    .message("HTTP method not allowed for this endpoint")
                    .expectedType("one of: " + String.join(", ", allowedMethods))
                    .actualValue(method.toUpperCase())
                    .build());
        }
    }

    /**
     * Validate that required parameters are present.
     * Requirement 2.4
     */
    private void validateRequiredParameters(ErrorRequest errorRequest, OpenAPI openAPI, ValidationResult result) {
        String endpoint = errorRequest.getApiEndpoint();
        String method = errorRequest.getHttpMethod().toLowerCase();
        
        PathItem pathItem = findPathItem(openAPI, endpoint);
        if (pathItem == null) {
            return; // Already reported in method validation
        }
        
        Operation operation = getOperationForMethod(pathItem, method);
        if (operation == null) {
            return; // Already reported in method validation
        }
        
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null || parameters.isEmpty()) {
            return; // No required parameters
        }
        
        // Check for required parameters
        for (Parameter parameter : parameters) {
            if (Boolean.TRUE.equals(parameter.getRequired())) {
                // For simplicity, we'll add a warning if required parameters exist
                // In a real implementation, we'd parse the request to check for these
                result.addWarning("Required parameter '" + parameter.getName() + "' should be present");
            }
        }
    }

    /**
     * Validate request payload against OpenAPI schema.
     * Requirement 2.2
     */
    private void validateRequestPayload(ErrorRequest errorRequest, OpenAPI openAPI, ValidationResult result) {
        String endpoint = errorRequest.getApiEndpoint();
        String method = errorRequest.getHttpMethod().toLowerCase();
        String requestPayload = errorRequest.getRequestPayload();
        
        // Skip validation if no payload
        if (requestPayload == null || requestPayload.trim().isEmpty()) {
            return;
        }
        
        PathItem pathItem = findPathItem(openAPI, endpoint);
        if (pathItem == null) {
            return; // Already reported in method validation
        }
        
        Operation operation = getOperationForMethod(pathItem, method);
        if (operation == null) {
            return; // Already reported in method validation
        }
        
        // Check if operation expects a request body
        if (operation.getRequestBody() == null) {
            result.addWarning("Request body provided but not expected by OpenAPI specification");
            return;
        }
        
        // Validate payload is valid JSON
        try {
            JsonNode payloadNode = objectMapper.readTree(requestPayload);
            
            // Get the schema for the request body
            Content content = operation.getRequestBody().getContent();
            if (content != null) {
                MediaType mediaType = content.get("application/json");
                if (mediaType != null && mediaType.getSchema() != null) {
                    Schema schema = mediaType.getSchema();
                    validateJsonAgainstSchema(payloadNode, schema, result);
                }
            }
            
        } catch (Exception e) {
            result.addError(ValidationError.builder()
                    .field("requestPayload")
                    .message("Invalid JSON payload: " + e.getMessage())
                    .expectedType("valid JSON")
                    .actualValue(requestPayload)
                    .build());
        }
    }

    /**
     * Validate JSON payload against OpenAPI schema.
     * This is a simplified validation - a full implementation would use a JSON schema validator.
     */
    private void validateJsonAgainstSchema(JsonNode payload, Schema schema, ValidationResult result) {
        // Simplified schema validation
        // In a production system, use a proper JSON schema validator library
        
        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            for (String requiredField : schema.getRequired()) {
                if (!payload.has(requiredField)) {
                    result.addError(ValidationError.builder()
                            .field("requestPayload." + requiredField)
                            .message("Required field missing")
                            .expectedType("required")
                            .actualValue("missing")
                            .build());
                }
            }
        }
    }

    /**
     * Find the PathItem for a given endpoint.
     * Handles exact matches and path parameters.
     */
    private PathItem findPathItem(OpenAPI openAPI, String endpoint) {
        if (openAPI.getPaths() == null) {
            return null;
        }
        
        // Try exact match first
        PathItem pathItem = openAPI.getPaths().get(endpoint);
        if (pathItem != null) {
            return pathItem;
        }
        
        // Try to match with path parameters (e.g., /users/{id})
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            String pathPattern = entry.getKey();
            if (matchesPathPattern(endpoint, pathPattern)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Check if an endpoint matches a path pattern with parameters.
     */
    private boolean matchesPathPattern(String endpoint, String pattern) {
        // Simple pattern matching for path parameters
        // e.g., /users/123 matches /users/{id}
        String[] endpointParts = endpoint.split("/");
        String[] patternParts = pattern.split("/");
        
        if (endpointParts.length != patternParts.length) {
            return false;
        }
        
        for (int i = 0; i < patternParts.length; i++) {
            String patternPart = patternParts[i];
            String endpointPart = endpointParts[i];
            
            // Skip parameter placeholders
            if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                continue;
            }
            
            // Must match exactly
            if (!patternPart.equals(endpointPart)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get the Operation for a specific HTTP method.
     */
    private Operation getOperationForMethod(PathItem pathItem, String method) {
        return switch (method.toLowerCase()) {
            case "get" -> pathItem.getGet();
            case "post" -> pathItem.getPost();
            case "put" -> pathItem.getPut();
            case "delete" -> pathItem.getDelete();
            case "patch" -> pathItem.getPatch();
            case "head" -> pathItem.getHead();
            case "options" -> pathItem.getOptions();
            default -> null;
        };
    }

    /**
     * Get list of allowed HTTP methods for a path.
     */
    private List<String> getAllowedMethods(PathItem pathItem) {
        List<String> methods = new ArrayList<>();
        if (pathItem.getGet() != null) methods.add("GET");
        if (pathItem.getPost() != null) methods.add("POST");
        if (pathItem.getPut() != null) methods.add("PUT");
        if (pathItem.getDelete() != null) methods.add("DELETE");
        if (pathItem.getPatch() != null) methods.add("PATCH");
        if (pathItem.getHead() != null) methods.add("HEAD");
        if (pathItem.getOptions() != null) methods.add("OPTIONS");
        return methods;
    }
}
