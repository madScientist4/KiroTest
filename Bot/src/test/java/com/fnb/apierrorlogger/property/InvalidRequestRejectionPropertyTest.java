package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.dto.ErrorRequestCreateRequest;
import com.fnb.apierrorlogger.service.ErrorService;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for invalid request rejection.
 * Feature: api-error-logger
 * 
 * **Property 4: Invalid request rejection**
 * **Validates: Requirements 1.4**
 */
@JqwikSpringSupport
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class InvalidRequestRejectionPropertyTest {

    @Autowired
    private ErrorService errorService;

    /**
     * Property 4: Invalid request rejection
     * **Validates: Requirements 1.4**
     * 
     * For any error request missing one or more required fields (endpoint, method, 
     * payload, status, body, timestamp), the submission should be rejected with a 
     * descriptive error message indicating which fields are missing.
     */
    @Property(tries = 100)
    void invalidRequestRejection(
            @ForAll("invalidErrorRequests") ErrorRequestCreateRequest request,
            @ForAll("expectedMissingFields") String expectedMissingField
    ) {
        // Attempt to create the error request
        assertThatThrownBy(() -> errorService.createErrorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required fields")
                .hasMessageContaining(expectedMissingField);
    }

    @Provide
    Arbitrary<ErrorRequestCreateRequest> invalidErrorRequests() {
        // Generate requests with at least one missing required field
        return Arbitraries.integers().between(0, 6).flatMap(missingFieldIndex -> {
            ErrorRequestCreateRequest.ErrorRequestCreateRequestBuilder builder = ErrorRequestCreateRequest.builder();
            
            // Set all fields initially
            String endpoint = "/api/test";
            String method = "POST";
            String payload = "{\"test\": \"data\"}";
            Integer status = 500;
            String body = "{\"error\": \"test\"}";
            LocalDateTime timestamp = LocalDateTime.now();
            String environment = "test";
            
            // Remove one field based on the index
            switch (missingFieldIndex) {
                case 0: // Missing endpoint
                    builder.apiEndpoint(null)
                           .httpMethod(method)
                           .requestPayload(payload)
                           .responseStatus(status)
                           .responseBody(body)
                           .timestamp(timestamp)
                           .environment(environment);
                    break;
                case 1: // Missing method
                    builder.apiEndpoint(endpoint)
                           .httpMethod(null)
                           .requestPayload(payload)
                           .responseStatus(status)
                           .responseBody(body)
                           .timestamp(timestamp)
                           .environment(environment);
                    break;
                case 2: // Missing payload
                    builder.apiEndpoint(endpoint)
                           .httpMethod(method)
                           .requestPayload(null)
                           .responseStatus(status)
                           .responseBody(body)
                           .timestamp(timestamp)
                           .environment(environment);
                    break;
                case 3: // Missing status
                    builder.apiEndpoint(endpoint)
                           .httpMethod(method)
                           .requestPayload(payload)
                           .responseStatus(null)
                           .responseBody(body)
                           .timestamp(timestamp)
                           .environment(environment);
                    break;
                case 4: // Missing body
                    builder.apiEndpoint(endpoint)
                           .httpMethod(method)
                           .requestPayload(payload)
                           .responseStatus(status)
                           .responseBody(null)
                           .timestamp(timestamp)
                           .environment(environment);
                    break;
                case 5: // Missing timestamp
                    builder.apiEndpoint(endpoint)
                           .httpMethod(method)
                           .requestPayload(payload)
                           .responseStatus(status)
                           .responseBody(body)
                           .timestamp(null)
                           .environment(environment);
                    break;
                case 6: // Missing environment
                    builder.apiEndpoint(endpoint)
                           .httpMethod(method)
                           .requestPayload(payload)
                           .responseStatus(status)
                           .responseBody(body)
                           .timestamp(timestamp)
                           .environment(null);
                    break;
            }
            
            return Arbitraries.just(builder.build());
        });
    }

    @Provide
    Arbitrary<String> expectedMissingFields() {
        // Return the field names that could be missing
        return Arbitraries.of(
                "apiEndpoint",
                "httpMethod",
                "requestPayload",
                "responseStatus",
                "responseBody",
                "timestamp",
                "environment"
        );
    }
}
