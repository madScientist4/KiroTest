package com.fnb.apierrorlogger.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating an error request against an OpenAPI specification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {
    
    private boolean isValid;
    
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * Create a successful validation result.
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .isValid(true)
                .build();
    }
    
    /**
     * Create a failed validation result with errors.
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return ValidationResult.builder()
                .isValid(false)
                .errors(errors)
                .build();
    }
    
    /**
     * Add a validation error.
     */
    public void addError(ValidationError error) {
        this.errors.add(error);
        this.isValid = false;
    }
    
    /**
     * Add a warning.
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
