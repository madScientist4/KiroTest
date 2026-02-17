package com.fnb.apierrorlogger.repository;

import com.fnb.apierrorlogger.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    
    // Find all email logs for a specific error request
    List<EmailLog> findByErrorRequestId(UUID errorRequestId);
    
    // Find by delivery status
    List<EmailLog> findByDeliveryStatus(String deliveryStatus);
    
    // Find failed email deliveries for a specific error request
    List<EmailLog> findByErrorRequestIdAndDeliveryStatus(UUID errorRequestId, String deliveryStatus);
}
