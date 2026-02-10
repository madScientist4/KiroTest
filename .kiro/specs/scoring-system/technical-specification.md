# Technical Specification Document
# Scoring System for Educational Quiz Sessions

**Version:** 1.0  
**Date:** February 9, 2026  
**Status:** Final  

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-09 | System Architect | Initial technical specification |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Overview](#2-system-overview)
3. [Technical Requirements](#3-technical-requirements)
4. [System Architecture](#4-system-architecture)
5. [Component Specifications](#5-component-specifications)
6. [Data Models and Schemas](#6-data-models-and-schemas)
7. [API Specifications](#7-api-specifications)
8. [Integration Specifications](#8-integration-specifications)
9. [Security Specifications](#9-security-specifications)
10. [Performance Requirements](#10-performance-requirements)
11. [Testing Strategy](#11-testing-strategy)
12. [Deployment Architecture](#12-deployment-architecture)
13. [Appendices](#13-appendices)

---

## 1. Executive Summary

### 1.1 Purpose

This technical specification defines the architecture, design, and implementation requirements for a real-time scoring system
designed for educational quiz and game sessions. The system tracks multiple players, calculates scores with streak-based multipliers, maintains real-time leaderboards, and integrates with external student databases.

### 1.2 Scope

The scoring system provides:
- Real-time score tracking for multiple concurrent sessions
- Player registration with student database integration
- Score calculation with streak-based multipliers (1.0x to 3.0x)
- Live leaderboard updates with ranking and tie-breaking
- Session lifecycle management (create, active, ended)
- Persistent storage of session results
- WebSocket-based real-time communication
- RESTful API for session management

### 1.3 Target Audience

This document is intended for:
- Software architects and developers implementing the system
- Quality assurance engineers designing test strategies
- DevOps engineers planning deployment infrastructure
- Technical project managers overseeing development
- Integration engineers connecting external systems

### 1.4 Technology Stack

**Primary Language:** Java 17+  
**Testing Framework:** JUnit 5, jqwik (property-based testing)  
**Build Tool:** Maven or Gradle  
**Real-Time Communication:** WebSocket (Java WebSocket API or Spring WebSocket)  
**Persistence:** Abstracted interface (supports JDBC, JPA, or NoSQL)  
**HTTP Client:** Java HttpClient or Apache HttpClient  
**JSON Processing:** Jackson or Gson  

---

## 2. System Overview

### 2.1 Business Context

Educational institutions require a robust scoring system for interactive quiz sessions where:
- Multiple students participate simultaneously
- Scores must update in real-time for immediate feedback
- Consistent performance (streaks) should be rewarded
- Results must be tracked and persisted for analysis
- Student identities must be verified against institutional databases

### 2.2 Key Features


**Player Management**
- Register players using student IDs from external database
- Prevent duplicate registrations within sessions
- Initialize player state (score=0, streak=0)

**Score Calculation**
- Award base points for correct answers
- Apply multipliers based on consecutive correct answers (streaks)
- Multiplier formula: `min(1.0 + (streak * 0.1), 3.0)`
- Reset streaks on incorrect answers
- Maintain score non-negativity invariant

**Real-Time Updates**
- Broadcast score changes to all session participants within 100ms
- Update leaderboard rankings in real-time
- Queue and retry failed message transmissions

**Leaderboard**
- Rank players by score (descending)
- Handle ties with alphabetical ordering by name
- Display rank, name, and score for all players

**Session Lifecycle**
- Create sessions with unique identifiers
- Accept registrations and score updates while active
- End sessions and trigger persistence
- Prevent modifications to ended sessions

**Data Persistence**
- Store session results with player scores
- Implement retry logic (up to 3 attempts with exponential backoff)
- Validate data integrity before storage

### 2.3 System Constraints

**Performance:**
- Score updates must complete within 100ms
- Student database queries timeout after 2 seconds
- Support multiple concurrent sessions

**Data Integrity:**
- All scores must be non-negative integers
- Session IDs must be unique (UUID format)
- Student IDs must be alphanumeric, 6-12 characters

**Reliability:**
- Graceful degradation when external services fail
- Message queuing for network failures
- Retry logic for transient failures

---

## 3. Technical Requirements

### 3.1 Functional Requirements


#### FR-1: Player Registration
- **FR-1.1:** System SHALL retrieve player name from Student Database using student ID
- **FR-1.2:** System SHALL reject registration if student ID not found in database
- **FR-1.3:** System SHALL initialize player score to zero upon successful registration
- **FR-1.4:** System SHALL prevent duplicate player registration within same session

#### FR-2: Score Calculation
- **FR-2.1:** System SHALL add base points to player score for correct answers
- **FR-2.2:** System SHALL apply streak multiplier to base points before adding to score
- **FR-2.3:** System SHALL reset player streak to zero on incorrect answers
- **FR-2.4:** System SHALL calculate multiplier using formula: `1 + (streak_count * 0.1)` with max 3.0
- **FR-2.5:** System SHALL ensure all score values remain non-negative integers

#### FR-3: Real-Time Score Updates
- **FR-3.1:** System SHALL update displayed score within 100 milliseconds of score change
- **FR-3.2:** System SHALL broadcast score updates to all connected clients in session
- **FR-3.3:** System SHALL update leaderboard display when rankings change

#### FR-4: Session Management
- **FR-4.1:** System SHALL generate unique session identifier when creating new session
- **FR-4.2:** System SHALL accept player registrations and score updates for active sessions
- **FR-4.3:** System SHALL persist all player scores when session ends
- **FR-4.4:** System SHALL prevent score updates to ended sessions
- **FR-4.5:** System SHALL maintain session state including all registered players and scores

#### FR-5: Leaderboard Display
- **FR-5.1:** System SHALL rank players by score in descending order
- **FR-5.2:** System SHALL display player name, score, and rank position
- **FR-5.3:** System SHALL assign same rank to players with equal scores, ordered alphabetically
- **FR-5.4:** System SHALL return current state for all players when leaderboard requested
- **FR-5.5:** System SHALL update leaderboard in real-time as scores change

#### FR-6: Data Persistence
- **FR-6.1:** System SHALL store all player scores with timestamps when session ends
- **FR-6.2:** System SHALL include session ID, student ID, player name, final score, and end time
- **FR-6.3:** System SHALL retry failed storage operations up to three times
- **FR-6.4:** System SHALL validate all fields before storage

#### FR-7: Student Database Integration
- **FR-7.1:** System SHALL use student ID as lookup key when querying Student Database
- **FR-7.2:** System SHALL return error and prevent registration if Student Database unavailable
- **FR-7.3:** System SHALL validate student ID and name fields present in database response
- **FR-7.4:** System SHALL handle Student Database response times up to 2 seconds

#### FR-8: Error Handling
- **FR-8.1:** System SHALL log errors and maintain previous valid score on calculation errors
- **FR-8.2:** System SHALL queue updates and retry transmission on network errors
- **FR-8.3:** System SHALL reject invalid student ID formats with descriptive error messages
- **FR-8.4:** System SHALL prioritize score calculation over leaderboard updates under resource constraints

### 3.2 Non-Functional Requirements


#### NFR-1: Performance
- **NFR-1.1:** Score calculation and update SHALL complete within 100ms (P95)
- **NFR-1.2:** Leaderboard generation SHALL complete within 200ms for up to 100 players
- **NFR-1.3:** System SHALL support minimum 10 concurrent sessions
- **NFR-1.4:** System SHALL handle minimum 50 players per session

#### NFR-2: Scalability
- **NFR-2.1:** Architecture SHALL support horizontal scaling of API layer
- **NFR-2.2:** Session state SHALL be maintainable in distributed cache if needed
- **NFR-2.3:** WebSocket connections SHALL be distributable across multiple servers

#### NFR-3: Reliability
- **NFR-3.1:** System SHALL achieve 99.5% uptime during active sessions
- **NFR-3.2:** System SHALL recover from transient failures without data loss
- **NFR-3.3:** System SHALL persist session data with 99.9% success rate

#### NFR-4: Maintainability
- **NFR-4.1:** Code SHALL achieve minimum 80% test coverage
- **NFR-4.2:** All public APIs SHALL be documented with JavaDoc
- **NFR-4.3:** Components SHALL follow SOLID principles for modularity

#### NFR-5: Security
- **NFR-5.1:** WebSocket connections SHALL be authenticated
- **NFR-5.2:** Student Database credentials SHALL be externalized and encrypted
- **NFR-5.3:** Input validation SHALL prevent injection attacks
- **NFR-5.4:** Session IDs SHALL be cryptographically random (UUID v4)

---

## 4. System Architecture

### 4.1 Architectural Style

The system follows a **layered architecture** with **event-driven** real-time communication:

**Presentation Layer:** WebSocket and REST API endpoints  
**Application Layer:** Session management, orchestration, and business logic  
**Domain Layer:** Pure business logic (score calculation, leaderboard ranking)  
**Infrastructure Layer:** Persistence, external service clients, messaging  

### 4.2 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
│              (Web Browser, Mobile App, etc.)                 │
└────────────────┬────────────────────────┬───────────────────┘
                 │                        │
                 │ WebSocket              │ REST API
                 │                        │
┌────────────────▼────────────────────────▼───────────────────┐
│                        API Layer                             │
│  ┌──────────────────┐      ┌──────────────────────────┐    │
│  │ WebSocket Handler│      │  REST Controllers        │    │
│  └──────────────────┘      └──────────────────────────┘    │
└────────────────┬────────────────────────┬───────────────────┘
                 │                        │
                 │                        │
┌────────────────▼────────────────────────▼───────────────────┐
│                   Application Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            Session Manager                           │   │
│  │  - Session lifecycle management                      │   │
│  │  - Player registration coordination                  │   │
│  │  - Score update orchestration                        │   │
│  └──────────────────────────────────────────────────────┘   │
└──┬─────────────┬──────────────┬──────────────┬─────────────┘
   │             │              │              │
   │             │              │              │
┌──▼─────────┐ ┌─▼──────────┐ ┌▼────────────┐ ┌▼─────────────┐
│   Score    │ │ Leaderboard│ │  Message    │ │ Persistence  │
│ Calculator │ │  Service   │ │ Broadcaster │ │    Layer     │
│            │ │            │ │             │ │              │
│ (Pure      │ │ (Ranking & │ │ (WebSocket  │ │ (Retry &     │
│ Functions) │ │ Sorting)   │ │ Broadcasting│ │ Validation)  │
└────────────┘ └────────────┘ └─────────────┘ └──────┬───────┘
                                                      │
┌─────────────────────────────────────────────────────▼───────┐
│                  Infrastructure Layer                        │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │ Student Database     │      │   Data Store         │    │
│  │ Client (HTTP)        │      │   (Database/Files)   │    │
│  └──────────────────────┘      └──────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Component Interaction Flow

**Player Registration Flow:**
```
Client → API Layer → Session Manager → Student DB Client → Student Database
                                    ↓
                         Initialize PlayerState (score=0, streak=0)
                                    ↓
                         Store in Session State
                                    ↓
                         Return Success → Client
```

**Score Update Flow:**
```
Client → API Layer → Session Manager → Score Calculator
                                    ↓
                         Update PlayerState
                                    ↓
                    ┌───────────────┴───────────────┐
                    ↓                               ↓
            Leaderboard Service            Message Broadcaster
                    ↓                               ↓
            Update Rankings              Broadcast to all clients
```

**Session End Flow:**
```
Client → API Layer → Session Manager → Change status to "ended"
                                    ↓
                         Persistence Layer (with retry)
                                    ↓
                         Store SessionRecord
                                    ↓
                         Return Final Summary → Client
```

### 4.4 Design Patterns


**Repository Pattern:** Persistence Layer abstracts data storage  
**Strategy Pattern:** Score calculation algorithm encapsulated  
**Observer Pattern:** Real-time updates via message broadcasting  
**Facade Pattern:** Session Manager coordinates multiple services  
**Result/Either Pattern:** Error handling without exceptions  

---

## 5. Component Specifications

### 5.1 Session Manager

**Responsibility:** Orchestrates session lifecycle, player management, and score updates

**State Management:**
```java
public class Session {
    private final SessionId id;
    private SessionStatus status; // ACTIVE, ENDED
    private final Map<StudentId, PlayerState> players;
    private final Instant startTime;
    private Instant endTime;
}

public class PlayerState {
    private final StudentId studentId;
    private final String name;
    private int score; // non-negative
    private int streak; // non-negative
}
```

**Public Interface:**
```java
public interface SessionManager {
    SessionId createSession();
    
    Result<PlayerState, Error> registerPlayer(
        SessionId sessionId, 
        StudentId studentId
    );
    
    Result<ScoreUpdate, Error> submitAnswer(
        SessionId sessionId,
        StudentId studentId,
        boolean isCorrect,
        int basePoints
    );
    
    Result<SessionSummary, Error> endSession(SessionId sessionId);
    
    Result<Session, Error> getSessionState(SessionId sessionId);
}
```

**Dependencies:**
- StudentDatabaseClient (for player registration)
- ScoreCalculator (for score computation)
- LeaderboardService (for ranking updates)
- PersistenceLayer (for session end)
- MessageBroadcaster (for real-time updates)

**Validation Rules:**
- Session must exist for all operations except createSession
- Session must be ACTIVE for registerPlayer and submitAnswer
- Player must be registered for submitAnswer
- No duplicate student IDs within a session

### 5.2 Score Calculator

**Responsibility:** Pure function for calculating scores with streak multipliers

**Algorithm Specification:**
```java
public class ScoreCalculator {
    private static final double MULTIPLIER_INCREMENT = 0.1;
    private static final double MAX_MULTIPLIER = 3.0;
    
    public ScoreResult calculateScore(
        int currentScore,
        int currentStreak,
        boolean isCorrect,
        int basePoints
    ) {
        if (!isCorrect) {
            return new ScoreResult(
                currentScore,  // score unchanged
                0,             // streak reset
                0,             // no points awarded
                0.0            // no multiplier
            );
        }
        
        int newStreak = currentStreak + 1;
        double multiplier = Math.min(
            1.0 + (newStreak * MULTIPLIER_INCREMENT),
            MAX_MULTIPLIER
        );
        int pointsAwarded = (int) Math.floor(basePoints * multiplier);
        int newScore = currentScore + pointsAwarded;
        
        return new ScoreResult(newScore, newStreak, pointsAwarded, multiplier);
    }
}
```

**Invariants:**
- Input: currentScore ≥ 0, currentStreak ≥ 0, basePoints ≥ 0
- Output: newScore ≥ currentScore (for correct answers)
- Output: 1.0 ≤ multiplier ≤ 3.0 (for correct answers)
- Output: newStreak = 0 (for incorrect answers)

**Properties (for Property-Based Testing):**
- Score never decreases
- Streak resets to 0 on incorrect answer
- Multiplier caps at 3.0
- Score remains non-negative

### 5.3 Leaderboard Service

**Responsibility:** Maintain sorted player rankings with tie-breaking

**Algorithm Specification:**
```java
public class LeaderboardService {
    public Leaderboard updateLeaderboard(
        SessionId sessionId,
        Map<StudentId, PlayerState> players
    ) {
        List<LeaderboardEntry> entries = players.entrySet().stream()
            .map(e -> new LeaderboardEntry(
                e.getKey(),
                e.getValue().getName(),
                e.getValue().getScore()
            ))
            .sorted(Comparator
                .comparing(LeaderboardEntry::getScore).reversed()
                .thenComparing(LeaderboardEntry::getName))
            .collect(Collectors.toList());
        
        // Assign ranks (ties get same rank)
        int currentRank = 1;
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0 && entries.get(i).getScore() < entries.get(i-1).getScore()) {
                currentRank = i + 1;
            }
            entries.get(i).setRank(currentRank);
        }
        
        return new Leaderboard(sessionId, entries);
    }
}
```

**Sorting Rules:**
1. Primary: Score (descending - highest first)
2. Secondary: Name (ascending - alphabetical)
3. Rank assignment: Same score = same rank

**Properties:**
- All registered players appear in leaderboard
- Entries sorted correctly by score then name
- Rank 1 has highest score
- Ties have same rank

### 5.4 Student Database Client

**Responsibility:** Interface with external student database via HTTP


**Interface:**
```java
public interface StudentDatabaseClient {
    Result<StudentInfo, Error> getStudentInfo(StudentId studentId);
}

public class StudentInfo {
    private final StudentId studentId;
    private final String name;
}
```

**Implementation Requirements:**
- HTTP GET request to: `{baseUrl}/students/{studentId}`
- Timeout: 2000ms
- Expected response: `{"studentId": "...", "name": "..."}`
- Status 200: Success
- Status 404: Student not found
- Timeout/Network error: Database unavailable

**Student ID Validation:**
```java
private boolean isValidStudentIdFormat(String studentId) {
    return studentId != null 
        && studentId.matches("^[a-zA-Z0-9-]{6,12}$");
}
```

**Error Handling:**
- Invalid format → Reject immediately
- Timeout → Return "Database unavailable" error
- 404 → Return "Student not found" error
- Invalid response → Return "Invalid database response" error

### 5.5 Persistence Layer

**Responsibility:** Store session results with retry logic

**Interface:**
```java
public interface PersistenceLayer {
    Result<Void, Error> saveSessionResults(Session session);
}

public class SessionRecord {
    private final SessionId sessionId;
    private final Instant endTime;
    private final List<PlayerRecord> players;
}

public class PlayerRecord {
    private final StudentId studentId;
    private final String name;
    private final int finalScore;
}
```

**Retry Logic:**
```java
private static final int MAX_RETRIES = 3;

public Result<Void, Error> saveSessionResults(Session session) {
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
        try {
            validateSessionData(session);
            storeToDatabase(session);
            return Result.success();
        } catch (Exception e) {
            if (attempt == MAX_RETRIES - 1) {
                return Result.error("Failed after 3 attempts: " + e.getMessage());
            }
            Thread.sleep(exponentialBackoff(attempt));
        }
    }
}

private long exponentialBackoff(int attempt) {
    return Math.min(100 * (long) Math.pow(2, attempt), 5000);
}
```

**Validation Rules:**
- Session ID must be valid UUID
- End time must be present
- All players must have valid student IDs
- All scores must be non-negative
- All names must be non-empty

### 5.6 Message Broadcaster

**Responsibility:** Broadcast real-time updates via WebSocket

**Interface:**
```java
public interface MessageBroadcaster {
    void broadcastScoreUpdate(SessionId sessionId, ScoreUpdateMessage message);
    void broadcastLeaderboardUpdate(SessionId sessionId, LeaderboardUpdateMessage message);
    void broadcastSessionEnded(SessionId sessionId, SessionEndedMessage message);
}
```

**Message Types:**
```java
public class ScoreUpdateMessage {
    private final String type = "SCORE_UPDATE";
    private final SessionId sessionId;
    private final StudentId studentId;
    private final int newScore;
    private final int newStreak;
    private final int pointsAwarded;
    private final double multiplierApplied;
}

public class LeaderboardUpdateMessage {
    private final String type = "LEADERBOARD_UPDATE";
    private final SessionId sessionId;
    private final Leaderboard leaderboard;
}

public class SessionEndedMessage {
    private final String type = "SESSION_ENDED";
    private final SessionId sessionId;
    private final Leaderboard finalLeaderboard;
}
```

**Reliability Features:**
- Message queuing for failed transmissions
- Retry with exponential backoff
- Per-session client tracking
- Graceful handling of disconnected clients

---

## 6. Data Models and Schemas

### 6.1 Core Domain Types

**SessionId**
```java
public class SessionId {
    private final UUID value;
    
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID());
    }
    
    public static SessionId fromString(String id) {
        return new SessionId(UUID.fromString(id));
    }
}
```

**StudentId**
```java
public class StudentId {
    private final String value;
    
    public StudentId(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid student ID format");
        }
        this.value = value;
    }
    
    private boolean isValid(String id) {
        return id != null && id.matches("^[a-zA-Z0-9-]{6,12}$");
    }
}
```

**SessionStatus**
```java
public enum SessionStatus {
    ACTIVE,
    ENDED
}
```

### 6.2 Error Model

```java
public enum ErrorCode {
    SESSION_NOT_FOUND,
    SESSION_ENDED,
    PLAYER_NOT_FOUND,
    DUPLICATE_PLAYER,
    STUDENT_NOT_FOUND,
    DATABASE_UNAVAILABLE,
    INVALID_INPUT,
    PERSISTENCE_FAILED
}

public class Error {
    private final ErrorCode code;
    private final String message;
    private final Map<String, String> details;
    private final Instant timestamp;
}

public class ErrorResponse {
    private final ErrorCode code;
    private final String message;
    private final Map<String, String> details;
    private final Instant timestamp;
}
```

### 6.3 Database Schema (Relational Example)


**sessions table:**
```sql
CREATE TABLE sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**session_players table:**
```sql
CREATE TABLE session_players (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(12) NOT NULL,
    player_name VARCHAR(255) NOT NULL,
    final_score INT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(session_id),
    UNIQUE KEY unique_player_per_session (session_id, student_id)
);
```

**Indexes:**
```sql
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_session_players_session ON session_players(session_id);
CREATE INDEX idx_session_players_student ON session_players(student_id);
```

---

## 7. API Specifications

### 7.1 REST API Endpoints

**Base URL:** `/api/v1`

#### 7.1.1 Create Session

**Endpoint:** `POST /sessions`

**Request:**
```json
{}
```

**Response (201 Created):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "startTime": "2026-02-09T10:30:00Z"
}
```

**Errors:**
- 500: Internal server error

---

#### 7.1.2 Register Player

**Endpoint:** `POST /sessions/{sessionId}/players`

**Request:**
```json
{
  "studentId": "STU123456"
}
```

**Response (201 Created):**
```json
{
  "studentId": "STU123456",
  "name": "John Doe",
  "score": 0,
  "streak": 0
}
```

**Errors:**
- 400: Invalid student ID format
- 404: Session not found or student not found in database
- 409: Duplicate player registration
- 410: Session already ended
- 503: Student database unavailable

---

#### 7.1.3 Submit Answer

**Endpoint:** `POST /sessions/{sessionId}/answers`

**Request:**
```json
{
  "studentId": "STU123456",
  "isCorrect": true,
  "basePoints": 10
}
```

**Response (200 OK):**
```json
{
  "studentId": "STU123456",
  "newScore": 11,
  "newStreak": 1,
  "pointsAwarded": 11,
  "multiplierApplied": 1.1
}
```

**Errors:**
- 400: Invalid input
- 404: Session or player not found
- 410: Session already ended

---

#### 7.1.4 End Session

**Endpoint:** `POST /sessions/{sessionId}/end`

**Request:**
```json
{}
```

**Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "endTime": "2026-02-09T11:30:00Z",
  "playerCount": 25,
  "finalLeaderboard": {
    "rankings": [
      {
        "rank": 1,
        "studentId": "STU123456",
        "name": "John Doe",
        "score": 150
      }
    ]
  }
}
```

**Errors:**
- 404: Session not found
- 409: Session already ended

---

#### 7.1.5 Get Session State

**Endpoint:** `GET /sessions/{sessionId}`

**Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "startTime": "2026-02-09T10:30:00Z",
  "endTime": null,
  "players": [
    {
      "studentId": "STU123456",
      "name": "John Doe",
      "score": 75,
      "streak": 3
    }
  ]
}
```

**Errors:**
- 404: Session not found

---

#### 7.1.6 Get Leaderboard

**Endpoint:** `GET /sessions/{sessionId}/leaderboard`

**Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "rankings": [
    {
      "rank": 1,
      "studentId": "STU123456",
      "name": "John Doe",
      "score": 150
    },
    {
      "rank": 2,
      "studentId": "STU789012",
      "name": "Jane Smith",
      "score": 140
    }
  ]
}
```

**Errors:**
- 404: Session not found

---

### 7.2 WebSocket API

**Connection Endpoint:** `ws://host:port/ws/sessions/{sessionId}`

**Authentication:** Include session token in connection headers

**Client → Server Messages:**

**Join Session:**
```json
{
  "type": "JOIN_SESSION",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Server → Client Messages:**

**Score Update:**
```json
{
  "type": "SCORE_UPDATE",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "studentId": "STU123456",
  "newScore": 75,
  "newStreak": 3,
  "pointsAwarded": 13,
  "multiplierApplied": 1.3
}
```

**Leaderboard Update:**
```json
{
  "type": "LEADERBOARD_UPDATE",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "leaderboard": {
    "rankings": [...]
  }
}
```

**Session Ended:**
```json
{
  "type": "SESSION_ENDED",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "finalLeaderboard": {
    "rankings": [...]
  }
}
```

---

## 8. Integration Specifications

### 8.1 Student Database Integration

**Protocol:** HTTP/HTTPS  
**Method:** GET  
**Endpoint:** `{baseUrl}/students/{studentId}`  
**Timeout:** 2000ms  
**Retry Policy:** No retry (fail fast)  

**Request Headers:**
```
Accept: application/json
Authorization: Bearer {token}
```

**Success Response (200):**
```json
{
  "studentId": "STU123456",
  "name": "John Doe",
  "email": "john.doe@university.edu"
}
```

**Error Responses:**
- 404: Student not found
- 401: Unauthorized
- 503: Service unavailable

**Required Fields:** studentId, name  
**Optional Fields:** email, department, year

### 8.2 Persistence Integration

**Supported Backends:**
- Relational databases (PostgreSQL, MySQL, Oracle)
- NoSQL databases (MongoDB, DynamoDB)
- File-based storage (JSON, CSV)

**Interface Contract:**
```java
public interface PersistenceLayer {
    Result<Void, Error> saveSessionResults(Session session);
    Result<SessionRecord, Error> getSessionResults(SessionId sessionId);
}
```

**Transaction Requirements:**
- Session and player records must be saved atomically
- Retry logic must handle transient failures
- Data validation must occur before persistence

---

## 9. Security Specifications

### 9.1 Authentication and Authorization

**WebSocket Connections:**
- Require session token in connection headers
- Validate token before accepting connection
- Disconnect on invalid or expired tokens

**REST API:**
- Require API key or JWT token in Authorization header
- Validate permissions for session operations
- Rate limiting: 100 requests per minute per client

### 9.2 Input Validation

**Student ID:**
- Pattern: `^[a-zA-Z0-9-]{6,12}$`
- Reject special characters except hyphen
- Sanitize before database queries

**Session ID:**
- Must be valid UUID v4
- Reject malformed UUIDs

**Base Points:**
- Must be positive integer
- Maximum value: 1000
- Reject negative or zero values

**Score Values:**
- Must be non-negative integers
- Maximum value: 2^31 - 1
- Prevent integer overflow

### 9.3 Data Protection

**Sensitive Data:**
- Student Database credentials stored in environment variables
- Database connection strings encrypted at rest
- API keys rotated regularly

**Logging:**
- Do not log student IDs in plain text (hash or mask)
- Do not log authentication tokens
- Log security events (failed auth, invalid input)

### 9.4 Network Security

**TLS/SSL:**
- Enforce HTTPS for all REST API endpoints
- Enforce WSS (WebSocket Secure) for WebSocket connections
- Minimum TLS version: 1.2

**CORS:**
- Configure allowed origins for web clients
- Restrict to known domains in production

---

## 10. Performance Requirements

### 10.1 Response Time Requirements

| Operation | Target (P95) | Maximum (P99) |
|-----------|--------------|---------------|
| Score calculation | 50ms | 100ms |
| Player registration | 500ms | 2000ms |
| Leaderboard generation | 100ms | 200ms |
| WebSocket message delivery | 50ms | 100ms |
| Session creation | 100ms | 200ms |

### 10.2 Throughput Requirements

| Metric | Minimum | Target |
|--------|---------|--------|
| Concurrent sessions | 10 | 50 |
| Players per session | 50 | 200 |
| Score updates per second | 100 | 500 |
| WebSocket connections | 500 | 2000 |

### 10.3 Resource Limits

**Memory:**
- Session state: ~1KB per player
- Maximum session size: 200 players = 200KB
- 50 concurrent sessions = 10MB

**CPU:**
- Score calculation: O(1) constant time
- Leaderboard sorting: O(n log n) where n = player count
- Target: <10% CPU utilization at 50% load

**Network:**
- WebSocket message size: <5KB per message
- Broadcast to 200 clients: 1MB per update
- Target bandwidth: 10 Mbps

---

## 11. Testing Strategy

### 11.1 Testing Approach

The system employs a **dual testing strategy**:

**Unit Tests:** Specific examples, edge cases, error conditions  
**Property-Based Tests:** Universal properties across all inputs  

### 11.2 Property-Based Testing

**Framework:** jqwik (Java)  
**Iterations:** Minimum 100 per property test  
**Tag Format:** `Feature: scoring-system, Property {N}: {description}`  

**Correctness Properties (23 total):**


1. Player registration initializes score to zero
2. Duplicate registration prevention
3. Score calculation with multipliers
4. Incorrect answer resets streak
5. Score non-negativity invariant
6. Score update broadcast
7. Leaderboard update propagation
8. Session ID uniqueness
9. Active session operations
10. Session end persistence
11. Ended session immutability
12. Session state consistency
13. Leaderboard sorting and ranking
14. Leaderboard completeness
15. Persistence retry behavior
16. Persistence data validation
17. Student database lookup
18. Database unavailability handling
19. Score calculation error recovery
20. Network error message queuing
21. Invalid student ID rejection
22. Student database integration round trip
23. Invalid student ID error handling

### 11.3 Test Coverage Requirements

**Component Coverage Targets:**

| Component | Unit Test Coverage | Property Test Coverage |
|-----------|-------------------|------------------------|
| Score Calculator | 100% | 3 properties |
| Leaderboard Service | 95% | 2 properties |
| Session Manager | 90% | 9 properties |
| Student DB Client | 85% | 4 properties |
| Persistence Layer | 85% | 3 properties |
| Message Broadcaster | 80% | 3 properties |

**Overall Target:** 85% code coverage

### 11.4 Integration Testing

**Test Scenarios:**
1. Complete session flow (create → register → submit → end → verify persistence)
2. Multi-player concurrent updates with leaderboard consistency
3. Error recovery (database failures, network errors)
4. Database integration with mocked HTTP responses
5. WebSocket message delivery end-to-end

**Performance Testing:**
- Load test: 50 concurrent sessions with 100 players each
- Stress test: Verify graceful degradation under high load
- Latency test: Verify 100ms score update requirement

### 11.5 Example Property Test

```java
@Property
@Label("Feature: scoring-system, Property 3: Score calculation with multipliers")
void scoreCalculationWithMultipliers(
    @ForAll @IntRange(min = 0, max = 10000) int currentScore,
    @ForAll @IntRange(min = 0, max = 50) int currentStreak,
    @ForAll @IntRange(min = 1, max = 100) int basePoints,
    @ForAll boolean isCorrect
) {
    ScoreCalculator calculator = new ScoreCalculator();
    ScoreResult result = calculator.calculateScore(
        currentScore, currentStreak, isCorrect, basePoints
    );
    
    if (isCorrect) {
        double expectedMultiplier = Math.min(1.0 + ((currentStreak + 1) * 0.1), 3.0);
        int expectedPoints = (int) Math.floor(basePoints * expectedMultiplier);
        int expectedScore = currentScore + expectedPoints;
        
        assertThat(result.getNewScore()).isEqualTo(expectedScore);
        assertThat(result.getNewStreak()).isEqualTo(currentStreak + 1);
        assertThat(result.getMultiplierApplied()).isEqualTo(expectedMultiplier);
        assertThat(result.getPointsAwarded()).isEqualTo(expectedPoints);
    } else {
        assertThat(result.getNewScore()).isEqualTo(currentScore);
        assertThat(result.getNewStreak()).isEqualTo(0);
        assertThat(result.getPointsAwarded()).isEqualTo(0);
    }
}
```

---

## 12. Deployment Architecture

### 12.1 Deployment Topology

**Single-Server Deployment (Development/Small Scale):**
```
┌─────────────────────────────────────┐
│         Application Server          │
│  ┌──────────────────────────────┐  │
│  │   Scoring System Application │  │
│  │   - REST API                 │  │
│  │   - WebSocket Server         │  │
│  │   - Business Logic           │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   Embedded Database          │  │
│  │   (H2, SQLite)               │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Multi-Server Deployment (Production/Large Scale):**
```
                    ┌─────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │ App     │       │ App     │       │ App     │
   │ Server 1│       │ Server 2│       │ Server 3│
   └────┬────┘       └────┬────┘       └────┬────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
                    ┌──────▼───────┐
                    │   Database   │
                    │   Cluster    │
                    └──────────────┘
```

### 12.2 Infrastructure Requirements

**Application Server:**
- Java 17+ runtime
- Minimum 2 CPU cores
- Minimum 4GB RAM
- 10GB disk space

**Database Server:**
- PostgreSQL 13+ or MySQL 8+
- Minimum 2 CPU cores
- Minimum 8GB RAM
- 50GB disk space (scales with session history)

**Network:**
- 100 Mbps minimum bandwidth
- Low latency (<50ms) between app and database
- WebSocket support (no proxy interference)

### 12.3 Configuration Management

**Environment Variables:**
```bash
# Application
APP_PORT=8080
APP_ENV=production

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=scoring_system
DB_USER=app_user
DB_PASSWORD=encrypted_password

# Student Database
STUDENT_DB_URL=https://student-api.university.edu
STUDENT_DB_TOKEN=encrypted_token
STUDENT_DB_TIMEOUT=2000

# WebSocket
WS_PORT=8081
WS_MAX_CONNECTIONS=2000

# Logging
LOG_LEVEL=INFO
LOG_FILE=/var/log/scoring-system/app.log
```

**Configuration Files:**
- `application.properties` or `application.yml`
- Externalized for different environments (dev, staging, prod)
- Secrets managed via environment variables or secret management service

### 12.4 Monitoring and Observability

**Metrics to Track:**
- Request rate (requests per second)
- Response time (P50, P95, P99)
- Error rate (errors per second)
- Active sessions count
- Active WebSocket connections
- Database connection pool utilization
- JVM memory usage and garbage collection

**Logging:**
- Application logs (INFO, WARN, ERROR)
- Access logs (HTTP requests)
- Security logs (authentication failures)
- Performance logs (slow queries)

**Health Checks:**
- `/health` endpoint for liveness probe
- `/ready` endpoint for readiness probe
- Check database connectivity
- Check student database availability

---

## 13. Appendices

### 13.1 Glossary

**Base Points:** Standard point value awarded for a correct answer  
**Leaderboard:** Ranked display of all players' scores in a session  
**Multiplier:** Factor applied to base points based on streak length  
**Player:** Student participating in a quiz or game session  
**Session:** Single quiz or game instance with defined start and end  
**Streak:** Consecutive sequence of correct answers by a player  
**Student Database:** External system containing student identification information  

### 13.2 Acronyms

**API:** Application Programming Interface  
**CORS:** Cross-Origin Resource Sharing  
**HTTP:** Hypertext Transfer Protocol  
**HTTPS:** HTTP Secure  
**JVM:** Java Virtual Machine  
**REST:** Representational State Transfer  
**SQL:** Structured Query Language  
**TLS:** Transport Layer Security  
**UUID:** Universally Unique Identifier  
**WebSocket:** Full-duplex communication protocol  
**WSS:** WebSocket Secure  

### 13.3 References

**Requirements Document:** `.kiro/specs/scoring-system/requirements.md`  
**Design Document:** `.kiro/specs/scoring-system/design.md`  
**Implementation Tasks:** `.kiro/specs/scoring-system/tasks.md`  

**External Standards:**
- RFC 4122: UUID Specification
- RFC 6455: WebSocket Protocol
- RFC 7231: HTTP/1.1 Semantics

**Testing Frameworks:**
- JUnit 5: https://junit.org/junit5/
- jqwik: https://jqwik.net/

### 13.4 Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | 2026-02-09 | System Architect | Initial technical specification |

---

**End of Technical Specification Document**
