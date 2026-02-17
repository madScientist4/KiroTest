package com.fnb.apierrorlogger.repository;

import com.fnb.apierrorlogger.model.ErrorRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ErrorRequestRepository extends JpaRepository<ErrorRequest, UUID> {
    
    // Filter by endpoint
    List<ErrorRequest> findByApiEndpoint(String apiEndpoint);
    
    // Filter by validation status
    List<ErrorRequest> findByValidationStatus(String validationStatus);
    
    // Filter by environment
    List<ErrorRequest> findByEnvironment(String environment);
    
    // Filter by date range
    List<ErrorRequest> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Combined filter: endpoint and validation status
    List<ErrorRequest> findByApiEndpointAndValidationStatus(String apiEndpoint, String validationStatus);
    
    // Combined filter: environment and validation status
    List<ErrorRequest> findByEnvironmentAndValidationStatus(String environment, String validationStatus);
    
    // Combined filter: date range and endpoint
    List<ErrorRequest> findByTimestampBetweenAndApiEndpoint(LocalDateTime startDate, LocalDateTime endDate, String apiEndpoint);
    
    // Combined filter: date range and validation status
    List<ErrorRequest> findByTimestampBetweenAndValidationStatus(LocalDateTime startDate, LocalDateTime endDate, String validationStatus);
    
    // Combined filter: date range and environment
    List<ErrorRequest> findByTimestampBetweenAndEnvironment(LocalDateTime startDate, LocalDateTime endDate, String environment);
    
    // Complex filter: all parameters
    @Query("SELECT e FROM ErrorRequest e WHERE " +
           "(:startDate IS NULL OR e.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR e.timestamp <= :endDate) AND " +
           "(:apiEndpoint IS NULL OR e.apiEndpoint = :apiEndpoint) AND " +
           "(:validationStatus IS NULL OR e.validationStatus = :validationStatus) AND " +
           "(:environment IS NULL OR e.environment = :environment)")
    List<ErrorRequest> findByFilters(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("apiEndpoint") String apiEndpoint,
        @Param("validationStatus") String validationStatus,
        @Param("environment") String environment
    );
    
    // Find by OpenAPI specification ID
    List<ErrorRequest> findByOpenApiSpecId(UUID openApiSpecId);
}
