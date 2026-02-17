# Implementation Plan: API Error Logger

## Overview

This implementation plan breaks down the API Error Logger into discrete coding tasks using Java for the backend and a modern JavaScript framework (React) for the frontend. The plan follows an incremental approach where each task builds on previous work, with testing integrated throughout to catch errors early.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Maven/Gradle project with Spring Boot
  - Add dependencies: Spring Web, Spring Data JPA, PostgreSQL driver, JavaMail, OpenAPI validation library (swagger-parser), Lombok
  - Configure application.properties for database and SMTP
  - Set up frontend React project with TypeScript
  - Add frontend dependencies: React Router, Axios, TailwindCSS (for FNB styling)
  - _Requirements: All_

- [ ] 2. Implement database models and repositories
  - [x] 2.1 Create JPA entity classes
    - Create ErrorRequest entity with all fields (id, apiEndpoint, httpMethod, requestPayload, responseStatus, responseBody, timestamp, environment, openApiSpecId, validationStatus, validationDetails, emailSent, emailDeliveryStatus, createdAt, updatedAt)
    - Create OpenAPISpecification entity (id, apiIdentifier, specContent, version, uploadedAt, uploadedBy, updatedAt)
    - Create Environment entity (id, name, status, updatedAt)
    - Create EmailLog entity (id, errorRequestId, recipient, sentAt, deliveryStatus, errorMessage)
    - Add appropriate JPA annotations, indexes, and relationships
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 5.1, 5.3, 4.1_

  - [x] 2.2 Write property test for error request persistence
    - **Property 1: Error request persistence round-trip**
    - **Validates: Requirements 1.1, 1.5**

  - [x] 2.3 Write property test for unique identifier assignment
    - **Property 2: Unique identifier assignment**
    - **Validates: Requirements 1.3**

  - [x] 2.4 Create Spring Data JPA repositories
    - Create ErrorRequestRepository with custom query methods for filtering
    - Create OpenAPISpecificationRepository with findByApiIdentifier method
    - Create EnvironmentRepository with findByName method
    - Create EmailLogRepository
    - _Requirements: 6.1, 6.3, 5.6, 4.1_

- [ ] 3. Implement OpenAPI specification management
  - [x] 3.1 Create OpenAPIManager service
    - Implement uploadSpecification method (parse JSON/YAML, validate OpenAPI format)
    - Implement getSpecification, getAllSpecifications, updateSpecification methods
    - Add validation for OpenAPI format using swagger-parser library
    - Handle both JSON and YAML formats
    - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

  - [x] 3.2 Write property test for multi-format specification upload
    - **Property 15: Multi-format specification upload**
    - **Validates: Requirements 5.1, 5.2**

  - [x] 3.3 Write property test for invalid specification rejection
    - **Property 17: Invalid specification rejection**
    - **Validates: Requirements 5.4**

  - [x] 3.4 Write property test for specification update persistence
    - **Property 18: Specification update persistence**
    - **Validates: Requirements 5.5**

  - [x] 3.5 Create OpenAPIController REST endpoints
    - POST /api/openapi - upload specification
    - GET /api/openapi - list all specifications
    - GET /api/openapi/{id} - get specific specification
    - PUT /api/openapi/{id} - update specification
    - Add request/response DTOs and validation
    - _Requirements: 5.1, 5.5, 5.6_

- [ ] 4. Implement validation engine
  - [x] 4.1 Create ValidationEngine service
    - Implement validateRequest method that takes ErrorRequest and OpenAPISpecification
    - Validate request payload against OpenAPI schema
    - Validate HTTP method is allowed for endpoint
    - Validate required parameters are present
    - Return ValidationResult with detailed errors
    - Handle missing OpenAPI specification case
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.6_

  - [x] 4.2 Write property test for comprehensive request validation
    - **Property 5: Comprehensive request validation**
    - **Validates: Requirements 2.2, 2.3, 2.4**

  - [x] 4.3 Write property test for validation status update
    - **Property 6: Validation status update**
    - **Validates: Requirements 2.5**

  - [x] 4.4 Write unit test for validation with missing specification
    - Test edge case where OpenAPI spec is not found
    - **Property 7: Validation with missing specification**
    - **Validates: Requirements 2.6**

