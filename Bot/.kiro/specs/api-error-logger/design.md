# Design Document: API Error Logger

## Overview

The API Error Logger is a web-based application that provides a systematic approach to logging, validating, and tracking API errors. The system consists of a frontend web interface, a backend API service, a validation engine, an email notification service, and a database for persistence.

The core workflow is:
1. User submits an error request through the web interface
2. Backend validates the request against the corresponding OpenAPI specification
3. If validation passes, an email is sent to the investigation team
4. User receives immediate feedback on submission status
5. All error requests are stored for querying and historical analysis

The application emphasizes real-time feedback, data integrity, and seamless integration with existing API specifications.

## Architecture

The system follows a three-tier architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (Web UI)                        │
│  - React-based SPA with FNB branding                        │
│  - Real-time feedback and interactive forms                 │
│  - Environment status dashboard                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTPS/REST API
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Backend API Service                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Error      │  │  Validation  │  │    Email     │     │
│  │  Controller  │  │    Engine    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Environment  │  │   OpenAPI    │  │    Query     │     │
│  │  Controller  │  │   Manager    │  │   Service    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ Database Queries
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Database Layer                            │
│  - Error Requests                                           │
│  - OpenAPI Specifications                                   │
│  - Environments                                             │
│  - Email Delivery Logs                                      │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack Considerations

- **Frontend**: Modern JavaScript framework (React, Vue, or Angular) for interactive UI
- **Backend**: RESTful API service (language TBD during task creation)
- **Database**: Relational database (PostgreSQL, MySQL) for structured data
- **Validation**: OpenAPI validation library (language-specific)
- **Email**: SMTP client for email delivery

## Components and Interfaces

### Frontend Components

**ErrorSubmissionForm**
- Collects error request details from users
- Validates required fields before submission
- Displays real-time validation feedback
- Shows submission status and email delivery confirmation

**EnvironmentDashboard**
- Displays list of environments with status indicators
- Updates status in real-time (polling or WebSocket)
- Color-coded status badges (active/inactive)

**ErrorRequestList**
- Displays paginated list of error requests
- Provides filtering by date, endpoint, status, environment
- Shows summary information for each request

**ErrorRequestDetail**
- Displays complete information for a single error request
- Shows validation results and email delivery status
- Formatted display of request/response payloads

**OpenAPIManager** (Admin)
- Upload interface for OpenAPI specification files
- List view of uploaded specifications
- Update and delete operations

### Backend Components

**ErrorController**
- `POST /api/errors`: Create new error request
- `GET /api/errors`: List error requests with filtering
- `GET /api/errors/{id}`: Get error request details
- Validates input, coordinates validation and email services

**ValidationEngine**
- `validateRequest(errorRequest, openApiSpec)`: Validates request against specification
- Checks HTTP method validity
- Validates request payload against schema
- Validates required parameters
- Returns validation result with detailed messages

**EmailService**
- `sendErrorNotification(errorRequest, validationResult)`: Sends email to investigation team
- Formats email with error details
- Handles SMTP configuration
- Logs delivery status
- Returns success/failure status

**EnvironmentController**
- `GET /api/environments`: List all environments
- `PUT /api/environments/{id}`: Update environment status (admin)
- Returns environment name and status

**OpenAPIManager**
- `POST /api/openapi`: Upload OpenAPI specification
- `GET /api/openapi`: List all specifications
- `GET /api/openapi/{id}`: Get specific specification
- `PUT /api/openapi/{id}`: Update specification
- Parses and validates OpenAPI files (JSON/YAML)

**QueryService**
- Handles complex queries for error requests
- Implements filtering logic
- Manages pagination
- Optimizes database queries

### Interfaces

**ErrorRequest Model**
```
ErrorRequest {
  id: string (UUID)
  apiEndpoint: string
  httpMethod: string (GET, POST, PUT, DELETE, PATCH)
  requestPayload: JSON
  responseStatus: integer
  responseBody: string
  timestamp: datetime
  environment: string
  openApiSpecId: string
  validationStatus: string (passed, failed, unable_to_validate)
  validationDetails: JSON
  emailSent: boolean
  emailDeliveryStatus: string (sent, failed, not_sent)
  createdAt: datetime
  updatedAt: datetime
}
```

