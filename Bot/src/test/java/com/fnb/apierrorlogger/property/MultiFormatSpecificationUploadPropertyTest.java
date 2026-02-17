package com.fnb.apierrorlogger.property;

import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import com.fnb.apierrorlogger.service.OpenAPIManager;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for multi-format OpenAPI specification upload.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class MultiFormatSpecificationUploadPropertyTest {

    @Autowired
    private OpenAPISpecificationRepository repository;

    private OpenAPIManager openAPIManager;

    /**
     * Property 15: Multi-format specification upload
     * **Validates: Requirements 5.1, 5.2**
     * 
     * For any valid OpenAPI specification in JSON or YAML format, the system 
     * should successfully parse and store the specification.
     */
    @Property(tries = 100)
    void multiFormatSpecificationUpload(
            @ForAll("validOpenAPISpecs") OpenAPISpecData specData
    ) {
        // Initialize the service for each test iteration
        openAPIManager = new OpenAPIManager(repository);
        
        // Upload the specification
        OpenAPISpecification result = openAPIManager.uploadSpecification(
                specData.apiIdentifier(),
                specData.specContent(),
                specData.uploadedBy()
        );
        
        // Assert that the specification was successfully stored
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getApiIdentifier()).isEqualTo(specData.apiIdentifier());
        assertThat(result.getSpecContent()).isEqualTo(specData.specContent());
        assertThat(result.getUploadedBy()).isEqualTo(specData.uploadedBy());
        assertThat(result.getUploadedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        
        // Assert that the version was extracted correctly
        assertThat(result.getVersion()).isNotNull();
        assertThat(result.getVersion()).isNotEmpty();
        
        // Verify the specification can be retrieved from the database
        OpenAPISpecification retrieved = repository.findById(result.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getApiIdentifier()).isEqualTo(specData.apiIdentifier());
        assertThat(retrieved.getSpecContent()).isEqualTo(specData.specContent());
        
        // Clean up for next iteration
        repository.deleteAll();
    }

    @Provide
    Arbitrary<OpenAPISpecData> validOpenAPISpecs() {
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
        
        // Generate API titles
        Arbitrary<String> apiTitles = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1) + " API");
        
        // Generate API versions
        Arbitrary<String> versions = Arbitraries.integers().between(1, 10)
                .flatMap(major -> Arbitraries.integers().between(0, 20)
                        .flatMap(minor -> Arbitraries.integers().between(0, 50)
                                .map(patch -> major + "." + minor + "." + patch)));
        
        // Generate endpoint paths
        Arbitrary<String> endpointPaths = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(15)
                .map(s -> "/" + s);
        
        // Generate endpoint descriptions
        Arbitrary<String> descriptions = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(30)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1));
        
        // Choose between JSON and YAML format
        Arbitrary<Boolean> isJson = Arbitraries.of(true, false);
        
        return Combinators.combine(
                apiIdentifiers,
                uploadedBy,
                apiTitles,
                versions,
                endpointPaths,
                descriptions,
                isJson
        ).as((apiId, user, title, version, path, desc, json) -> {
            String specContent;
            if (json) {
                // Generate valid JSON OpenAPI specification
                specContent = String.format("""
                        {
                          "openapi": "3.0.0",
                          "info": {
                            "title": "%s",
                            "version": "%s",
                            "description": "%s"
                          },
                          "paths": {
                            "%s": {
                              "get": {
                                "summary": "%s",
                                "responses": {
                                  "200": {
                                    "description": "Success"
                                  },
                                  "404": {
                                    "description": "Not found"
                                  }
                                }
                              }
                            }
                          }
                        }
                        """, title, version, desc, path, desc);
            } else {
                // Generate valid YAML OpenAPI specification
                specContent = String.format("""
                        openapi: 3.0.0
                        info:
                          title: %s
                          version: %s
                          description: %s
                        paths:
                          %s:
                            get:
                              summary: %s
                              responses:
                                '200':
                                  description: Success
                                '404':
                                  description: Not found
                        """, title, version, desc, path, desc);
            }
            return new OpenAPISpecData(apiId, specContent, user);
        });
    }

    /**
     * Record to hold OpenAPI specification test data.
     */
    record OpenAPISpecData(String apiIdentifier, String specContent, String uploadedBy) {}
}
