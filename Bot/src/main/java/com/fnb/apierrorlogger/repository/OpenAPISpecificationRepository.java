package com.fnb.apierrorlogger.repository;

import com.fnb.apierrorlogger.model.OpenAPISpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpenAPISpecificationRepository extends JpaRepository<OpenAPISpecification, UUID> {
    
    // Find by API identifier (Requirement 5.3)
    Optional<OpenAPISpecification> findByApiIdentifier(String apiIdentifier);
    
    // Check if API identifier exists
    boolean existsByApiIdentifier(String apiIdentifier);
}