**OpenAPISpecification Model**
```
OpenAPISpecification {
  id: string (UUID)
  apiIdentifier: string
  specContent: JSON/YAML
  version: string
  uploadedAt: datetime
  uploadedBy: string
  updatedAt: datetime
}
```

**Environment Model**
```
Environment {
  id: string (UUID)
  name: string
  status: string (active, inactive)
  updatedAt: datetime
}
```

**ValidationResult**
```
ValidationResult {
  isValid: boolean
  errors: array of ValidationError
  warnings: array of string
}

ValidationError {
  field: string
  message: string
  expectedType: string
  actualValue: any
}
```

## Data Models

### Database Schema

**error_requests table**
- id (UUID, primary key)
- api_endpoint (VARCHAR, indexed)
- http_method (VARCHAR)
- request_payload (JSONB)
- response_status (INTEGER)
- response_body (TEXT)
- timestamp (TIMESTAMP, indexed)
- environment (VARCHAR, indexed)
- openapi_spec_id (UUID, foreign key)
- validation_status (VARCHAR, indexed)
- validation_details (JSONB)
- email_sent (BOOLEAN)
- email_delivery_status (VARCHAR)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**openapi_specifications table**
- id (UUID, primary key)
- api_identifier (VARCHAR, unique, indexed)
- spec_content (JSONB)
- version (VARCHAR)
- uploaded_at (TIMESTAMP)
- uploaded_by (VARCHAR)
- updated_at (TIMESTAMP)

**environments table**
- id (UUID, primary key)
- name (VARCHAR, unique, indexed)
- status (VARCHAR)
- updated_at (TIMESTAMP)

**email_logs table**
- id (UUID, primary key)
- error_request_id (UUID, foreign key)
- recipient (VARCHAR)
- sent_at (TIMESTAMP)
- delivery_status (VARCHAR)
- error_message (TEXT, nullable)

### Data Relationships

- ErrorRequest → OpenAPISpecification (many-to-one)
- ErrorRequest → EmailLog (one-to-many)
- ErrorRequest → Environment (many-to-one, via environment name)

### Data Validation Rules

- api_endpoint must be a valid URL path
- http_method must be one of: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- response_status must be a valid HTTP status code (100-599)
- timestamp must not be in the future
- validation_status must be one of: passed, failed, unable_to_validate
- email_delivery_status must be one of: sent, failed, not_sent

## Correctness Properties


A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Core Data Properties

**Property 1: Error request persistence round-trip**
*For any* valid error request with all required fields, creating the request should result in a persisted record that can be immediately retrieved from the database with the same data.
**Validates: Requirements 1.1, 1.5**

**Property 2: Unique identifier assignment**
*For any* set of error requests created, all assigned identifiers should be unique across the entire set.
**Validates: Requirements 1.3**

**Property 3: OpenAPI specification association**
*For any* error request created with a valid API identifier, the request should be correctly associated with the corresponding OpenAPI specification.
**Validates: Requirements 1.2**

**Property 4: Invalid request rejection**
*For any* error request missing one or more required fields (endpoint, method, payload, status, body, timestamp), the submission should be rejected with a descriptive error message indicating which fields are missing.
**Validates: Requirements 1.4**

### Validation Properties

**Property 5: Comprehensive request validation**
*For any* error request and its corresponding OpenAPI specification, the validation engine should verify: (1) the request payload matches the schema, (2) the HTTP method is valid for the endpoint, and (3) all required parameters are present.
**Validates: Requirements 2.2, 2.3, 2.4**

**Property 6: Validation status update**
*For any* error request that undergoes validation, the request record should be updated with the validation status (passed/failed) and detailed validation results.
**Validates: Requirements 2.5**

**Property 7: Validation with missing specification**
*For any* error request referencing a non-existent OpenAPI specification, the validation engine should mark the request as "unable_to_validate" and log the missing specification.
**Validates: Requirements 2.6** (edge case)

### Email Notification Properties

**Property 8: Conditional email sending**
*For any* error request, an email notification should be sent to the investigation team if and only if the request passes validation.
**Validates: Requirements 3.1, 3.3**

**Property 9: Email content completeness**
*For any* error request that triggers an email notification, the email should contain all required details: endpoint, HTTP method, request payload, response status, response body, timestamp, and validation results.
**Validates: Requirements 3.2**

