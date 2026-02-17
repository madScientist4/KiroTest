package com.fnb.apierrorlogger.property;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.repository.ErrorRequestRepository;
import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import com.fnb.apierrorlogger.service.ValidationEngine;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for validation status update.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ValidationStatusUpdatePropertyTest {

    @Autowired
    private ErrorRequestRepository errorRequestRepository;
    
    @Autowired
    private OpenAPISpecificationRepository specificationRepository;
    
    private ValidationEngine validationEngine;
    private ObjectMapper objectMapper;

    /**
     * Property 6: Validation status update
     * **Validates: Requirements 2.5**
     * 
     * For any error request that undergoes validation, the request record 
     * should be updated with the validation status (passed/failed) and 
     * detailed validation results.
     */
    @Property(tries = 100)
    void validationStatusUpdate(
            @ForAll("errorRequestsWithSpecs") RequestWithSpecData data
    ) {
        // Initialize services for each test iteration
        validationEngine = new ValidationEngine();
        objectMapper = new ObjectMapper();
        
        // Save the specification first
        OpenAPISpecification savedSpec = specificationRepository.save(data.specification());
        
        // Create and save the error request
        ErrorRequest errorRequest = data.errorRequest();
        errorRequest.setOpenApiSpecId(savedSpec.getId());
        ErrorRequest savedRequest = errorRequestRepository.save(errorRequest);
        
        // Perform validation
        ValidationResult validationResult = validationEngine.validateRequest(
                savedRequest,
                savedSpec
        );
        
        // Update the error request with validation results
        String validationStatus = validationResult.isValid() ? "passed" : "failed";
        savedRequest.setValidationStatus(validationStatus);
        
        // Convert validation result to JSON for storage
        try {
            String validationDetailsJson = objectMapper.writeValueAsString(validationResult);
            savedRequest.setValidationDetails(validationDetailsJson);
        } catch (JsonProcessingException e) {
            savedRequest.setValidationDetails("{\"error\": \"Failed to serialize validation result\"}");
        }
        
        // Save the updated request
        ErrorRequest updatedRequest = errorRequestRepository.save(savedRequest);
        
        // Verify the validation status was updated
        assertThat(updatedRequest.getValidationStatus())
                .as("Validation status should be set")
                .isNotNull()
                .isIn("passed", "failed");
        
        // Verify validation details were stored
        assertThat(updatedRequest.getValidationDetails())
                .as("Validation details should be stored")
                .isNotNull()
                .isNotBlank();
        
        // Verify the status matches the validation result
        if (validationResult.isValid()) {
            assertThat(updatedRequest.getValidationStatus())
                    .as("Valid request should have 'passed' status")
                    .isEqualTo("passed");
        } else {
            assertThat(updatedRequest.getValidationStatus())
                    .as("Invalid request should have 'failed' status")
                    .isEqualTo("failed");
        }
        
        // Verify we can retrieve the updated request from database
        ErrorRequest retrieved = errorRequestRepository.findById(updatedRequest.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getValidationStatus()).isEqualTo(updatedRequest.getValidationStatus());
        assertThat(retrieved.getValidationDetails()).isEqualTo(updatedRequest.getValidationDetails());
        
        // Clean up for next iteration
        errorRequestRepository.deleteAll();
        specificationRepository.deleteAll();
    }

    @Provide
    Arbitrary<RequestWithSpecData> errorRequestsWithSpecs() {
        // Generate API identifiers
        Arbitrary<String> apiIdentifiers = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> "api-" + s);
        
        // Generate endpoint paths
        Arbitrary<String> endpointPaths = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "/" + s);
        
        // Generate HTTP methods
        Arbitrary<String> httpMethods = Arbitraries.of("GET", "POST", "PUT", "DELETE");
        
        // Generate response statuses
        Arbitrary<Integer> responseStatuses = Arbitraries.integers().between(200, 599);
        
        // Generate request payloads
        Arbitrary<String> requestPayloads = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> String.format("{\"data\": \"%s\"}", s));
        
        // Generate response bodies
        Arbitrary<String> responseBodies = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(s -> String.format("{\"message\": \"%s\"}", s));
        
        // Generate environments
        Arbitrary<String> environments = Arbitraries.of("development", "staging", "production");
        
        return Combinators.combine(
                apiIdentifiers,
                endpointPaths,
                httpMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                environments
        ).as((apiId, path, method, payload, status, body, env) -> {
            // Create OpenAPI specification
            String specContent = String.format("""
                    {
                      "openapi": "3.0.0",
                      "info": {
                        "title": "Test API",
                        "version": "1.0.0"
                      },
                      "paths": {
                        "%s": {
                          "%s": {
                            "summary": "Test operation",
                            "responses": {
                              "200": {
                                "description": "Success"
                              }
                            }
                          }
                        }
                      }
                    }
                    """, path, method.toLowerCase());
            
            OpenAPISpecification spec = OpenAPISpecification.builder()
                    .apiIdentifier(apiId)
                    .specContent(specContent)
                    .version("1.0.0")
                    .uploadedBy("test-user")
                    .build();
            
            ErrorRequest request = ErrorRequest.builder()
                    .apiEndpoint(path)
                    .httpMethod(method)
                    .requestPayload(payload)
                    .responseStatus(status)
                    .responseBody(body)
                    .timestamp(LocalDateTime.now())
                    .environment(env)
                    .emailSent(false)
                    .build();
            
            return new RequestWithSpecData(request, spec);
        });
    }

    /**
     * Record to hold error request with specification.
     */
    record RequestWithSpecData(
            ErrorRequest errorRequest,
            OpenAPISpecification specification
    ) {}
}