- [x] 5. Checkpoint - Ensure validation tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Implement email notification service
  - [x] 6.1 Create EmailService
    - Implement sendErrorNotification method using JavaMail
    - Format email with error details (endpoint, method, payload, status, body, timestamp, validation results)
    - Use configured SMTP settings from application.properties
    - Log email delivery status to EmailLog table
    - Handle email delivery failures gracefully
    - _Requirements: 3.1, 3.2, 3.4, 3.5_

  - [x] 6.2 Write property test for conditional email sending
    - **Property 8: Conditional email sending**
    - **Validates: Requirements 3.1, 3.3**

  - [x] 6.3 Write property test for email content completeness
    - **Property 9: Email content completeness**
    - **Validates: Requirements 3.2**

  - [x] 6.4 Write property test for email failure handling
    - **Property 10: Email failure handling**
    - **Validates: Requirements 3.4**

- [ ] 7. Implement error request submission and processing
  - [ ] 7.1 Create ErrorController REST endpoints
    - POST /api/errors - create new error request
    - GET /api/errors - list error requests with filtering (date range, endpoint, validation status, environment)
    - GET /api/errors/{id} - get error request details
    - Add request/response DTOs with validation annotations
    - _Requirements: 1.1, 1.4, 6.1, 6.3, 6.4_

  - [x] 7.2 Create ErrorService to orchestrate workflow
    - Implement createErrorRequest method
    - Validate required fields are present
    - Associate request with OpenAPI specification
    - Trigger validation engine
    - Conditionally trigger email service based on validation result
    - Update error request with validation and email status
    - Return appropriate response to user
    - _Requirements: 1.1, 1.2, 1.4, 2.5, 3.1, 3.3_

  - [-] 7.3 Write property test for invalid request rejection
    - **Property 4: Invalid request rejection**
    - **Validates: Requirements 1.4**

  - [ ] 7.4 Write property test for OpenAPI specification association
    - **Property 3: OpenAPI specification association**
    - **Validates: Requirements 1.2**

  - [ ] 7.5 Write property test for error request retrieval
    - **Property 20: Error request retrieval**
    - **Validates: Requirements 6.4**

  - [ ] 7.6 Write property test for filter correctness
    - **Property 22: Filter correctness**
    - **Validates: Requirements 6.3**

- [ ] 8. Implement environment management
  - [ ] 8.1 Create EnvironmentService
    - Implement getAllEnvironments method (sorted by name)
    - Implement updateEnvironmentStatus method
    - _Requirements: 4.1, 4.4, 4.5_

  - [ ] 8.2 Create EnvironmentController REST endpoints
    - GET /api/environments - list all environments
    - PUT /api/environments/{id} - update environment status (admin only)
    - _Requirements: 4.1, 4.4_

  - [ ] 8.3 Write property test for complete environment display
    - **Property 12: Complete environment display**
    - **Validates: Requirements 4.1, 4.2**

  - [ ] 8.4 Write property test for environment list sorting
    - **Property 14: Environment list sorting**
    - **Validates: Requirements 4.5**

  - [ ] 8.5 Write property test for environment status update
    - **Property 13: Environment status update**
    - **Validates: Requirements 4.4**

- [ ] 9. Implement global error handling and response formatting
  - Create @ControllerAdvice class for global exception handling
  - Implement handlers for validation errors, not found errors, database errors
  - Return consistent error response format with code, message, details, timestamp
  - Add logging for all errors
  - _Requirements: 1.4, 5.4_

- [ ] 10. Checkpoint - Ensure backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Implement frontend error submission form
  - [ ] 11.1 Create ErrorSubmissionForm component
    - Create form with fields: API endpoint, HTTP method, request payload (JSON editor), response status, response body, timestamp, environment
    - Add client-side validation for required fields
    - Implement form submission to POST /api/errors
    - Display loading indicator during submission
    - Show success/error messages based on response
    - Apply FNB color scheme using TailwindCSS
    - _Requirements: 1.1, 1.4, 8.1, 8.2, 9.1, 9.2, 9.4_

  - [ ] 11.2 Write property test for interactive form feedback
    - **Property 28: Interactive form feedback**
    - **Validates: Requirements 9.3**

  - [ ] 11.3 Write property test for loading indicator display
    - **Property 29: Loading indicator display**
    - **Validates: Requirements 9.4**

