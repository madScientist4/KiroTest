# Sequence Diagrams

## Overview

This document contains sequence diagrams illustrating the key interactions and flows within the scoring system. These diagrams show how components collaborate to fulfill the system's requirements.

---

## 1. Player Registration Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant SDC as Student Database Client
    participant SDB as Student Database
    participant MB as Message Broadcaster

    Client->>API: POST /sessions/{sessionId}/players<br/>{studentId: "STU123456"}
    API->>SM: registerPlayer(sessionId, studentId)
    
    SM->>SM: Validate session exists and is ACTIVE
    SM->>SM: Check for duplicate registration
    
    SM->>SDC: getStudentInfo(studentId)
    SDC->>SDC: Validate student ID format
    SDC->>SDB: GET /students/STU123456
    
    alt Student Found
        SDB-->>SDC: 200 OK {studentId, name}
        SDC-->>SM: Success(StudentInfo)
        
        SM->>SM: Create PlayerState<br/>(score=0, streak=0)
        SM->>SM: Add player to session
        
        SM->>MB: Broadcast player joined event
        MB->>Client: WebSocket: PLAYER_JOINED
        
        SM-->>API: Success(PlayerState)
        API-->>Client: 201 Created<br/>{studentId, name, score: 0, streak: 0}
        
    else Student Not Found
        SDB-->>SDC: 404 Not Found
        SDC-->>SM: Error(STUDENT_NOT_FOUND)
        SM-->>API: Error(STUDENT_NOT_FOUND)
        API-->>Client: 404 Not Found<br/>{error: "Student not found"}
        
    else Database Unavailable
        SDB-->>SDC: Timeout/Network Error
        SDC-->>SM: Error(DATABASE_UNAVAILABLE)
        SM-->>API: Error(DATABASE_UNAVAILABLE)
        API-->>Client: 503 Service Unavailable<br/>{error: "Database unavailable"}
    end
```

---

## 2. Score Update Flow (Correct Answer)

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant SC as Score Calculator
    participant LS as Leaderboard Service
    participant MB as Message Broadcaster
    participant AllClients as All Session Clients

    Client->>API: POST /sessions/{sessionId}/answers<br/>{studentId, isCorrect: true, basePoints: 10}
    API->>SM: submitAnswer(sessionId, studentId, true, 10)
    
    SM->>SM: Validate session is ACTIVE
    SM->>SM: Validate player is registered
    SM->>SM: Get current PlayerState<br/>(score: 50, streak: 4)
    
    SM->>SC: calculateScore(50, 4, true, 10)
    SC->>SC: newStreak = 4 + 1 = 5
    SC->>SC: multiplier = min(1.0 + (5 * 0.1), 3.0) = 1.5
    SC->>SC: pointsAwarded = floor(10 * 1.5) = 15
    SC->>SC: newScore = 50 + 15 = 65
    SC-->>SM: ScoreResult(65, 5, 15, 1.5)
    
    SM->>SM: Update PlayerState<br/>(score: 65, streak: 5)
    
    par Parallel Updates
        SM->>LS: updateLeaderboard(sessionId, players)
        LS->>LS: Sort by score DESC, name ASC
        LS->>LS: Assign ranks (handle ties)
        LS-->>SM: Leaderboard
        
        SM->>MB: broadcastScoreUpdate(sessionId, scoreUpdateMsg)
        MB->>AllClients: WebSocket: SCORE_UPDATE<br/>{studentId, newScore: 65, newStreak: 5, pointsAwarded: 15, multiplier: 1.5}
        
        SM->>MB: broadcastLeaderboardUpdate(sessionId, leaderboardMsg)
        MB->>AllClients: WebSocket: LEADERBOARD_UPDATE<br/>{rankings: [...]}
    end
    
    SM-->>API: Success(ScoreUpdate)
    API-->>Client: 200 OK<br/>{newScore: 65, newStreak: 5, pointsAwarded: 15, multiplier: 1.5}
```

