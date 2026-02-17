package com.fnb.apierrorlogger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "openapi_specifications", indexes = {
    @Index(name = "idx_api_identifier", columnList = "api_identifier", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAPISpecification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "api_identifier", nullable = false, unique = true)
    private String apiIdentifier;
    
    @Column(name = "spec_content", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String specContent;
    
    @Column(nullable = false)
    private String version;
    
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    @Column(name = "uploaded_by")
    private String uploadedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