**Property 10: Email failure handling**
*For any* error request where email delivery fails, the system should log the failure and update the request's email delivery status to "failed".
**Validates: Requirements 3.4**

**Property 11: SMTP configuration usage**
*For any* configured SMTP settings, the email service should use those settings when sending notifications.
**Validates: Requirements 3.5**

### Environment Management Properties

**Property 12: Complete environment display**
*For any* set of configured environments, the system should display all environments with both name and status (active/inactive).
**Validates: Requirements 4.1, 4.2**

**Property 13: Environment status update**
*For any* environment, when an administrator updates its status, the change should be persisted and reflected in subsequent queries.
**Validates: Requirements 4.4**

**Property 14: Environment list sorting**
*For any* list of environments displayed, the environments should be sorted alphabetically by name.
**Validates: Requirements 4.5**

### OpenAPI Specification Management Properties

**Property 15: Multi-format specification upload**
*For any* valid OpenAPI specification in JSON or YAML format, the system should successfully parse and store the specification.
**Validates: Requirements 5.1, 5.2**

**Property 16: Specification identifier association**
*For any* uploaded OpenAPI specification, the system should associate it with the API identifier extracted from or provided with the specification.
**Validates: Requirements 5.3**

**Property 17: Invalid specification rejection**
*For any* invalid OpenAPI specification file (malformed JSON/YAML or invalid OpenAPI structure), the upload should be rejected with a descriptive error message.
**Validates: Requirements 5.4**

**Property 18: Specification update persistence**
*For any* existing OpenAPI specification, updating it with new content should result in the updated content being persisted and retrievable.
**Validates: Requirements 5.5**

**Property 19: Specification list completeness**
*For any* set of uploaded OpenAPI specifications, querying for all specifications should return the complete set.
**Validates: Requirements 5.6**

### Query and Display Properties

**Property 20: Error request retrieval**
*For any* error request created in the system, it should be retrievable by its unique identifier with all stored information intact.
**Validates: Requirements 6.4**

**Property 21: Error request list display completeness**
*For any* error request in the displayed list, the system should show all key information: timestamp, endpoint, HTTP method, validation status, and email status.
**Validates: Requirements 6.2**

**Property 22: Filter correctness**
*For any* combination of filter criteria (date range, endpoint, validation status, environment), the returned error requests should match all specified criteria.
**Validates: Requirements 6.3**

**Property 23: Detail view completeness**
*For any* error request detail view, all logged information should be displayed including request payload, response body, and validation results.
**Validates: Requirements 6.5**

### Concurrency Properties

**Property 24: Concurrent request independence**
*For any* set of error requests submitted concurrently, each request should be processed independently and result in a separate, correctly stored record.
**Validates: Requirements 7.1, 7.2**

### User Feedback Properties

**Property 25: Validation success notification**
*For any* error request that passes validation and successfully sends an email, the system should display a confirmation message indicating the investigation team has been notified.
**Validates: Requirements 8.1, 8.2**

**Property 26: Validation failure notification**
*For any* error request that fails validation, the system should display a message indicating no email was sent and include the specific validation failure reasons.
**Validates: Requirements 8.3**

**Property 27: Email delivery failure notification**
*For any* error request where validation passes but email delivery fails, the system should display a warning message indicating the delivery failure.
**Validates: Requirements 8.4**

**Property 28: Interactive form feedback**
*For any* user interaction with forms or buttons, the system should provide immediate visual feedback (state changes, button states, etc.).
**Validates: Requirements 9.3**

**Property 29: Loading indicator display**
*For any* asynchronous operation (API request, validation, email sending), the system should display a loading indicator while the operation is in progress.
**Validates: Requirements 9.4**

## Error Handling

The system implements comprehensive error handling at multiple levels:

### Input Validation Errors
- Missing required fields → 400 Bad Request with field-specific error messages
- Invalid data types → 400 Bad Request with type mismatch details
- Invalid HTTP methods → 400 Bad Request with list of valid methods
- Invalid URLs → 400 Bad Request with URL format requirements

