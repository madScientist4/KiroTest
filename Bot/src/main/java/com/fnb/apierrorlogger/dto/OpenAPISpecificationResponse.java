package com.fnb.apierrorlogger.dto;

import com.fnb.apierrorlogger.model.OpenAPISpecification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for OpenAPI specifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAPISpecificationResponse {
    
    private UUID id;
    private String apiIdentifier;
    private String specContent;
    private String version;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
    private LocalDateTime updatedAt;
    
    /**
     * Convert entity to response DTO.
     */
    public static OpenAPISpecificationResponse fromEntity(OpenAPISpecification entity) {
        return OpenAPISpecificationResponse.builder()
                .id(entity.getId())
                .apiIdentifier(entity.getApiIdentifier())
                .specContent(entity.getSpecContent())
                .version(entity.getVersion())
                .uploadedAt(entity.getUploadedAt())
                .uploadedBy(entity.getUploadedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
