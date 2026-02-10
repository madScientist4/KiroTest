# Implementation Plan: Scoring System

## Overview

This implementation plan breaks down the scoring system into discrete coding tasks using Java. The approach follows a bottom-up strategy: implementing core business logic first (score calculation), then building stateful components (session management), followed by integration layers (persistence, database client), and finally the API layer with real-time communication.

The implementation uses:
- Java 17+ with modern language features
- Property-based testing with jqwik
- JUnit 5 for unit tests
- WebSocket for real-time communication
- A persistence abstraction (implementation-agnostic)

## Tasks

- [ ] 1. Set up project structure and dependencies
  - Create Maven/Gradle project with Java 17+
  - Add dependencies: jqwik (property testing), JUnit 5, WebSocket library, JSON library
  - Set up package structure: `core`, `session`, `persistence`, `database`, `api`, `model`
  - Create basic model classes: `SessionId`, `StudentId`, `PlayerState`, `Session`
  - _Requirements: All_

- [ ] 2. Implement Score Calculator (pure functions)
  - [ ] 2.1 Create ScoreCalculator class with calculateScore method
    - Implement score calculation algorithm with multiplier logic
    - Handle correct answers: calculate multiplier = min(1.0 + ((streak + 1) * 0.1), 3.0)
    - Handle incorrect answers: maintain score, reset streak to 0
    - Return ScoreResult with newScore, newStreak, pointsAwarded, multiplierApplied
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [ ]* 2.2 Write property test for score calculation with multipliers
    - **Property 3: Score Calculation with Multipliers**
    - **Validates: Requirements 2.1, 2.2, 2.4**
    - Generate random: currentScore (0-10000), currentStreak (0-50), basePoints (1-100), isCorrect
    - Verify correct calculation for both correct and incorrect answers
  
  - [ ]* 2.3 Write property test for streak reset on incorrect answers
    - **Property 4: Incorrect Answer Resets Streak**
    - **Validates: Requirements 2.3**
    - Generate random player states with various streaks
    - Verify streak resets to 0 on incorrect answer while score unchanged
  
  - [ ]* 2.4 Write property test for score non-negativity invariant
    - **Property 5: Score Non-Negativity Invariant**
    - **Validates: Requirements 2.5**
    - Generate random sequences of operations
    - Verify scores remain non-negative integers throughout
  
  - [ ]* 2.5 Write unit tests for edge cases
    - Test zero base points
    - Test maximum streak (multiplier caps at 3.0)
    - Test streak boundary (when multiplier reaches 3.0)
    - _Requirements: 2.1, 2.2, 2.4_

- [ ] 3. Implement Leaderboard Service
  - [ ] 3.1 Create LeaderboardService class
    - Implement updateLeaderboard method: sort by score desc, then name asc
    - Implement rank assignment with tie handling (same score = same rank)
    - Create LeaderboardEntry and Leaderboard model classes
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [ ]* 3.2 Write property test for leaderboard sorting and ranking
    - **Property 13: Leaderboard Sorting and Ranking**
    - **Validates: Requirements 5.1, 5.2, 5.3**
    - Generate random sets of players with various scores
    - Verify correct sorting, ranking, and tie-breaking
    - Verify all required fields present
  
  - [ ]* 3.3 Write property test for leaderboard completeness
    - **Property 14: Leaderboard Completeness**
    - **Validates: Requirements 5.4**
    - Generate random sessions with players
    - Verify leaderboard includes all registered players
  
  - [ ]* 3.4 Write unit tests for leaderboard edge cases
    - Test empty leaderboard
    - Test single player
    - Test all players with same score
    - Test alphabetical ordering for ties
    - _Requirements: 5.1, 5.3_

- [ ] 4. Checkpoint - Ensure core logic tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement Student Database Client
  - [ ] 5.1 Create StudentDatabaseClient interface and implementation
    - Define getStudentInfo method returning Result<StudentInfo, Error>
    - Implement HTTP client with 2-second timeout
    - Implement student ID format validation (alphanumeric, 6-12 chars)
    - Implement response validation (check studentId and name fields present)
    - Handle errors: timeout, network error, not found, invalid response
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 8.3_
  
  - [ ]* 5.2 Write property test for student database lookup
    - **Property 17: Student Database Lookup**
    - **Validates: Requirements 7.1, 7.3**
    - Generate random valid student IDs
    - Mock database responses
    - Verify correct lookup key usage and response validation
  
  - [ ]* 5.3 Write property test for invalid student ID rejection
    - **Property 21: Invalid Student ID Rejection**
    - **Validates: Requirements 8.3**
    - Generate random invalid student ID formats
    - Verify all are rejected with descriptive errors
  
  - [ ]* 5.4 Write property test for database unavailability handling
    - **Property 18: Database Unavailability Handling**
    - **Validates: Requirements 7.2**
    - Simulate database unavailability
    - Verify error returned and registration prevented
  
  - [ ]* 5.5 Write unit tests for database client
    - Test timeout handling
    - Test student not found (404 response)
    - Test invalid response format
    - Test network errors
    - _Requirements: 7.2, 7.3, 7.4_

