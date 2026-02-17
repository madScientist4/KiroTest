package com.fnb.apierrorlogger.controller;

import com.fnb.apierrorlogger.dto.ErrorRequestCreateRequest;
import com.fnb.apierrorlogger.dto.ErrorRequestListResponse;
import com.fnb.apierrorlogger.dto.ErrorRequestResponse;
import com.fnb.apierrorlogger.service.ErrorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@Slf4j
public class ErrorController {
    
    private final ErrorService errorService;
    
    /**
     * Create a new error request
     * POST /api/errors
     */
    @PostMapping
    public ResponseEntity<ErrorRequestResponse> createErrorRequest(
            @Valid @RequestBody ErrorRequestCreateRequest request) {
        log.info("Received error request for endpoint: {}", request.getApiEndpoint());
        
        ErrorRequestResponse response = errorService.createErrorRequest(request);
        
        log.info("Created error request with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * List error requests with optional filtering
     * GET /api/errors
     */
    @GetMapping
    public ResponseEntity<List<ErrorRequestListResponse>> listErrorRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String validationStatus,
            @RequestParam(required = false) String environment) {
        
        log.info("Listing error requests with filters - startDate: {}, endDate: {}, endpoint: {}, validationStatus: {}, environment: {}",
                startDate, endDate, endpoint, validationStatus, environment);
        
        List<ErrorRequestListResponse> responses = errorService.listErrorRequests(
                startDate, endDate, endpoint, validationStatus, environment);
        
        log.info("Found {} error requests", responses.size());
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get error request details by ID
     * GET /api/errors/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ErrorRequestResponse> getErrorRequest(@PathVariable UUID id) {
        log.info("Fetching error request with ID: {}", id);
        
        ErrorRequestResponse response = errorService.getErrorRequest(id);
        
        log.info("Retrieved error request: {}", id);
        return ResponseEntity.ok(response);
    }
}
