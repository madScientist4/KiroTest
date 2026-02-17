package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.service.ValidationEngine;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for comprehensive request validation.
 * Feature: api-error-logger
 */
public class ComprehensiveRequestValidationPropertyTest {

    private ValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        validationEngine = new ValidationEngine();
    }

    /**
     * Property 5: Comprehensive request validation
     * **Validates: Requirements 2.2, 2.3, 2.4**
     * 
     * For any error request and its corresponding OpenAPI specification, 
     * the validation engine should verify: (1) the request payload matches 
     * the schema, (2) the HTTP method is valid for the endpoint, and 
     * (3) all required parameters are present.
     */
    @Property(tries = 100)
    void comprehensiveRequestValidation(
            @ForAll("validRequestsWithSpecs") RequestWithSpec requestWithSpec
    ) {
        // Validate the request against the specification
        ValidationResult result = validationEngine.validateRequest(
                requestWithSpec.errorRequest(),
                requestWithSpec.specification()
        );
        
        // Assert that validation was performed
        assertThat(result).isNotNull();
        
        // For valid requests, validation should pass
        if (requestWithSpec.shouldBeValid()) {
            assertThat(result.isValid())
                    .as("Valid request should pass validation")
                    .isTrue();
            assertThat(result.getErrors())
                    .as("Valid request should have no errors")
                    .isEmpty();
        } else {
            // For invalid requests, validation should fail with specific errors
            assertThat(result.isValid())
                    .as("Invalid request should fail validation")
                    .isFalse();
            assertThat(result.getErrors())
                    .as("Invalid request should have errors")
                    .isNotEmpty();
            
            // Verify error messages are descriptive
            result.getErrors().forEach(error -> {
                assertThat(error.getField()).isNotNull();
                assertThat(error.getMessage()).isNotBlank();
            });
        }
    }

    @Provide
    Arbitrary<RequestWithSpec> validRequestsWithSpecs() {
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
        Arbitrary<String> httpMethods = Arbitraries.of("GET", "POST", "PUT", "DELETE", "PATCH");
        
        // Generate invalid HTTP methods for testing
        Arbitrary<String> invalidMethods = Arbitraries.of("INVALID", "TRACE", "CONNECT");
        
        // Generate response statuses
        Arbitrary<Integer> responseStatuses = Arbitraries.integers().between(200, 599);
        
        // Generate request payloads
        Arbitrary<String> requestPayloads = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> String.format("{\"data\": \"%s\", \"id\": %d}", s, s.hashCode()));
        
        // Generate response bodies
        Arbitrary<String> responseBodies = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(s -> String.format("{\"message\": \"%s\"}", s));
        
        // Generate environments
        Arbitrary<String> environments = Arbitraries.of("development", "staging", "production");
        
        // Generate valid request/spec pairs
        Arbitrary<RequestWithSpec> validPairs = Combinators.combine(
                apiIdentifiers,
                endpointPaths,
                httpMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                environments
        ).as((apiId, path, method, payload, status, body, env) -> {
            // Create OpenAPI specification that matches the request
            String specContent = createOpenAPISpec(path, method, true);
            
            OpenAPISpecification spec = OpenAPISpecification.builder()
                    .id(UUID.randomUUID())
                    .apiIdentifier(apiId)
                    .specContent(specContent)
                    .version("1.0.0")
                    .uploadedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            ErrorRequest request = ErrorRequest.builder()
                    .id(UUID.randomUUID())
                    .apiEndpoint(path)
                    .httpMethod(method)
                    .requestPayload(payload)
                    .responseStatus(status)
                    .responseBody(body)
                    .timestamp(LocalDateTime.now())
                    .environment(env)
                    .openApiSpecId(spec.getId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            return new RequestWithSpec(request, spec, true);
        });
        
        // Generate invalid request/spec pairs (wrong HTTP method)
        Arbitrary<RequestWithSpec> invalidMethodPairs = Combinators.combine(
                apiIdentifiers,
                endpointPaths,
                invalidMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                environments
        ).as((apiId, path, method, payload, status, body, env) -> {
            // Create OpenAPI specification with only GET method
            String specContent = createOpenAPISpec(path, "GET", true);
            
            OpenAPISpecification spec = OpenAPISpecification.builder()
                    .id(UUID.randomUUID())
                    .apiIdentifier(apiId)
                    .specContent(specContent)
                    .version("1.0.0")
                    .uploadedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            ErrorRequest request = ErrorRequest.builder()
                    .id(UUID.randomUUID())
                    .apiEndpoint(path)
                    .httpMethod(method)
                    .requestPayload(payload)
                    .responseStatus(status)
                    .responseBody(body)
                    .timestamp(LocalDateTime.now())
                    .environment(env)
                    .openApiSpecId(spec.getId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            return new RequestWithSpec(request, spec, false);
        });
        
        // Combine valid and invalid pairs
        return Arbitraries.frequencyOf(
                Tuple.of(7, validPairs),    // 70% valid
                Tuple.of(3, invalidMethodPairs)  // 30% invalid
        );
    }

    /**
     * Create an OpenAPI specification for testing.
     */
    private String createOpenAPISpec(String path, String method, boolean includeRequestBody) {
        String requestBodySection = "";
        if (includeRequestBody && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            requestBodySection = """
                    ,
                    "requestBody": {
                      "required": true,
                      "content": {
                        "application/json": {
                          "schema": {
                            "type": "object",
                            "properties": {
                              "data": {
                                "type": "string"
                              },
                              "id": {
                                "type": "integer"
                              }
                            },
                            "required": ["data"]
                          }
                        }
                      }
                    }
                    """;
        }
        
        return String.format("""
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
                        }%s
                      }
                    }
                  }
                }
                """, path, method.toLowerCase(), requestBodySection);
    }

    /**
     * Record to hold request with specification and validity flag.
     */
    record RequestWithSpec(
            ErrorRequest errorRequest,
            OpenAPISpecification specification,
            boolean shouldBeValid
    ) {}
}
