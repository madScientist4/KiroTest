package com.fnb.apierrorlogger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "error_requests", indexes = {
    @Index(name = "idx_api_endpoint", columnList = "api_endpoint"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_environment", columnList = "environment"),
    @Index(name = "idx_validation_status", columnList = "validation_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;
    
    @Column(name = "http_method", nullable = false)
    private String httpMethod;
    
    @Column(name = "request_payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestPayload;
    
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private String environment;
    
    @Column(name = "openapi_spec_id")
    private UUID openApiSpecId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "openapi_spec_id", insertable = false, updatable = false)
    private OpenAPISpecification openApiSpecification;
    
    @Column(name = "validation_status")
    private String validationStatus;
    
    @Column(name = "validation_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String validationDetails;
    
    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private Boolean emailSent = false;
    
    @Column(name = "email_delivery_status")
    private String emailDeliveryStatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
