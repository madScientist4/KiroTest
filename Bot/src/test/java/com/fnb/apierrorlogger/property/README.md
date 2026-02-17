# Property-Based Tests for API Error Logger

This directory contains property-based tests using jqwik framework.

## Error Request Persistence Property Test

### Test: `ErrorRequestPersistencePropertyTest`

**Property 1: Error request persistence round-trip**
- **Validates**: Requirements 1.1, 1.5
- **Description**: For any valid error request with all required fields, creating the request should result in a persisted record that can be immediately retrieved from the database with the same data.

### Test Configuration

- **Framework**: jqwik 1.8.2 with Spring support
- **Iterations**: 100 per property test
- **Database**: H2 in-memory database (configured in `application-test.properties`)
- **Test Type**: Integration test with `@DataJpaTest`

### What the Test Does

1. **Generates** 100 random valid error requests with:
   - API endpoints (e.g., `/api/users`, `/api/orders`)
   - HTTP methods (GET, POST, PUT, DELETE, PATCH)
   - Request payloads (JSON format)
   - Response statuses (100-599)
   - Response bodies (JSON format)
   - Timestamps (between 2020 and now)
   - Environments (development, staging, production, test)
   - OpenAPI specification IDs (UUIDs)
   - Validation statuses (passed, failed, unable_to_validate, null)
   - Validation details (JSON format, nullable)
   - Email sent flags (true/false)
   - Email delivery statuses (sent, failed, not_sent, null)

2. **Persists** each generated error request to the database

3. **Retrieves** the persisted error request by its ID

4. **Verifies** that all fields match between the original and retrieved request

### Running the Test

#### Option 1: Using Maven (Recommended)
```bash
mvn test -Dtest=ErrorRequestPersistencePropertyTest
```

#### Option 2: Using the provided batch script
```bash
run-property-test.bat
```

#### Option 3: Using your IDE
- Right-click on `ErrorRequestPersistencePropertyTest.java`
- Select "Run" or "Run Tests"

### Expected Output

When the test passes, you should see output similar to:
```
[INFO] Running com.fnb.apierrorlogger.property.ErrorRequestPersistencePropertyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The test will run 100 iterations with different randomly generated error requests.

### Dependencies Required

The following dependencies are configured in `pom.xml`:
- `net.jqwik:jqwik:1.8.2` - Property-based testing framework
- `net.jqwik:jqwik-spring:1.0.0` - Spring integration for jqwik
- `spring-boot-starter-test` - Spring Boot testing support
- `spring-boot-starter-data-jpa` - JPA support
- `h2` - In-memory database for testing

### Troubleshooting

**Issue**: Maven not found
- **Solution**: Install Maven from https://maven.apache.org/download.cgi and add it to your PATH

**Issue**: Test fails with database errors
- **Solution**: Check that H2 database dependency is in `pom.xml` and `application-test.properties` is configured correctly

**Issue**: Test fails with Spring context errors
- **Solution**: Ensure `jqwik-spring` dependency is present and `@JqwikSpringSupport` annotation is on the test class


## Unique Identifier Assignment Property Test

### Test: `UniqueIdentifierPropertyTest`

**Property 2: Unique identifier assignment**
- **Validates**: Requirements 1.3
- **Description**: For any set of error requests created, all assigned identifiers should be unique across the entire set.

### Test Configuration

- **Framework**: jqwik 1.8.2 with Spring support
- **Iterations**: 100 per property test
- **Database**: H2 in-memory database (configured in `application-test.properties`)
- **Test Type**: Integration test with `@DataJpaTest`

### What the Test Does

1. **Generates** 100 random sets of error requests (2-20 requests per set)

2. **Persists** all error requests in each set to the database

3. **Extracts** all assigned IDs from the persisted requests

4. **Verifies** that:
   - All IDs are non-null
   - All IDs are unique (no duplicates)
   - The number of unique IDs equals the total number of requests

### Running the Test

#### Option 1: Using Maven (Recommended)
```bash
mvn test -Dtest=UniqueIdentifierPropertyTest
```

#### Option 2: Using the provided batch script
```bash
run-unique-identifier-test.bat
```

#### Option 3: Using your IDE
- Right-click on `UniqueIdentifierPropertyTest.java`
- Select "Run" or "Run Tests"

### Expected Output

When the test passes, you should see output similar to:
```
[INFO] Running com.fnb.apierrorlogger.property.UniqueIdentifierPropertyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The test will run 100 iterations with different randomly generated sets of error requests.

### Why This Test Matters

This property test ensures that the UUID generation strategy (`@GeneratedValue(strategy = GenerationType.UUID)`) in the `ErrorRequest` entity correctly assigns unique identifiers to all error requests, even when multiple requests are created in quick succession. This is critical for:
- Data integrity
- Preventing ID collisions
- Ensuring reliable error request retrieval
- Supporting concurrent error submissions (Requirement 7.1, 7.2)

## Multi-Format Specification Upload Property Test

### Test: `MultiFormatSpecificationUploadPropertyTest`

