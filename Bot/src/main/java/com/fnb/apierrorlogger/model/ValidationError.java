package com.fnb.apierrorlogger.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error with details about what failed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationError {
    
    private String field;
    private String message;
    private String expectedType;
    private Object actualValue;
}
