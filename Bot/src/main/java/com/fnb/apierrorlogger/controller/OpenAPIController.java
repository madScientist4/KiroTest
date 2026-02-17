package com.fnb.apierrorlogger.controller;

import com.fnb.apierrorlogger.dto.OpenAPISpecificationRequest;
import com.fnb.apierrorlogger.dto.OpenAPISpecificationResponse;
import com.fnb.apierrorlogger.dto.OpenAPISpecificationUpdateRequest;
import com.fnb.apierrorlogger.model.OpenAPISpecification;
import com.fnb.apierrorlogger.service.OpenAPIManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing OpenAPI specifications.
 * Provides endpoints for uploading, retrieving, and updating specifications.
 * 
 * Requirements: 5.1, 5.5, 5.6
 */
@RestController
@RequestMapping("/api/openapi")
@RequiredArgsConstructor
@Slf4j
public class OpenAPIController {

    private final OpenAPIManager openAPIManager;

    /**
     * Upload a new OpenAPI specification.
     * POST /api/openapi
     * 
     * @param request The specification upload request
     * @return The created specification
     * 
     * Requirement: 5.1
     */
    @PostMapping
    public ResponseEntity<OpenAPISpecificationResponse> uploadSpecification(
            @Valid @RequestBody OpenAPISpecificationRequest request) {
        
        log.info("Received request to upload OpenAPI specification for API: {}", request.getApiIdentifier());
        
        try {
            OpenAPISpecification specification = openAPIManager.uploadSpecification(
                    request.getApiIdentifier(),
                    request.getSpecContent(),
                    request.getUploadedBy()
            );
            
            OpenAPISpecificationResponse response = OpenAPISpecificationResponse.fromEntity(specification);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Failed to upload specification: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get all OpenAPI specifications.
     * GET /api/openapi
     * 
     * @return List of all specifications
     * 
     * Requirement: 5.6
     */
    @GetMapping
    public ResponseEntity<List<OpenAPISpecificationResponse>> getAllSpecifications() {
        log.info("Received request to get all OpenAPI specifications");
        
        List<OpenAPISpecification> specifications = openAPIManager.getAllSpecifications();
        
        List<OpenAPISpecificationResponse> responses = specifications.stream()
                .map(OpenAPISpecificationResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific OpenAPI specification by ID.
     * GET /api/openapi/{id}
     * 
     * @param id The specification ID
     * @return The specification if found
     * 
     * Requirement: 5.6
     */
    @GetMapping("/{id}")
    public ResponseEntity<OpenAPISpecificationResponse> getSpecification(@PathVariable UUID id) {
        log.info("Received request to get OpenAPI specification with ID: {}", id);
        
        return openAPIManager.getSpecification(id)
                .map(OpenAPISpecificationResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an existing OpenAPI specification.
     * PUT /api/openapi/{id}
     * 
     * @param id The specification ID to update
     * @param request The update request
     * @return The updated specification
     * 
     * Requirement: 5.5
     */
    @PutMapping("/{id}")
    public ResponseEntity<OpenAPISpecificationResponse> updateSpecification(
            @PathVariable UUID id,
            @Valid @RequestBody OpenAPISpecificationUpdateRequest request) {
        
        log.info("Received request to update OpenAPI specification with ID: {}", id);
        
        try {
            OpenAPISpecification specification = openAPIManager.updateSpecification(
                    id,
                    request.getSpecContent(),
                    request.getUpdatedBy()
            );
            
            OpenAPISpecificationResponse response = OpenAPISpecificationResponse.fromEntity(specification);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Failed to update specification: {}", e.getMessage());
            
            // Check if it's a not found error
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            
            throw e;
        }
    }
}
