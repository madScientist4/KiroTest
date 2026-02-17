package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.service.ValidationEngine;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for validation with missing specification.
 * Feature: api-error-logger
 */
public class ValidationWithMissingSpecificationPropertyTest {

    private ValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        validationEngine = new ValidationEngine();
    }

    /**
     * Property 7: Validation with missing specification
     * **Validates: Requirements 2.6**
     * 
     * For any error request referencing a non-existent OpenAPI specification, 
     * the validation engine should mark the request as "unable_to_validate" 
     * and log the missing specification.
     */
    @Property(tries = 100)
    void validationWithMissingSpecification(
            @ForAll("errorRequests") ErrorRequest errorRequest
    ) {
        // Validate the request with null specification (missing)
        ValidationResult result = validationEngine.validateRequest(errorRequest, null);
        
        // Assert that validation was performed
        assertThat(result).isNotNull();
        
        // Validation should fail when specification is missing
        assertThat(result.isValid())
                .as("Validation should fail when specification is missing")
                .isFalse();
        
        // Should have at least one error
        assertThat(result.getErrors())
                .as("Should have errors when specification is missing")
                .isNotEmpty();
        
        // Error should indicate missing specification
        assertThat(result.getErrors())
                .anyMatch(error -> 
                    error.getField().equals("openApiSpecification") &&
                    error.getMessage().toLowerCase().contains("not found")
                )
                .as("Error should indicate OpenAPI specification not found");
        
        // The error message should be descriptive
        result.getErrors().forEach(error -> {
            assertThat(error.getField())
                    .as("Error field should be set")
                    .isNotNull()
                    .isNotBlank();
            assertThat(error.getMessage())
                    .as("Error message should be descriptive")
                    .isNotNull()
                    .isNotBlank();
        });
    }

    @Provide
    Arbitrary<ErrorRequest> errorRequests() {
        // Generate endpoint paths
        Arbitrary<String> endpointPaths = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "/" + s);
        
        // Generate HTTP methods
        Arbitrary<String> httpMethods = Arbitraries.of("GET", "POST", "PUT", "DELETE", "PATCH");
        
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
                endpointPaths,
                httpMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                environments
        ).as((path, method, payload, status, body, env) -> 
            ErrorRequest.builder()
                    .id(UUID.randomUUID())
                    .apiEndpoint(path)
                    .httpMethod(method)
                    .requestPayload(payload)
                    .responseStatus(status)
                    .responseBody(body)
                    .timestamp(LocalDateTime.now())
                    .environment(env)
                    .openApiSpecId(UUID.randomUUID()) // Non-existent spec ID
                    .emailSent(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build()
        );
    }
}
