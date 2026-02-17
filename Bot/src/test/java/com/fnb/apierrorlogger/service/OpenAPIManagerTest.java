package com.fnb.apierrorlogger.service;

import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OpenAPIManager service.
 * Tests basic functionality and edge cases.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class OpenAPIManagerTest {

    @Autowired
    private OpenAPISpecificationRepository repository;

    private OpenAPIManager openAPIManager;

    // Valid OpenAPI 3.0 specification in JSON format
    private static final String VALID_JSON_SPEC = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0"
              },
              "paths": {
                "/test": {
                  "get": {
                    "summary": "Test endpoint",
                    "responses": {
                      "200": {
                        "description": "Success"
                      }
                    }
                  }
                }
              }
            }
            """;

    // Valid OpenAPI 3.0 specification in YAML format
    private static final String VALID_YAML_SPEC = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 2.0.0
            paths:
              /test:
                get:
                  summary: Test endpoint
                  responses:
                    '200':
                      description: Success
            """;

    // Invalid specification (missing required fields)
    private static final String INVALID_SPEC = """
            {
              "invalid": "spec"
            }
            """;

    @BeforeEach
    void setUp() {
        openAPIManager = new OpenAPIManager(repository);
        repository.deleteAll();
    }

    @Test
    void uploadSpecification_withValidJson_shouldSucceed() {
        // When
        OpenAPISpecification result = openAPIManager.uploadSpecification(
                "test-api",
                VALID_JSON_SPEC,
                "test-user"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getApiIdentifier()).isEqualTo("test-api");
        assertThat(result.getSpecContent()).isEqualTo(VALID_JSON_SPEC);
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getUploadedBy()).isEqualTo("test-user");
        assertThat(result.getUploadedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void uploadSpecification_withValidYaml_shouldSucceed() {
        // When
        OpenAPISpecification result = openAPIManager.uploadSpecification(
                "test-api-yaml",
                VALID_YAML_SPEC,
                "test-user"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiIdentifier()).isEqualTo("test-api-yaml");
        assertThat(result.getVersion()).isEqualTo("2.0.0");
    }

    @Test
    void uploadSpecification_withInvalidSpec_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> openAPIManager.uploadSpecification(
                "invalid-api",
                INVALID_SPEC,
                "test-user"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OpenAPI specification");
    }

    @Test
    void uploadSpecification_withEmptyContent_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> openAPIManager.uploadSpecification(
                "empty-api",
                "",
                "test-user"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    void uploadSpecification_withDuplicateIdentifier_shouldThrowException() {
        // Given
        openAPIManager.uploadSpecification("duplicate-api", VALID_JSON_SPEC, "test-user");

        // When/Then
        assertThatThrownBy(() -> openAPIManager.uploadSpecification(
                "duplicate-api",
                VALID_JSON_SPEC,
                "test-user"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getSpecification_withExistingId_shouldReturnSpecification() {
        // Given
        OpenAPISpecification uploaded = openAPIManager.uploadSpecification(
                "test-api",
                VALID_JSON_SPEC,
                "test-user"
        );

        // When
        Optional<OpenAPISpecification> result = openAPIManager.getSpecification(uploaded.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getApiIdentifier()).isEqualTo("test-api");
    }

    @Test
    void getSpecification_withNonExistingId_shouldReturnEmpty() {
        // When
        Optional<OpenAPISpecification> result = openAPIManager.getSpecification(UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getSpecificationByApiIdentifier_withExistingIdentifier_shouldReturnSpecification() {
        // Given
        openAPIManager.uploadSpecification("test-api", VALID_JSON_SPEC, "test-user");

        // When
        Optional<OpenAPISpecification> result = openAPIManager.getSpecificationByApiIdentifier("test-api");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getApiIdentifier()).isEqualTo("test-api");
    }

    @Test
    void getSpecificationByApiIdentifier_withNonExistingIdentifier_shouldReturnEmpty() {
        // When
        Optional<OpenAPISpecification> result = openAPIManager.getSpecificationByApiIdentifier("non-existing");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAllSpecifications_withMultipleSpecs_shouldReturnAll() {
        // Given
        openAPIManager.uploadSpecification("api-1", VALID_JSON_SPEC, "user-1");
        openAPIManager.uploadSpecification("api-2", VALID_YAML_SPEC, "user-2");

        // When
        List<OpenAPISpecification> result = openAPIManager.getAllSpecifications();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(OpenAPISpecification::getApiIdentifier)
                .containsExactlyInAnyOrder("api-1", "api-2");
    }

    @Test
    void getAllSpecifications_withNoSpecs_shouldReturnEmptyList() {
        // When
        List<OpenAPISpecification> result = openAPIManager.getAllSpecifications();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateSpecification_withValidSpec_shouldSucceed() {
        // Given
        OpenAPISpecification uploaded = openAPIManager.uploadSpecification(
                "test-api",
                VALID_JSON_SPEC,
                "test-user"
        );

        // When
        OpenAPISpecification updated = openAPIManager.updateSpecification(
                uploaded.getId(),
                VALID_YAML_SPEC,
                "updated-user"
        );

        // Then
        assertThat(updated.getId()).isEqualTo(uploaded.getId());
        assertThat(updated.getApiIdentifier()).isEqualTo("test-api");
        assertThat(updated.getSpecContent()).isEqualTo(VALID_YAML_SPEC);
        assertThat(updated.getVersion()).isEqualTo("2.0.0");
        assertThat(updated.getUploadedBy()).isEqualTo("updated-user");
        assertThat(updated.getUpdatedAt()).isAfter(uploaded.getUpdatedAt());
    }

    @Test
    void updateSpecification_withInvalidSpec_shouldThrowException() {
        // Given
        OpenAPISpecification uploaded = openAPIManager.uploadSpecification(
                "test-api",
                VALID_JSON_SPEC,
                "test-user"
        );

        // When/Then
        assertThatThrownBy(() -> openAPIManager.updateSpecification(
                uploaded.getId(),
                INVALID_SPEC,
                "test-user"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OpenAPI specification");
    }

    @Test
    void updateSpecification_withNonExistingId_shouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> openAPIManager.updateSpecification(
                UUID.randomUUID(),
                VALID_JSON_SPEC,
                "test-user"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
