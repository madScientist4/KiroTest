package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import com.fnb.apierrorlogger.service.OpenAPIManager;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for invalid OpenAPI specification rejection.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class InvalidSpecificationRejectionPropertyTest {

    @Autowired
    private OpenAPISpecificationRepository repository;

    private OpenAPIManager openAPIManager;

    /**
     * Property 17: Invalid specification rejection
     * **Validates: Requirements 5.4**
     * 
     * For any invalid OpenAPI specification file (malformed JSON/YAML or invalid 
     * OpenAPI structure), the upload should be rejected with a descriptive error message.
     */
    @Property(tries = 100)
    void invalidSpecificationRejection(
            @ForAll("invalidOpenAPISpecs") InvalidSpecData specData
    ) {
        // Initialize the service for each test iteration
        openAPIManager = new OpenAPIManager(repository);
        
        // Attempt to upload the invalid specification
        assertThatThrownBy(() -> openAPIManager.uploadSpecification(
                specData.apiIdentifier(),
                specData.specContent(),
                specData.uploadedBy()
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(specData.expectedErrorFragment());
        
        // Verify that no specification was stored in the database
        assertThat(repository.findByApiIdentifier(specData.apiIdentifier())).isEmpty();
        
        // Clean up for next iteration
        repository.deleteAll();
    }

    @Provide
    Arbitrary<InvalidSpecData> invalidOpenAPISpecs() {
        // Generate API identifiers
        Arbitrary<String> apiIdentifiers = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(s -> "api-" + s);
        
        // Generate uploaded by usernames
        Arbitrary<String> uploadedBy = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "user-" + s);
        
        // Generate different types of invalid specifications
        Arbitrary<InvalidSpecType> invalidTypes = Arbitraries.of(InvalidSpecType.values());
        
        return Combinators.combine(apiIdentifiers, uploadedBy, invalidTypes)
                .as((apiId, user, type) -> {
                    String specContent;
                    String expectedError;
                    
                    switch (type) {
                        case EMPTY_CONTENT:
                            specContent = "";
                            expectedError = "cannot be empty";
                            break;
                            
                        case NULL_CONTENT:
                            specContent = null;
                            expectedError = "cannot be empty";
                            break;
                            
                        case WHITESPACE_ONLY:
                            specContent = "   \n\t  \n  ";
                            expectedError = "cannot be empty";
                            break;
                            
                        case MALFORMED_JSON:
                            specContent = "{ \"openapi\": \"3.0.0\", \"info\": { \"title\": \"Test\" }";
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case MALFORMED_YAML:
                            specContent = """
                                    openapi: 3.0.0
                                    info:
                                      title: Test
                                        version: 1.0.0
                                    """;
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case INVALID_JSON_STRUCTURE:
                            specContent = "{ \"not\": \"an\", \"openapi\": \"spec\" }";
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case INVALID_YAML_STRUCTURE:
                            specContent = """
                                    not: an
                                    openapi: spec
                                    """;
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case MISSING_OPENAPI_VERSION:
                            specContent = """
                                    {
                                      "info": {
                                        "title": "Test API",
                                        "version": "1.0.0"
                                      },
                                      "paths": {}
                                    }
                                    """;
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case MISSING_INFO_SECTION:
                            specContent = """
                                    {
                                      "openapi": "3.0.0",
                                      "paths": {}
                                    }
                                    """;
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case INVALID_OPENAPI_VERSION:
                            specContent = """
                                    {
                                      "openapi": "99.99.99",
                                      "info": {
                                        "title": "Test API",
                                        "version": "1.0.0"
                                      },
                                      "paths": {}
                                    }
                                    """;
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case RANDOM_TEXT:
                            specContent = "This is just random text, not JSON or YAML";
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        case INCOMPLETE_JSON:
                            specContent = "{ \"openapi\": \"3.0.0\"";
                            expectedError = "Invalid OpenAPI specification";
                            break;
                            
                        default:
                            specContent = "";
                            expectedError = "cannot be empty";
                    }
                    
                    return new InvalidSpecData(apiId, specContent, user, expectedError);
                });
    }

    /**
     * Enum representing different types of invalid specifications.
     */
    enum InvalidSpecType {
        EMPTY_CONTENT,
        NULL_CONTENT,
        WHITESPACE_ONLY,
        MALFORMED_JSON,
        MALFORMED_YAML,
        INVALID_JSON_STRUCTURE,
        INVALID_YAML_STRUCTURE,
        MISSING_OPENAPI_VERSION,
        MISSING_INFO_SECTION,
        INVALID_OPENAPI_VERSION,
        RANDOM_TEXT,
        INCOMPLETE_JSON
    }

    /**
     * Record to hold invalid OpenAPI specification test data.
     */
    record InvalidSpecData(
            String apiIdentifier, 
            String specContent, 
            String uploadedBy,
            String expectedErrorFragment
    ) {}
}
