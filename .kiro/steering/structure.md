# Project Structure

## Directory Organization

```
.
├── .kiro/
│   ├── specs/
│   │   └── scoring-system/          # Feature specifications
│   │       ├── requirements.md       # User stories & acceptance criteria
│   │       ├── design.md            # Architecture & correctness properties
│   │       ├── tasks.md             # Implementation task list
│   │       ├── technical-specification.md
│   │       ├── business-process-flows.md
│   │       ├── use-case-diagrams.md
│   │       └── activity-diagrams.md
│   └── steering/                    # AI assistant guidance documents
│       ├── product.md               # Product overview
│       ├── tech.md                  # Technology stack
│       └── structure.md             # This file
├── data-mapping.yaml                # Input/output field mappings
└── get-account-api.yaml             # OpenAPI 3.0 account API spec
```

## Specification-Driven Development

This project follows **spec-driven development** methodology:

1. **Requirements** define user stories and acceptance criteria
2. **Design** specifies architecture, components, and correctness properties
3. **Tasks** break down implementation into actionable steps
4. **Implementation** follows the spec with property-based testing validation

### Spec File Conventions

- **Location**: `.kiro/specs/{feature-name}/`
- **Naming**: kebab-case for feature directories (e.g., `scoring-system`)
- **Required files**: `requirements.md`, `design.md`, `tasks.md`
- **File references**: Use `#[[file:<relative_path>]]` to include external specs (OpenAPI, GraphQL, etc.)

## Component Architecture

The system follows a **layered architecture**:

### API Layer
- Handles client connections (WebSocket/HTTP)
- Routes requests to services
- Manages real-time communication
- Integrates with external APIs

### Business Logic Layer
- **Session Manager**: Lifecycle and state management
- **Score Calculator**: Pure functions for point calculation
- **Leaderboard Service**: Ranking and sorting logic

### Data Layer
- **Persistence Layer**: Storage abstraction with retry logic
- **Student Database Client**: External API integration
- **In-memory State**: Active session data

## Correctness Properties

The design document defines **23 correctness properties** that must be validated through property-based testing. Each property:
- Maps to specific acceptance criteria
- Must be implemented as a property-based test
- Requires minimum 100 test iterations
- Uses format: `Feature: scoring-system, Property {N}: {description}`

## Testing Organization

- **Unit tests**: Specific examples and edge cases
- **Property-based tests**: Universal properties across all inputs
- **Integration tests**: End-to-end scenarios with mocked dependencies
- **Performance tests**: Verify < 100ms latency requirements

## API Contracts

All external API contracts are defined in YAML specifications:
- OpenAPI 3.0 format for REST endpoints
- Data mapping files for input/output transformations
- Version controlled alongside code

## State Management

- **Active sessions**: In-memory with real-time updates
- **Completed sessions**: Persisted to storage with retry logic
- **Player state**: Score, streak, and metadata per session
- **Leaderboard**: Computed on-demand from player states
