package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.EmailLog;
import com.fnb.apierrorlogger.model.ErrorRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.model.ValidationResult;
import com.fnb.apierrorlogger.repository.EmailLogRepository;
import com.fnb.apierrorlogger.repository.ErrorRequestRepository;
import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import com.fnb.apierrorlogger.service.EmailService;
import com.fnb.apierrorlogger.service.ValidationEngine;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for conditional email sending.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ConditionalEmailSendingPropertyTest {

    @Autowired
    private ErrorRequestRepository errorRequestRepository;
    
    @Autowired
    private OpenAPISpecificationRepository specificationRepository;
    
    @Autowired
    private EmailLogRepository emailLogRepository;
    
    @MockBean
    private JavaMailSender mailSender;
    
    private ValidationEngine validationEngine;
    private EmailService emailService;

    /**
     * Property 8: Conditional email sending
     * **Validates: Requirements 3.1, 3.3**
     * 
     * For any error request, an email notification should be sent to the 
     * investigation team if and only if the request passes validation.
     */
    @Property(tries = 100)
    void conditionalEmailSending(
            @ForAll("errorRequestsWithSpecs") RequestWithSpecData data
    ) {
        // Initialize services for each test iteration
        validationEngine = new ValidationEngine();
        emailService = new EmailService(mailSender, emailLogRepository);
        
        // Mock the mail sender to simulate successful email sending
        when(mailSender.createMimeMessage()).thenReturn(new org.springframework.mail.javamail.JavaMailSenderImpl().createMimeMessage());
        
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
        
        // Conditionally send email based on validation result
        boolean emailSent = false;
        if (validationResult.isValid()) {
            emailSent = emailService.sendErrorNotification(savedRequest, validationResult);
        }
        
        // Verify email was sent if and only if validation passed
        List<EmailLog> emailLogs = emailLogRepository.findByErrorRequestId(savedRequest.getId());
        
        if (validationResult.isValid()) {
            // Validation passed - email should have been attempted
            assertThat(emailSent)
                    .as("Email should be sent when validation passes")
                    .isTrue();
            
            assertThat(emailLogs)
                    .as("Email log should exist when validation passes")
                    .isNotEmpty();
            
            EmailLog emailLog = emailLogs.get(0);
            assertThat(emailLog.getDeliveryStatus())
                    .as("Email delivery status should be 'sent'")
                    .isEqualTo("sent");
        } else {
            // Validation failed - email should NOT have been sent
            assertThat(emailSent)
                    .as("Email should not be sent when validation fails")
                    .isFalse();
            
            assertThat(emailLogs)
                    .as("No email log should exist when validation fails")
                    .isEmpty();
        }
        
        // Clean up for next iteration
        emailLogRepository.deleteAll();
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
        
        // Generate boolean to determine if spec should match request (for validation pass/fail)
        Arbitrary<Boolean> shouldMatch = Arbitraries.of(true, false);
        
        return Combinators.combine(
                apiIdentifiers,
                endpointPaths,
                httpMethods,
                requestPayloads,
                responseStatuses,
                responseBodies,
                environments,
                shouldMatch
        ).as((apiId, path, method, payload, status, body, env, match) -> {
            // Create OpenAPI specification
            // If match is true, spec will match the request (validation passes)
            // If match is false, spec will have different endpoint (validation fails)
            String specPath = match ? path : "/different-path";
            String specMethod = match ? method : "GET";
            
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
                    """, specPath, specMethod.toLowerCase());
            
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