**Property 15: Multi-format specification upload**
- **Validates**: Requirements 5.1, 5.2
- **Description**: For any valid OpenAPI specification in JSON or YAML format, the system should successfully parse and store the specification.

### Test Configuration

- **Framework**: jqwik 1.8.2 with Spring support
- **Iterations**: 100 per property test
- **Database**: H2 in-memory database (configured in `application-test.properties`)
- **Test Type**: Integration test with `@DataJpaTest`

### What the Test Does

1. **Generates** 100 random valid OpenAPI specifications with:
   - Random API identifiers (e.g., `api-xyz`)
   - Random API titles and descriptions
   - Random version numbers (semantic versioning format)
   - Random endpoint paths and descriptions
   - **Both JSON and YAML formats** (randomly selected)
   - Random uploaded by usernames

2. **Uploads** each generated specification using the `OpenAPIManager` service

3. **Verifies** that:
   - The specification is successfully stored with a non-null ID
   - All fields (API identifier, content, uploaded by) are correctly persisted
   - The version is extracted and stored correctly
   - Upload and update timestamps are set
   - The specification can be retrieved from the database

4. **Cleans up** after each iteration to ensure test isolation

### Running the Test

#### Option 1: Using Maven (Recommended)
```bash
mvn test -Dtest=MultiFormatSpecificationUploadPropertyTest
```

#### Option 2: Using the provided batch script
```bash
run-multi-format-spec-test.bat
```

#### Option 3: Using your IDE
- Right-click on `MultiFormatSpecificationUploadPropertyTest.java`
- Select "Run" or "Run Tests"

### Expected Output

When the test passes, you should see output similar to:
```
[INFO] Running com.fnb.apierrorlogger.property.MultiFormatSpecificationUploadPropertyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The test will run 100 iterations with different randomly generated OpenAPI specifications in both JSON and YAML formats.

### Why This Test Matters

This property test ensures that the `OpenAPIManager` service correctly:
- Parses both JSON and YAML OpenAPI specification formats
- Validates the OpenAPI structure using the swagger-parser library
- Extracts version information from specifications
- Persists specifications to the database
- Maintains data integrity across different input formats

This is critical for:
- Supporting multiple specification formats (Requirement 5.1)
- Validating specification format (Requirement 5.2)
- Ensuring reliable specification storage and retrieval
- Enabling error request validation against stored specifications

## Invalid Specification Rejection Property Test

### Test: `InvalidSpecificationRejectionPropertyTest`

**Property 17: Invalid specification rejection**
- **Validates**: Requirements 5.4
- **Description**: For any invalid OpenAPI specification file (malformed JSON/YAML or invalid OpenAPI structure), the upload should be rejected with a descriptive error message.

### Test Configuration

- **Framework**: jqwik 1.8.2 with Spring support
- **Iterations**: 100 per property test
- **Database**: H2 in-memory database (configured in `application-test.properties`)
- **Test Type**: Integration test with `@DataJpaTest`

### What the Test Does

1. **Generates** 100 random invalid OpenAPI specifications including:
   - Empty content
   - Null content
   - Whitespace-only content
   - Malformed JSON (unclosed braces, invalid syntax)
   - Malformed YAML (incorrect indentation)
   - Invalid JSON structure (missing required OpenAPI fields)
   - Invalid YAML structure (missing required OpenAPI fields)
   - Missing OpenAPI version field
   - Missing info section
   - Invalid OpenAPI version numbers
   - Random text (not JSON or YAML)
   - Incomplete JSON (truncated content)

2. **Attempts to upload** each invalid specification using the `OpenAPIManager` service

3. **Verifies** that:
   - An `IllegalArgumentException` is thrown
   - The exception message contains a descriptive error fragment
   - No specification is stored in the database
   - The system properly rejects all types of invalid input

4. **Cleans up** after each iteration to ensure test isolation

### Running the Test

#### Option 1: Using Gradle (Recommended)
```bash
gradlew test --tests InvalidSpecificationRejectionPropertyTest
```

#### Option 2: Using the provided batch script
```bash
run-invalid-spec-test.bat
```

#### Option 3: Using your IDE
- Right-click on `InvalidSpecificationRejectionPropertyTest.java`
- Select "Run" or "Run Tests"

### Expected Output

When the test passes, you should see output similar to:
```
[INFO] Running com.fnb.apierrorlogger.property.InvalidSpecificationRejectionPropertyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

The test will run 100 iterations with different types of invalid OpenAPI specifications.

### Why This Test Matters

This property test ensures that the `OpenAPIManager` service correctly:
- Rejects empty, null, or whitespace-only content
- Detects and rejects malformed JSON and YAML
- Validates OpenAPI structure requirements
- Provides descriptive error messages for all failure cases
- Prevents invalid specifications from being stored in the database

This is critical for:
- Data integrity and system reliability (Requirement 5.4)
- Providing clear feedback to administrators
- Preventing validation errors downstream
- Ensuring only valid specifications are used for error request validation

## Running All Property Tests

To run all property tests at once:
```bash
gradlew test --tests "*PropertyTest"
```

Or run all batch scripts sequentially.