### OpenAPI Specification Errors
- Specification not found → Mark request as "unable_to_validate", log warning
- Invalid specification format → 400 Bad Request during upload with parsing errors
- Specification parsing errors → 500 Internal Server Error with error details

### Validation Errors
- Schema validation failures → Store detailed validation errors in request record
- Method validation failures → Store method mismatch details
- Parameter validation failures → Store missing parameter list

### Email Delivery Errors
- SMTP connection failures → Log error, mark email status as "failed", notify user
- Invalid recipient configuration → Log error, mark email status as "failed"
- Email timeout → Retry once, then mark as "failed" if still unsuccessful

### Database Errors
- Connection failures → 503 Service Unavailable, retry with exponential backoff
- Constraint violations → 409 Conflict with constraint details
- Query timeouts → 504 Gateway Timeout, log slow query for optimization

### Concurrent Access Errors
- Optimistic locking conflicts → Retry operation with updated data
- Deadlocks → Automatic retry with exponential backoff (max 3 attempts)

### General Error Response Format
All API errors return consistent JSON structure:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "specific field information",
      "expected": "expected value or format",
      "actual": "actual value received"
    },
    "timestamp": "ISO 8601 timestamp"
  }
}
```

## Testing Strategy

The API Error Logger will employ a dual testing approach combining unit tests and property-based tests to ensure comprehensive coverage and correctness.

### Property-Based Testing

Property-based testing validates universal properties across many generated inputs. We will use a property-based testing library appropriate for the chosen implementation language (e.g., Hypothesis for Python, fast-check for TypeScript/JavaScript, QuickCheck for Haskell).

**Configuration:**
- Each property test will run a minimum of 100 iterations
- Each test will be tagged with a comment referencing the design property
- Tag format: `Feature: api-error-logger, Property N: [property description]`
- Each correctness property from this design document will be implemented as a single property-based test

**Property Test Coverage:**
- Data persistence and retrieval (Properties 1, 2, 3, 20)
- Input validation and rejection (Properties 4, 17)
- OpenAPI validation logic (Properties 5, 6, 7)
- Email notification conditions (Properties 8, 9, 10, 11)
- Environment management (Properties 12, 13, 14)
- Specification management (Properties 15, 16, 18, 19)
- Query filtering (Property 22)
- Concurrent operations (Property 24)
- User feedback (Properties 25, 26, 27, 28, 29)

**Example Property Test Structure:**
```
test_property_1_error_request_persistence_round_trip:
  # Feature: api-error-logger, Property 1: Error request persistence round-trip
  for 100 iterations:
    generate random valid error request
    create request in system
    retrieve request from database
    assert retrieved data matches created data
```

### Unit Testing

Unit tests will focus on specific examples, edge cases, and integration points that complement property-based tests.

**Unit Test Coverage:**
- Specific HTTP status code handling (200, 400, 404, 500, etc.)
- Boundary conditions (empty payloads, maximum field lengths)
- Specific OpenAPI schema validation scenarios
- Email template formatting with specific data
- Date range filtering edge cases (same day, year boundaries)
- Environment status transitions (active → inactive → active)
- SMTP configuration variations
- Error message formatting and localization
- UI component rendering with specific data
- Loading state transitions

**Integration Tests:**
- End-to-end error submission flow
- OpenAPI specification upload and validation workflow
- Email delivery with mock SMTP server
- Database transaction rollback scenarios
- API endpoint integration with frontend

**Test Organization:**
- Unit tests organized by component (ErrorController, ValidationEngine, EmailService, etc.)
- Property tests organized by requirement domain (data, validation, email, etc.)
- Integration tests in separate test suite
- All tests automated in CI/CD pipeline

**Testing Tools:**
- Property-based testing library (language-specific)
- Unit testing framework (language-specific)
- Mock SMTP server for email testing
- Test database with fixtures
- API testing tools (Postman, REST Client, or similar)
- Frontend testing library (Jest, Vitest, or similar for UI components)

### Test Data Management

- Use factories/builders for generating test data
- Maintain fixtures for common OpenAPI specifications
- Use database transactions for test isolation
- Clean up test data after each test run
- Separate test database from development database

### Coverage Goals

- Minimum 80% code coverage for backend services
- 100% coverage of correctness properties via property tests
- All error handling paths covered by unit tests
- All API endpoints covered by integration tests
