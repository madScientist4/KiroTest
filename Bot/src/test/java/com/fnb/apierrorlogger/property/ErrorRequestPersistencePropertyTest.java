package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.repository.ErrorRequestRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for error request persistence.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ErrorRequestPersistencePropertyTest {

    @Autowired
    private ErrorRequestRepository errorRequestRepository;

    /**
     * Property 1: Error request persistence round-trip
     * **Validates: Requirements 1.1, 1.5**
     * 
     * For any valid error request with all required fields, creating the request 
     * should result in a persisted record that can be immediately retrieved from 
     * the database with the same data.
     */
    @Property(tries = 100)
    void errorRequestPersistenceRoundTrip(
            @ForAll("validErrorRequests") ErrorRequest errorRequest
    ) {
        // Create and persist the error request
        ErrorRequest saved = errorRequestRepository.save(errorRequest);
        errorRequestRepository.flush();
        
        // Retrieve the error request from the database
        ErrorRequest retrieved = errorRequestRepository.findById(saved.getId()).orElse(null);
        
        // Assert that the retrieved request is not null
        assertThat(retrieved).isNotNull();
        
        // Assert that all fields match
        assertThat(retrieved.getApiEndpoint()).isEqualTo(errorRequest.getApiEndpoint());
        assertThat(retrieved.getHttpMethod()).isEqualTo(errorRequest.getHttpMethod());
        assertThat(retrieved.getRequestPayload()).isEqualTo(errorRequest.getRequestPayload());
        assertThat(retrieved.getResponseStatus()).isEqualTo(errorRequest.getResponseStatus());
        assertThat(retrieved.getResponseBody()).isEqualTo(errorRequest.getResponseBody());
        assertThat(retrieved.getTimestamp()).isEqualTo(errorRequest.getTimestamp());
        assertThat(retrieved.getEnvironment()).isEqualTo(errorRequest.getEnvironment());
        assertThat(retrieved.getOpenApiSpecId()).isEqualTo(errorRequest.getOpenApiSpecId());
        assertThat(retrieved.getValidationStatus()).isEqualTo(errorRequest.getValidationStatus());
        assertThat(retrieved.getValidationDetails()).isEqualTo(errorRequest.getValidationDetails());
        assertThat(retrieved.getEmailSent()).isEqualTo(errorRequest.getEmailSent());
        assertThat(retrieved.getEmailDeliveryStatus()).isEqualTo(errorRequest.getEmailDeliveryStatus());
        
        // Assert that timestamps were set
        assertThat(retrieved.getCreatedAt()).isNotNull();
        assertThat(retrieved.getUpdatedAt()).isNotNull();
    }

    @Provide
    Arbitrary<ErrorRequest> validErrorRequests() {
        Arbitrary<String> apiEndpoints = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(100)
                .map(s -> "/api/" + s);
        
        Arbitrary<String> httpMethods = Arbitraries.of("GET", "POST", "PUT", "DELETE", "PATCH");
        
        Arbitrary<String> requestPayloads = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(500)
                .map(s -> "{\"data\": \"" + s + "\"}");
        
        Arbitrary<Integer> responseStatuses = Arbitraries.integers().between(100, 599);
        
        Arbitrary<String> responseBodies = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(500)
                .map(s -> "{\"error\": \"" + s + "\"}");
        
        Arbitrary<LocalDateTime> timestamps = Arbitraries.longs()
                .between(
                        LocalDateTime.of(2020, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC),
                        LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC)
                )
                .map(seconds -> LocalDateTime.ofEpochSecond(seconds, 0, java.time.ZoneOffset.UTC));
        
        Arbitrary<String> environments = Arbitraries.of("development", "staging", "production", "test");
        
        Arbitrary<UUID> openApiSpecIds = Arbitraries.create(() -> UUID.randomUUID());
        
        Arbitrary<String> validationStatuses = Arbitraries.of("passed", "failed", "unable_to_validate", null);
        
        Arbitrary<String> validationDetails = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(200)
                .map(s -> "{\"details\": \"" + s + "\"}")
                .injectNull(0.3);
        
        Arbitrary<Boolean> emailSent = Arbitraries.of(true, false);
        
        Arbitrary<String> emailDeliveryStatuses = Arbitraries.of("sent", "failed", "not_sent", null);
        
        return Combinators.combine(
                apiEndpoints,
                httpMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                timestamps,
                environments,
                openApiSpecIds,
                validationStatuses,
                validationDetails,
                emailSent,
                emailDeliveryStatuses
        ).as((endpoint, method, payload, status, body, timestamp, env, specId, valStatus, valDetails, sent, deliveryStatus) ->
                ErrorRequest.builder()
                        .apiEndpoint(endpoint)
                        .httpMethod(method)
                        .requestPayload(payload)
                        .responseStatus(status)
                        .responseBody(body)
                        .timestamp(timestamp)
                        .environment(env)
                        .openApiSpecId(specId)
                        .validationStatus(valStatus)
                        .validationDetails(valDetails)
                        .emailSent(sent)
                        .emailDeliveryStatus(deliveryStatus)
                        .build()
        );
    }
}
