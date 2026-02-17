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
 * Property-based tests for OpenAPI specification update persistence.
 * Feature: api-error-logger
 */
@JqwikSpringSupport
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class SpecificationUpdatePersistencePropertyTest {

    @Autowired
    private OpenAPISpecificationRepository repository;

    private OpenAPIManager openAPIManager;

    /**
     * Property 18: Specification update persistence
     * **Validates: Requirements 5.5**
     * 
     * For any existing OpenAPI specification, updating it with new content 
     * should result in the updated content being persisted and retrievable.
     */
    @Property(tries = 100)
    void specificationUpdatePersistence(
            @ForAll("validOpenAPISpecPairs") SpecificationUpdateData updateData
    ) {
        // Initialize the service for each test iteration
        openAPIManager = new OpenAPIManager(repository);
        
        // Upload the initial specification
        OpenAPISpecification initial = openAPIManager.uploadSpecification(
                updateData.apiIdentifier(),
                updateData.initialSpecContent(),
                updateData.initialUploadedBy()
        );
        
        assertThat(initial).isNotNull();
        assertThat(initial.getId()).isNotNull();
        
        // Update the specification with new content
        OpenAPISpecification updated = openAPIManager.updateSpecification(
                initial.getId(),
                updateData.updatedSpecContent(),
                updateData.updatedBy()
        );
        
        // Assert that the update was successful
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(initial.getId());
        assertThat(updated.getApiIdentifier()).isEqualTo(updateData.apiIdentifier());
        assertThat(updated.getSpecContent()).isEqualTo(updateData.updatedSpecContent());
        assertThat(updated.getUploadedBy()).isEqualTo(updateData.updatedBy());
        assertThat(updated.getUpdatedAt()).isNotNull();
        
        // Assert that the version was updated correctly
        assertThat(updated.getVersion()).isNotNull();
        assertThat(updated.getVersion()).isNotEmpty();
        
        // Verify the updated specification can be retrieved from the database
        OpenAPISpecification retrieved = repository.findById(updated.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getApiIdentifier()).isEqualTo(updateData.apiIdentifier());
        assertThat(retrieved.getSpecContent()).isEqualTo(updateData.updatedSpecContent());
        assertThat(retrieved.getUploadedBy()).isEqualTo(updateData.updatedBy());
        
        // Verify the old content is no longer present
        assertThat(retrieved.getSpecContent()).isNotEqualTo(updateData.initialSpecContent());
        
        // Clean up for next iteration
        repository.deleteAll();
    }

    @Provide
    Arbitrary<SpecificationUpdateData> validOpenAPISpecPairs() {
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
        
        return Combinators.combine(
                apiIdentifiers,
                uploadedBy,
                uploadedBy,
                apiTitles,
                apiTitles,
                versions,
                versions,
                endpointPaths,
                endpointPaths,
                descriptions,
                descriptions
        ).as((apiId, initialUser, updatedUser, initialTitle, updatedTitle, 
              initialVersion, updatedVersion, initialPath, updatedPath, 
              initialDesc, updatedDesc) -> {
            
            // Generate initial JSON OpenAPI specification
            String initialSpecContent = String.format("""
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
                              }
                            }
                          }
                        }
                      }
                    }
                    """, initialTitle, initialVersion, initialDesc, initialPath, initialDesc);
            
            // Generate updated JSON OpenAPI specification with different content
            String updatedSpecContent = String.format("""
                    {
                      "openapi": "3.0.0",
                      "info": {
                        "title": "%s",
                        "version": "%s",
                        "description": "%s Updated"
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
                          },
                          "post": {
                            "summary": "Create %s",
                            "responses": {
                              "201": {
                                "description": "Created"
                              }
                            }
                          }
                        }
                      }
                    }
                    """, updatedTitle, updatedVersion, updatedDesc, updatedPath, updatedDesc, updatedDesc);
            
            return new SpecificationUpdateData(
                    apiId, 
                    initialSpecContent, 
                    initialUser, 
                    updatedSpecContent, 
                    updatedUser
            );
        });
    }

    /**
     * Record to hold specification update test data.
     */
    record SpecificationUpdateData(
            String apiIdentifier,
            String initialSpecContent,
            String initialUploadedBy,
            String updatedSpecContent,
            String updatedBy
    ) {}
}