---

## 3. Score Update Flow (Incorrect Answer)

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant SC as Score Calculator
    participant LS as Leaderboard Service
    participant MB as Message Broadcaster
    participant AllClients as All Session Clients

    Client->>API: POST /sessions/{sessionId}/answers<br/>{studentId, isCorrect: false, basePoints: 10}
    API->>SM: submitAnswer(sessionId, studentId, false, 10)
    
    SM->>SM: Validate session is ACTIVE
    SM->>SM: Get current PlayerState<br/>(score: 65, streak: 5)
    
    SM->>SC: calculateScore(65, 5, false, 10)
    SC->>SC: Incorrect answer detected
    SC->>SC: newScore = 65 (unchanged)
    SC->>SC: newStreak = 0 (reset)
    SC->>SC: pointsAwarded = 0
    SC-->>SM: ScoreResult(65, 0, 0, 0.0)
    
    SM->>SM: Update PlayerState<br/>(score: 65, streak: 0)
    
    par Parallel Updates
        SM->>LS: updateLeaderboard(sessionId, players)
        Note over LS: Rankings may not change<br/>if score unchanged
        LS-->>SM: Leaderboard
        
        SM->>MB: broadcastScoreUpdate(sessionId, scoreUpdateMsg)
        MB->>AllClients: WebSocket: SCORE_UPDATE<br/>{studentId, newScore: 65, newStreak: 0, pointsAwarded: 0, multiplier: 0.0}
    end
    
    SM-->>API: Success(ScoreUpdate)
    API-->>Client: 200 OK<br/>{newScore: 65, newStreak: 0, pointsAwarded: 0, multiplier: 0.0}
```

---

## 4. Session End Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant LS as Leaderboard Service
    participant PL as Persistence Layer
    participant DB as Database
    participant MB as Message Broadcaster
    participant AllClients as All Session Clients

    Client->>API: POST /sessions/{sessionId}/end
    API->>SM: endSession(sessionId)
    
    SM->>SM: Validate session exists
    SM->>SM: Check session not already ended
    
    SM->>SM: Change status to ENDED
    SM->>SM: Set endTime = now()
    
    SM->>LS: updateLeaderboard(sessionId, players)
    LS->>LS: Generate final rankings
    LS-->>SM: Final Leaderboard
    
    SM->>PL: saveSessionResults(session)
    
    loop Retry up to 3 times
        PL->>PL: Validate session data
        PL->>DB: INSERT session and players
        
        alt Success
            DB-->>PL: Success
            PL-->>SM: Success
        else Failure
            DB-->>PL: Error
            PL->>PL: Wait (exponential backoff)
            Note over PL: Retry attempt
        end
    end
    
    alt Persistence Success
        SM->>MB: broadcastSessionEnded(sessionId, finalLeaderboard)
        MB->>AllClients: WebSocket: SESSION_ENDED<br/>{sessionId, finalLeaderboard}
        
        SM-->>API: Success(SessionSummary)
        API-->>Client: 200 OK<br/>{sessionId, endTime, playerCount, finalLeaderboard}
        
    else Persistence Failed After Retries
        SM-->>API: Error(PERSISTENCE_FAILED)
        API-->>Client: 500 Internal Server Error<br/>{error: "Failed to save session"}
    end
```

---

## 5. Session Creation Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager

    Client->>API: POST /sessions
    API->>SM: createSession()
    
    SM->>SM: Generate unique SessionId (UUID)
    SM->>SM: Create Session object<br/>(status: ACTIVE, startTime: now())
    SM->>SM: Initialize empty players map
    SM->>SM: Store session in memory
    
    SM-->>API: SessionId
    API-->>Client: 201 Created<br/>{sessionId, status: "ACTIVE", startTime}