- [ ] 12. Implement user feedback notifications
  - [ ] 12.1 Create NotificationService for frontend
    - Implement success notification for validated requests with email sent
    - Implement error notification for validation failures with reasons
    - Implement warning notification for email delivery failures
    - Use toast/snackbar component for notifications
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ] 12.2 Write property test for validation success notification
    - **Property 25: Validation success notification**
    - **Validates: Requirements 8.1, 8.2**

  - [ ] 12.3 Write property test for validation failure notification
    - **Property 26: Validation failure notification**
    - **Validates: Requirements 8.3**

  - [ ] 12.4 Write property test for email delivery failure notification
    - **Property 27: Email delivery failure notification**
    - **Validates: Requirements 8.4**

- [ ] 13. Implement environment status dashboard
  - [ ] 13.1 Create EnvironmentDashboard component
    - Fetch environments from GET /api/environments
    - Display environment list with name and status badges
    - Use color-coded badges (green for active, red for inactive)
    - Implement polling or WebSocket for real-time updates
    - Apply FNB color scheme
    - _Requirements: 4.1, 4.2, 9.2_

- [ ] 14. Implement error request list and detail views
  - [ ] 14.1 Create ErrorRequestList component
    - Fetch error requests from GET /api/errors
    - Display paginated list with key information (timestamp, endpoint, method, validation status, email status)
    - Implement filters for date range, endpoint, validation status, environment
    - Add sorting options
    - Apply FNB color scheme
    - _Requirements: 6.1, 6.2, 6.3, 9.2_

  - [ ] 14.2 Create ErrorRequestDetail component
    - Fetch specific error request from GET /api/errors/{id}
    - Display all information including request payload, response body, validation results
    - Format JSON payloads with syntax highlighting
    - Show email delivery status
    - Apply FNB color scheme
    - _Requirements: 6.4, 6.5, 9.2_

  - [ ] 14.3 Write property test for error request list display completeness
    - **Property 21: Error request list display completeness**
    - **Validates: Requirements 6.2**

  - [ ] 14.4 Write property test for detail view completeness
    - **Property 23: Detail view completeness**
    - **Validates: Requirements 6.5**

- [ ] 15. Implement OpenAPI specification management UI
  - [ ] 15.1 Create OpenAPIManager component (admin)
    - Create upload form for OpenAPI files (JSON/YAML)
    - Display list of uploaded specifications
    - Implement update and view functionality
    - Show upload errors with descriptive messages
    - Apply FNB color scheme
    - _Requirements: 5.1, 5.4, 5.5, 5.6, 9.2_

  - [ ] 15.2 Write property test for specification list completeness
    - **Property 19: Specification list completeness**
    - **Validates: Requirements 5.6**

- [ ] 16. Implement routing and navigation
  - Set up React Router with routes for:
    - / - Dashboard with environment status
    - /errors - Error request list
    - /errors/:id - Error request detail
    - /submit - Error submission form
    - /admin/openapi - OpenAPI management (admin)
  - Create navigation component with FNB branding
  - _Requirements: 9.2_

- [ ] 17. Add concurrent request handling and testing
  - [ ] 17.1 Configure Spring Boot for concurrent requests
    - Configure thread pool settings
    - Add transaction management for data integrity
    - Implement optimistic locking for concurrent updates
    - _Requirements: 7.1, 7.2_

  - [ ] 17.2 Write property test for concurrent request independence
    - **Property 24: Concurrent request independence**
    - **Validates: Requirements 7.1, 7.2**

- [ ] 18. Final integration and polish
  - [ ] 18.1 Wire all components together
    - Ensure all API endpoints are connected to frontend
    - Test end-to-end workflows
    - Verify FNB color scheme is consistent throughout
    - Add responsive design for mobile devices
    - _Requirements: 9.2, 9.5_

  - [ ] 18.2 Write integration tests
    - Test complete error submission workflow
    - Test OpenAPI upload and validation workflow
    - Test email notification workflow with mock SMTP
    - _Requirements: All_

- [ ] 19. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties using a Java property-based testing library (e.g., jqwik or QuickTheories)
- Unit tests validate specific examples and edge cases
- FNB color scheme should be defined in a central theme configuration file
- All property tests should run minimum 100 iterations
- Backend uses Spring Boot with Java, frontend uses React with TypeScript
