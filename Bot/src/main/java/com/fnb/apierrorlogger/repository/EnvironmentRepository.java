package com.fnb.apierrorlogger.repository;

import com.fnb.apierrorlogger.model.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {
    
    // Find by name (Requirement 4.1)
    Optional<Environment> findByName(String name);
    
    // Find all environments sorted by name (Requirement 4.5)
    List<Environment> findAllByOrderByNameAsc();
    
    // Check if environment name exists
    boolean existsByName(String name);
}
