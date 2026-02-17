package com.fnb.apierrorlogger.service;

import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.repository.OpenAPISpecificationRepository;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing OpenAPI specifications.
 * Handles upload, validation, retrieval, and updates of OpenAPI specification files.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.5, 5.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAPIManager {

    private final OpenAPISpecificationRepository repository;
    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();

    /**
     * Upload and validate an OpenAPI specification.
     * Parses JSON/YAML format and validates OpenAPI structure.
     * 
     * @param apiIdentifier The unique identifier for the API
     * @param specContent The specification content (JSON or YAML)
     * @param uploadedBy The user uploading the specification
     * @return The saved OpenAPISpecification
     * @throws IllegalArgumentException if the specification is invalid
     * 
     * Requirements: 5.1, 5.2, 5.3
     */
    @Transactional
    public OpenAPISpecification uploadSpecification(String apiIdentifier, String specContent, String uploadedBy) {
        log.info("Uploading OpenAPI specification for API: {}", apiIdentifier);
        
        // Validate the specification format
        validateOpenAPIFormat(specContent);
        
        // Parse to extract version
        String version = extractVersion(specContent);
        
        // Check if specification already exists
        if (repository.existsByApiIdentifier(apiIdentifier)) {
            throw new IllegalArgumentException("OpenAPI specification with identifier '" + apiIdentifier + "' already exists. Use update instead.");
        }
        
        // Create and save the specification
        OpenAPISpecification specification = OpenAPISpecification.builder()
                .apiIdentifier(apiIdentifier)
                .specContent(specContent)
                .version(version)
                .uploadedBy(uploadedBy)
                .build();
        
        OpenAPISpecification saved = repository.save(specification);
        log.info("Successfully uploaded OpenAPI specification with ID: {}", saved.getId());
        
        return saved;
    }

    /**
     * Get a specific OpenAPI specification by ID.
     * 
     * @param id The specification ID
     * @return Optional containing the specification if found
     * 
     * Requirement: 5.6
     */
    public Optional<OpenAPISpecification> getSpecification(UUID id) {
        log.debug("Retrieving OpenAPI specification with ID: {}", id);
        return repository.findById(id);
    }

    /**
     * Get a specific OpenAPI specification by API identifier.
     * 
     * @param apiIdentifier The API identifier
     * @return Optional containing the specification if found
     * 
     * Requirement: 5.3
     */
    public Optional<OpenAPISpecification> getSpecificationByApiIdentifier(String apiIdentifier) {
        log.debug("Retrieving OpenAPI specification for API: {}", apiIdentifier);
        return repository.findByApiIdentifier(apiIdentifier);
    }

    /**
     * Get all OpenAPI specifications.
     * 
     * @return List of all specifications
     * 
     * Requirement: 5.6
     */
    public List<OpenAPISpecification> getAllSpecifications() {
        log.debug("Retrieving all OpenAPI specifications");
        return repository.findAll();
    }

    /**
     * Update an existing OpenAPI specification.
     * 
     * @param id The specification ID to update
     * @param specContent The new specification content
     * @param updatedBy The user updating the specification
     * @return The updated specification
     * @throws IllegalArgumentException if the specification is invalid or not found
     * 
     * Requirement: 5.5
     */
    @Transactional
    public OpenAPISpecification updateSpecification(UUID id, String specContent, String updatedBy) {
        log.info("Updating OpenAPI specification with ID: {}", id);
        
        // Validate the new specification format
        validateOpenAPIFormat(specContent);
        
        // Find existing specification
        OpenAPISpecification existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("OpenAPI specification with ID '" + id + "' not found"));
        
        // Extract version from new content
        String version = extractVersion(specContent);
        
        // Update fields
        existing.setSpecContent(specContent);
        existing.setVersion(version);
        existing.setUploadedBy(updatedBy);
        
        OpenAPISpecification updated = repository.save(existing);
        log.info("Successfully updated OpenAPI specification with ID: {}", id);
        
        return updated;
    }

    /**
     * Validate OpenAPI specification format using swagger-parser.
     * Handles both JSON and YAML formats.
     * 
     * @param specContent The specification content to validate
     * @throws IllegalArgumentException if the specification is invalid
     * 
     * Requirements: 5.1, 5.2, 5.4
     */
    private void validateOpenAPIFormat(String specContent) {
        if (specContent == null || specContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Specification content cannot be empty");
        }
        
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        
        SwaggerParseResult result = parser.readContents(specContent, null, options);
        
        // Check for parsing errors
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            String errorMessage = String.join("; ", result.getMessages());
            log.error("OpenAPI validation failed: {}", errorMessage);
            throw new IllegalArgumentException("Invalid OpenAPI specification: " + errorMessage);
        }
        
        // Check if OpenAPI object was successfully parsed
        if (result.getOpenAPI() == null) {
            throw new IllegalArgumentException("Invalid OpenAPI specification: Unable to parse specification");
        }
        
        log.debug("OpenAPI specification validation successful");
    }

    /**
     * Extract version from OpenAPI specification.
     * 
     * @param specContent The specification content
     * @return The version string, or "unknown" if not found
     */
    private String extractVersion(String specContent) {
        try {
            SwaggerParseResult result = parser.readContents(specContent, null, null);
            if (result.getOpenAPI() != null && result.getOpenAPI().getInfo() != null) {
                String version = result.getOpenAPI().getInfo().getVersion();
                return version != null ? version : "unknown";
            }
        } catch (Exception e) {
            log.warn("Failed to extract version from specification: {}", e.getMessage());
        }
        return "unknown";
    }
}
