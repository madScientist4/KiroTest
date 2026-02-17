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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
import static org.mockito.Mockito.*;

/**
 * Property-based tests for email failure handling.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class EmailFailureHandlingPropertyTest {

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
     * Property 10: Email failure handling
     * **Validates: Requirements 3.4**
     * 
     * For any error request where email delivery fails, the system should 
     * log the failure and update the request's email delivery status to "failed".
     */
    @Property(tries = 100)
    void emailFailureHandling(
            @ForAll("validErrorRequests") RequestWithSpecData data,
            @ForAll("failureTypes") FailureType failureType
    ) throws MessagingException {
        // Initialize services for each test iteration
        validationEngine = new ValidationEngine();
        emailService = new EmailService(mailSender, emailLogRepository);
        
        // Simulate email failure based on failure type
        switch (failureType) {
            case MESSAGING_EXCEPTION:
                // Simulate MessagingException during email creation
                when(mailSender.createMimeMessage()).thenThrow(
                        new MessagingException("SMTP connection failed")
                );
                break;
                
            case SEND_EXCEPTION:
                // Simulate exception during send
                MimeMessage mockMessage = mock(MimeMessage.class);
                when(mailSender.createMimeMessage()).thenReturn(mockMessage);
                doThrow(new MessagingException("Failed to send email"))
                        .when(mailSender).send(any(MimeMessage.class));
                break;
                
            case RUNTIME_EXCEPTION:
                // Simulate unexpected runtime exception
                when(mailSender.createMimeMessage()).thenThrow(
                        new RuntimeException("Unexpected error")
                );
                break;
        }
        
        // Save the specification first
        OpenAPISpecification savedSpec = specificationRepository.save(data.specification());
        
        // Create and save the error request
        ErrorRequest errorRequest = data.errorRequest();
        errorRequest.setOpenApiSpecId(savedSpec.getId());
        ErrorRequest savedRequest = errorRequestRepository.save(errorRequest);
        
        // Perform validation (should pass for this test)
        ValidationResult validationResult = validationEngine.validateRequest(
                savedRequest,
                savedSpec
        );
        
        // Only test if validation passes (since email is only sent on validation pass)
        if (validationResult.isValid()) {
            // Attempt to send email notification (should fail)
            boolean emailSent = emailService.sendErrorNotification(savedRequest, validationResult);
            
            // Verify email sending failed
            assertThat(emailSent)
                    .as("Email sending should fail when exception occurs")
                    .isFalse();
            
            // Verify email log was created with failure status
            List<EmailLog> emailLogs = emailLogRepository.findByErrorRequestId(savedRequest.getId());
            
            assertThat(emailLogs)
                    .as("Email log should be created even when sending fails")
                    .isNotEmpty();
            
            EmailLog emailLog = emailLogs.get(0);
            
            // Verify failure was logged
            assertThat(emailLog.getDeliveryStatus())
                    .as("Email delivery status should be 'failed'")
                    .isEqualTo("failed");
            
            assertThat(emailLog.getErrorMessage())
                    .as("Error message should be logged")
                    .isNotNull()
                    .isNotBlank();
            
            // Verify error message contains relevant information
            String errorMessage = emailLog.getErrorMessage();
            assertThat(errorMessage)
                    .as("Error message should describe the failure")
                    .satisfiesAnyOf(
                            msg -> assertThat(msg).containsIgnoringCase("failed"),
                            msg -> assertThat(msg).containsIgnoringCase("error"),
                            msg -> assertThat(msg).containsIgnoringCase("exception")
                    );
            
            // Verify the email log has correct error request ID
            assertThat(emailLog.getErrorRequestId())
                    .as("Email log should reference the correct error request")
                    .isEqualTo(savedRequest.getId());
            
            // Verify the email log has a timestamp
            assertThat(emailLog.getSentAt())
                    .as("Email log should have a timestamp")
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }
        
        // Clean up for next iteration
        emailLogRepository.deleteAll();
        errorRequestRepository.deleteAll();
        specificationRepository.deleteAll();
    }

    @Provide
    Arbitrary<RequestWithSpecData> validErrorRequests() {
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
            // Create OpenAPI specification that matches the request
            // This ensures validation will pass
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
    
    @Provide
    Arbitrary<FailureType> failureTypes() {
        return Arbitraries.of(FailureType.values());
    }

    /**
     * Record to hold error request with specification.
     */
    record RequestWithSpecData(
            ErrorRequest errorRequest,
            OpenAPISpecification specification
    ) {}
    
    /**
     * Enum representing different types of email failures.
     */
    enum FailureType {
        MESSAGING_EXCEPTION,  // MessagingException during message creation
        SEND_EXCEPTION,       // MessagingException during send
        RUNTIME_EXCEPTION     // Unexpected runtime exception
    }
}