- [ ] 6. Implement Persistence Layer
  - [ ] 6.1 Create PersistenceLayer interface and implementation
    - Define saveSessionResults method with retry logic
    - Implement exponential backoff: min(100 * 2^attempt, 5000)ms
    - Implement data validation before storage
    - Create SessionRecord and PlayerRecord model classes
    - Handle up to 3 retry attempts
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [ ]* 6.2 Write property test for session persistence completeness
    - **Property 10: Session End Persistence**
    - **Validates: Requirements 4.3, 6.1, 6.2**
    - Generate random sessions with players
    - Verify all data persisted: session ID, student IDs, names, scores, timestamp
  
  - [ ]* 6.3 Write property test for persistence retry behavior
    - **Property 15: Persistence Retry Behavior**
    - **Validates: Requirements 6.3**
    - Simulate storage failures
    - Verify exactly 3 retry attempts with exponential backoff
  
  - [ ]* 6.4 Write property test for data validation
    - **Property 16: Persistence Data Validation**
    - **Validates: Requirements 6.4**
    - Generate random valid and invalid session data
    - Verify validation occurs before storage
    - Verify invalid data rejected
  
  - [ ]* 6.5 Write unit tests for persistence layer
    - Test successful save
    - Test retry on transient failure
    - Test failure after 3 retries
    - Test validation errors
    - _Requirements: 6.3, 6.4_

- [ ] 7. Implement Session Manager
  - [ ] 7.1 Create SessionManager class with state management
    - Implement createSession: generate unique UUID, initialize empty player map
    - Implement registerPlayer: validate session active, query database, initialize player state
    - Implement submitAnswer: validate session/player, call ScoreCalculator, update state
    - Implement endSession: change status to ended, trigger persistence
    - Implement getSessionState: return current session snapshot
    - Maintain in-memory session state: Map<SessionId, Session>
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ]* 7.2 Write property test for player registration initialization
    - **Property 1: Player Registration Initializes Score to Zero**
    - **Validates: Requirements 1.3**
    - Generate random valid student IDs and sessions
    - Verify initial score and streak are zero
  
  - [ ]* 7.3 Write property test for duplicate registration prevention
    - **Property 2: Duplicate Registration Prevention**
    - **Validates: Requirements 1.4**
    - Generate random sessions with players
    - Attempt duplicate registrations
    - Verify error returned and player list unchanged
  
  - [ ]* 7.4 Write property test for session ID uniqueness
    - **Property 8: Session ID Uniqueness**
    - **Validates: Requirements 4.1**
    - Create many sessions (100+)
    - Verify all session IDs are unique
  
  - [ ]* 7.5 Write property test for active session operations
    - **Property 9: Active Session Operations**
    - **Validates: Requirements 4.2**
    - Generate random active sessions
    - Verify registrations and score updates accepted
  
  - [ ]* 7.6 Write property test for ended session immutability
    - **Property 11: Ended Session Immutability**
    - **Validates: Requirements 4.4**
    - Generate random ended sessions
    - Attempt score updates
    - Verify rejected with error and state unchanged
  
  - [ ]* 7.7 Write property test for session state consistency
    - **Property 12: Session State Consistency**
    - **Validates: Requirements 4.5**
    - Generate random sessions with operations
    - Query state at various points
    - Verify accurate player information returned
  
  - [ ]* 7.8 Write property test for database integration round trip
    - **Property 22: Student Database Integration Round Trip**
    - **Validates: Requirements 1.1**
    - Generate random valid student IDs
    - Mock database responses with names
    - Register players and verify names match database
  
  - [ ]* 7.9 Write property test for invalid student ID error handling
    - **Property 23: Invalid Student ID Error Handling**
    - **Validates: Requirements 1.2**
    - Generate student IDs not in database
    - Verify registration rejected with appropriate error
  
  - [ ]* 7.10 Write unit tests for session manager
    - Test session lifecycle transitions
    - Test error handling during registration
    - Test error handling during score updates
    - Test concurrent operations (thread safety if needed)
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 8. Checkpoint - Ensure business logic tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement Real-Time Communication Layer
  - [ ] 9.1 Create MessageBroadcaster class
    - Implement broadcast method for score updates
    - Implement broadcast method for leaderboard updates
    - Implement message queuing for failed transmissions
    - Implement retry logic with exponential backoff
    - Create message models: ScoreUpdateMessage, LeaderboardUpdateMessage, SessionEndedMessage
    - _Requirements: 3.2, 3.3, 8.2_
  
  - [ ]* 9.2 Write property test for score update broadcast
    - **Property 6: Score Update Broadcast**
    - **Validates: Requirements 3.2**
    - Generate random score changes
    - Mock multiple connected clients
    - Verify all clients receive update message with correct data
  
  - [ ]* 9.3 Write property test for leaderboard update propagation
    - **Property 7: Leaderboard Update Propagation**
    - **Validates: Requirements 3.3**
    - Generate random score changes affecting rankings
    - Verify all players receive updated leaderboard
  
  - [ ]* 9.4 Write property test for network error message queuing
    - **Property 20: Network Error Message Queuing**
    - **Validates: Requirements 8.2**
    - Simulate network errors during transmission
    - Verify messages queued and retried (not dropped)
  
  - [ ]* 9.5 Write unit tests for message broadcaster
    - Test successful broadcast to multiple clients
    - Test retry on network failure
    - Test message queue management
    - _Requirements: 3.2, 3.3, 8.2_

