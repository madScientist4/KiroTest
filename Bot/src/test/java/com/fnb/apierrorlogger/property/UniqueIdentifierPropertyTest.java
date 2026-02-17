package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.repository.ErrorRequestRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for unique identifier assignment.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class UniqueIdentifierPropertyTest {

    @Autowired
    private ErrorRequestRepository errorRequestRepository;

    /**
     * Property 2: Unique identifier assignment
     * **Validates: Requirements 1.3**
     * 
     * For any set of error requests created, all assigned identifiers should be 
     * unique across the entire set.
     */
    @Property(tries = 100)
    void uniqueIdentifierAssignment(
            @ForAll("errorRequestLists") List<ErrorRequest> errorRequests
    ) {
        // Save all error requests
        List<ErrorRequest> savedRequests = errorRequests.stream()
                .map(errorRequestRepository::save)
                .collect(Collectors.toList());
        errorRequestRepository.flush();
        
        // Extract all IDs
        List<UUID> ids = savedRequests.stream()
                .map(ErrorRequest::getId)
                .collect(Collectors.toList());
        
        // Assert that all IDs are non-null
        assertThat(ids).doesNotContainNull();
        
        // Assert that all IDs are unique by comparing list size with set size
        Set<UUID> uniqueIds = new HashSet<>(ids);
        assertThat(uniqueIds).hasSize(ids.size());
        
        // Alternative assertion: no duplicates
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Provide
    Arbitrary<List<ErrorRequest>> errorRequestLists() {
        return validErrorRequests().list().ofMinSize(2).ofMaxSize(20);
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
