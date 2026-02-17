package com.fnb.apierrorlogger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "error_request_id", nullable = false)
    private UUID errorRequestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_request_id", insertable = false, updatable = false)
    private ErrorRequest errorRequest;
    
    @Column(nullable = false)
    private String recipient;
    
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
    
    @Column(name = "delivery_status", nullable = false)
    private String deliveryStatus;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
