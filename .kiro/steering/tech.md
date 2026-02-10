# Technology Stack

## Architecture Style

- **Event-driven, real-time application** with layered architecture
- **Language-agnostic design** - implementation can use any modern language
- **RESTful API** integration with OpenAPI 3.0 specifications

## Core Technologies

### Communication
- **Real-time updates**: WebSockets, Server-Sent Events, or similar bidirectional protocol
- **HTTP client**: For external API integration (Student Database, Account API)
- **REST API**: OpenAPI 3.0 compliant endpoints

### Data Storage
- **Persistent data store**: Database or file system for session results
- **In-memory state**: Session and player state management during active sessions

### Testing Framework

**Property-Based Testing** is a core requirement:
- **Python**: Hypothesis
- **TypeScript/JavaScript**: fast-check
- **Java**: jqwik
- **Go**: gopter
- **Rust**: proptest

**Test Configuration**:
- Minimum 100 iterations per property test
- Dual approach: unit tests + property-based tests
- Property tests must reference design document properties

## API Specifications

All API contracts are defined in YAML:
- `get-account-api.yaml`: OpenAPI 3.0 spec for account retrieval
- `data-mapping.yaml`: Input/output field mappings

## Performance Requirements

- **Score updates**: < 100ms latency
- **Database timeout**: 2 seconds max for student lookups
- **Retry logic**: Up to 3 attempts with exponential backoff for persistence

## Common Commands

*Note: Specific commands depend on chosen implementation language and build system*

### Testing
```bash
# Run all tests (including property-based tests)
[test-command] --run

# Run property tests with verbose output
[test-command] --verbose --filter property
```

### Development
```bash
# Start development server (if applicable)
[dev-command]

# Build project
[build-command]

# Lint/format code
[lint-command]
```

## Design Principles

1. **Pure functions** for score calculation (enables comprehensive testing)
2. **Atomic operations** for state updates
3. **Fail-fast validation** for inputs
4. **Graceful degradation** for non-critical failures
5. **Retry with backoff** for external service calls