- [ ] 10. Implement API Layer with WebSocket support
  - [ ] 10.1 Create WebSocket endpoint handlers
    - Implement connection handler: authenticate and track clients
    - Implement message handler: route requests to SessionManager
    - Implement disconnect handler: clean up client connections
    - Create request/response models for API
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [ ] 10.2 Create REST endpoints for session management
    - POST /sessions - create new session
    - POST /sessions/{id}/players - register player
    - POST /sessions/{id}/answers - submit answer
    - POST /sessions/{id}/end - end session
    - GET /sessions/{id} - get session state
    - GET /sessions/{id}/leaderboard - get leaderboard
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.4_
  
  - [ ]* 10.3 Write integration tests for API endpoints
    - Test complete session flow: create → register → submit → end
    - Test WebSocket message delivery
    - Test error responses
    - _Requirements: All_

- [ ] 11. Implement Error Handling and Recovery
  - [ ] 11.1 Create error handling infrastructure
    - Define ErrorCode enum with all error types
    - Create ErrorResponse model
    - Implement error logging
    - Implement graceful degradation logic
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [ ]* 11.2 Write property test for score calculation error recovery
    - **Property 19: Score Calculation Error Recovery**
    - **Validates: Requirements 8.1**
    - Simulate errors during calculation
    - Verify previous score maintained and error logged
  
  - [ ]* 11.3 Write unit tests for error handling
    - Test all error code paths
    - Test error message formatting
    - Test logging behavior
    - _Requirements: 8.1, 8.2, 8.3_

- [ ] 12. Wire all components together
  - [ ] 12.1 Create main application class
    - Initialize all components with dependency injection
    - Wire SessionManager with ScoreCalculator, LeaderboardService, PersistenceLayer, StudentDatabaseClient
    - Wire API layer with SessionManager and MessageBroadcaster
    - Configure WebSocket server
    - Add application configuration (ports, database URLs, etc.)
    - _Requirements: All_
  
  - [ ]* 12.2 Write end-to-end integration tests
    - Test complete multi-player session with real-time updates
    - Test concurrent sessions
    - Test error recovery scenarios
    - Test database integration
    - Test persistence
    - _Requirements: All_

- [ ] 13. Final checkpoint - Ensure all tests pass
  - Run all unit tests and property tests
  - Verify test coverage meets requirements
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik with minimum 100 iterations
- All property tests include tags: `Feature: scoring-system, Property {N}: {description}`
- Core business logic (ScoreCalculator, LeaderboardService) is implemented first as pure functions for easier testing
- Session Manager coordinates all components and maintains state
- Real-time communication is abstracted for testability
- The implementation prioritizes testability and separation of concerns