```

---

## 6. Get Leaderboard Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant LS as Leaderboard Service

    Client->>API: GET /sessions/{sessionId}/leaderboard
    API->>SM: getSessionState(sessionId)
    
    SM->>SM: Validate session exists
    SM->>SM: Get all players from session
    
    SM->>LS: updateLeaderboard(sessionId, players)
    LS->>LS: Sort players by score DESC, name ASC
    LS->>LS: Assign ranks (same score = same rank)
    LS->>LS: Build LeaderboardEntry list
    LS-->>SM: Leaderboard
    
    SM-->>API: Leaderboard
    API-->>Client: 200 OK<br/>{sessionId, rankings: [{rank, studentId, name, score}, ...]}
```

---

## 7. WebSocket Connection Flow

```mermaid
sequenceDiagram
    participant Client
    participant WS as WebSocket Handler
    participant SM as Session Manager
    participant MB as Message Broadcaster

    Client->>WS: Connect ws://host/ws/sessions/{sessionId}
    WS->>WS: Authenticate connection
    
    alt Authentication Success
        WS->>SM: Validate session exists and is ACTIVE
        SM-->>WS: Session valid
        
        WS->>MB: Register client for session
        MB->>MB: Add client to session broadcast list
        
        WS-->>Client: Connection established
        WS->>Client: Send current session state
        
        loop While connected
            Note over MB,Client: Real-time updates flow
            MB->>Client: SCORE_UPDATE / LEADERBOARD_UPDATE
        end
        
    else Authentication Failed
        WS-->>Client: 401 Unauthorized
        WS->>Client: Close connection
    end
```

---

## 8. Error Handling Flow - Database Timeout

```mermaid
sequenceDiagram
    participant Client
    participant API as API Layer
    participant SM as Session Manager
    participant SDC as Student Database Client
    participant SDB as Student Database

    Client->>API: POST /sessions/{sessionId}/players<br/>{studentId: "STU123456"}
    API->>SM: registerPlayer(sessionId, studentId)
    
    SM->>SDC: getStudentInfo(studentId)
    SDC->>SDB: GET /students/STU123456<br/>(timeout: 2000ms)
    
    Note over SDB: Database slow/unresponsive
    
    SDC->>SDC: Wait 2000ms
    SDC->>SDC: Timeout detected
    
    SDC-->>SM: Error(DATABASE_UNAVAILABLE)
    SM->>SM: Log error
    SM->>SM: Maintain session state (no changes)
    
    SM-->>API: Error(DATABASE_UNAVAILABLE)
    API-->>Client: 503 Service Unavailable<br/>{error: "Student database unavailable", code: "DATABASE_UNAVAILABLE"}
```

---

## 9. Error Handling Flow - Network Failure During Broadcast

```mermaid
sequenceDiagram
    participant SM as Session Manager
    participant MB as Message Broadcaster
    participant Client1 as Client 1
    participant Client2 as Client 2 (Disconnected)
    participant Client3 as Client 3

    SM->>MB: broadcastScoreUpdate(sessionId, message)
    
    MB->>Client1: WebSocket: SCORE_UPDATE
    Client1-->>MB: ACK
    
    MB->>Client2: WebSocket: SCORE_UPDATE
    Note over Client2: Network error / Disconnected
    Client2--xMB: Connection failed
    
    MB->>MB: Queue message for Client2
    MB->>MB: Schedule retry with backoff
    
    MB->>Client3: WebSocket: SCORE_UPDATE
    Client3-->>MB: ACK
    
    loop Retry for Client2
        MB->>MB: Wait (exponential backoff)
        MB->>Client2: Retry WebSocket: SCORE_UPDATE
        
        alt Reconnected
            Client2-->>MB: ACK
            MB->>MB: Remove from retry queue
        else Still Disconnected
            MB->>MB: Continue retry cycle
        end
    end
```

---

