package com.fnb.apierrorlogger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading or updating OpenAPI specifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAPISpecificationRequest {
    
    @NotBlank(message = "API identifier is required")
    private String apiIdentifier;
    
    @NotBlank(message = "Specification content is required")
    private String specContent;
    
    private String uploadedBy;
}