## 10. Complete Quiz Session Flow (End-to-End)

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant P1 as Player 1
    participant P2 as Player 2
    participant API as API Layer
    participant SM as Session Manager
    participant SC as Score Calculator
    participant LS as Leaderboard Service
    participant PL as Persistence Layer

    Admin->>API: Create Session
    API->>SM: createSession()
    SM-->>API: SessionId
    API-->>Admin: Session created

    P1->>API: Register (STU001)
    API->>SM: registerPlayer(sessionId, STU001)
    SM-->>API: PlayerState (Alice, score: 0)
    API-->>P1: Registered

    P2->>API: Register (STU002)
    API->>SM: registerPlayer(sessionId, STU002)
    SM-->>API: PlayerState (Bob, score: 0)
    API-->>P2: Registered

    Note over P1,P2: Quiz begins

    P1->>API: Submit correct answer (10 pts)
    API->>SM: submitAnswer(STU001, true, 10)
    SM->>SC: calculateScore(0, 0, true, 10)
    SC-->>SM: (score: 10, streak: 1, multiplier: 1.1)
    SM->>LS: Update leaderboard
    LS-->>SM: Rankings: [Alice: 10, Bob: 0]
    SM-->>API: ScoreUpdate
    API-->>P1: Score: 10
    Note over P1,P2: Broadcast to all: Alice leads

    P2->>API: Submit correct answer (10 pts)
    API->>SM: submitAnswer(STU002, true, 10)
    SM->>SC: calculateScore(0, 0, true, 10)
    SC-->>SM: (score: 10, streak: 1, multiplier: 1.1)
    SM->>LS: Update leaderboard
    LS-->>SM: Rankings: [Alice: 10, Bob: 10] (tie)
    SM-->>API: ScoreUpdate
    API-->>P2: Score: 10
    Note over P1,P2: Broadcast: Tied at 10

    P1->>API: Submit correct answer (10 pts)
    API->>SM: submitAnswer(STU001, true, 10)
    SM->>SC: calculateScore(10, 1, true, 10)
    SC-->>SM: (score: 22, streak: 2, multiplier: 1.2)
    SM->>LS: Update leaderboard
    LS-->>SM: Rankings: [Alice: 22, Bob: 10]
    SM-->>API: ScoreUpdate
    API-->>P1: Score: 22
    Note over P1,P2: Broadcast: Alice leads 22-10

    P2->>API: Submit incorrect answer
    API->>SM: submitAnswer(STU002, false, 10)
    SM->>SC: calculateScore(10, 1, false, 10)
    SC-->>SM: (score: 10, streak: 0, multiplier: 0.0)
    SM-->>API: ScoreUpdate
    API-->>P2: Score: 10 (streak reset)

    Note over P1,P2: Quiz ends

    Admin->>API: End Session
    API->>SM: endSession(sessionId)
    SM->>LS: Generate final leaderboard
    LS-->>SM: Final Rankings: [Alice: 22, Bob: 10]
    SM->>PL: saveSessionResults(session)
    PL-->>SM: Success
    SM-->>API: SessionSummary
    API-->>Admin: Session ended
    Note over P1,P2: Broadcast: SESSION_ENDED with final results
```

---

## Notes

### Timing Constraints

- **Score updates**: Must complete within 100ms from submission to broadcast
- **Database queries**: 2-second timeout for student database lookups
- **Persistence retries**: Exponential backoff (100ms, 200ms, 400ms) with max 5s wait

### Concurrency Considerations

- Multiple players can submit answers simultaneously
- Session Manager must handle concurrent score updates atomically
- Leaderboard updates are triggered after each score change
- Message broadcasting happens asynchronously to avoid blocking

### Error Recovery

- Student database failures prevent registration but don't crash the system
- Network failures during broadcast trigger message queuing and retry
- Persistence failures are retried up to 3 times before reporting error
- Score calculation errors maintain previous valid state

### Real-Time Communication

- WebSocket connections are maintained per client per session
- All clients in a session receive score and leaderboard updates
- Disconnected clients are removed from broadcast list
- Reconnecting clients receive current session state
