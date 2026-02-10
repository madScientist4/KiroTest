# Flow Steps - Table Format

## Overview

This document presents all system flows in a structured table format for easy reference and implementation.

---

## 1. Player Registration Flow

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Initiate registration | POST `/sessions/{sessionId}/players`<br/>Body: `{studentId: "STU123456"}` | HTTP Request |
| 2 | API Layer | Validate request | Extract sessionId and studentId | Call Session Manager |
| 3 | Session Manager | Validate session | Check session exists and status = ACTIVE | Continue or Error |
| 4 | Session Manager | Check duplicates | Search players map for studentId | Continue or DUPLICATE_PLAYER |
| 5 | Student DB Client | Validate format | Check pattern: `^[a-zA-Z0-9-]{6,12}$` | Continue or INVALID_INPUT |
| 6 | Student DB Client | Query database | GET `{baseUrl}/students/{studentId}`<br/>Timeout: 2000ms | HTTP Response |
| 7a | Student DB Client | Handle success | Parse JSON: `{studentId, name}` | StudentInfo |
| 7b | Student DB Client | Handle 404 | Student not found | STUDENT_NOT_FOUND error |
| 7c | Student DB Client | Handle timeout | Database unavailable | DATABASE_UNAVAILABLE error |
| 8 | Session Manager | Create PlayerState | Initialize: score=0, streak=0, name from DB | PlayerState object |
| 9 | Session Manager | Add to session | Insert into players map | Updated session |
| 10 | Message Broadcaster | Notify clients | Broadcast PLAYER_JOINED event | WebSocket messages |
| 11 | API Layer | Return success | 201 Created<br/>Body: `{studentId, name, score: 0, streak: 0}` | HTTP Response |

**Preconditions:** Session exists and is ACTIVE, Student ID not already registered  
**Postconditions:** Player registered with score=0, streak=0, All clients notified  
**Errors:** INVALID_INPUT, SESSION_NOT_FOUND, SESSION_ENDED, DUPLICATE_PLAYER, STUDENT_NOT_FOUND, DATABASE_UNAVAILABLE

---

## 2. Score Update Flow (Correct Answer)

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Submit answer | POST `/sessions/{sessionId}/answers`<br/>Body: `{studentId, isCorrect: true, basePoints: 10}` | HTTP Request |
| 2 | API Layer | Route request | Extract parameters | Call Session Manager |
| 3 | Session Manager | Validate session | Check exists and ACTIVE | Continue or Error |
| 4 | Session Manager | Validate player | Check player registered | Continue or PLAYER_NOT_FOUND |
| 5 | Session Manager | Get current state | Retrieve PlayerState<br/>Example: `{score: 50, streak: 4}` | Current state |
| 6 | Score Calculator | Calculate new streak | `newStreak = currentStreak + 1 = 5` | newStreak: 5 |
| 7 | Score Calculator | Calculate multiplier | `min(1.0 + (5 * 0.1), 3.0) = 1.5` | multiplier: 1.5 |
| 8 | Score Calculator | Calculate points | `floor(10 * 1.5) = 15` | pointsAwarded: 15 |
| 9 | Score Calculator | Calculate new score | `50 + 15 = 65` | newScore: 65 |
| 10 | Score Calculator | Return result | ScoreResult object | `{65, 5, 15, 1.5}` |
| 11 | Session Manager | Update state | Set score=65, streak=5 | Updated PlayerState |
| 12a | Leaderboard Service | Update rankings | Sort by score DESC, name ASC<br/>Assign ranks | Leaderboard object |
| 12b | Message Broadcaster | Broadcast score | Send SCORE_UPDATE to all clients | WebSocket messages |
| 12c | Message Broadcaster | Broadcast leaderboard | Send LEADERBOARD_UPDATE to all clients | WebSocket messages |
| 13 | API Layer | Return success | 200 OK<br/>Body: `{newScore: 65, newStreak: 5, pointsAwarded: 15, multiplierApplied: 1.5}` | HTTP Response |

**Preconditions:** Session ACTIVE, Player registered, Base points > 0  
**Postconditions:** Score increased, Streak incremented, All clients updated within 100ms  
**Performance:** Total flow < 100ms

---

## 3. Score Update Flow (Incorrect Answer)

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Submit answer | POST `/sessions/{sessionId}/answers`<br/>Body: `{studentId, isCorrect: false, basePoints: 10}` | HTTP Request |
| 2 | API Layer | Route request | Extract parameters | Call Session Manager |
| 3 | Session Manager | Validate | Check session ACTIVE and player registered | Continue or Error |
| 4 | Session Manager | Get current state | Retrieve PlayerState<br/>Example: `{score: 65, streak: 5}` | Current state |
| 5 | Score Calculator | Detect incorrect | isCorrect = false | Process as incorrect |
| 6 | Score Calculator | Maintain score | newScore = 65 (unchanged) | newScore: 65 |
| 7 | Score Calculator | Reset streak | newStreak = 0 | newStreak: 0 |
| 8 | Score Calculator | No points | pointsAwarded = 0, multiplier = 0.0 | ScoreResult |
| 9 | Session Manager | Update state | Set score=65, streak=0 | Updated PlayerState |
| 10 | Message Broadcaster | Broadcast update | Send SCORE_UPDATE (streak reset) | WebSocket messages |
| 11 | API Layer | Return success | 200 OK<br/>Body: `{newScore: 65, newStreak: 0, pointsAwarded: 0, multiplierApplied: 0.0}` | HTTP Response |

**Preconditions:** Session ACTIVE, Player registered  
**Postconditions:** Score unchanged, Streak reset to 0, Clients notified  
**Key Invariant:** Score never decreases

---

## 4. Session End Flow

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Administrator | End session | POST `/sessions/{sessionId}/end`<br/>Body: `{}` | HTTP Request |
| 2 | API Layer | Route request | Extract sessionId | Call Session Manager |
| 3 | Session Manager | Validate session | Check exists and not already ENDED | Continue or Error |
| 4 | Session Manager | Change status | Set status = ENDED<br/>Set endTime = now() | Updated session |
| 5 | Leaderboard Service | Generate final | Sort all players, assign final ranks | Final Leaderboard |
| 6 | Persistence Layer | Begin save | Call saveSessionResults(session) | Start retry loop |
| 7a | Persistence Layer | Attempt 1 | Validate data, store to DB<br/>Wait: 0ms | Success or retry |
| 7b | Persistence Layer | Attempt 2 | If failed: wait 100ms, retry | Success or retry |
| 7c | Persistence Layer | Attempt 3 | If failed: wait 200ms, retry | Success or retry |
| 7d | Persistence Layer | Attempt 4 | If failed: wait 400ms, final attempt | Success or Error |
| 8a | Persistence Layer | Success path | Return Success | Continue |
| 8b | Persistence Layer | Failure path | Return PERSISTENCE_FAILED | Return 500 error |
| 9 | Message Broadcaster | Notify clients | Broadcast SESSION_ENDED with final leaderboard | WebSocket messages |
| 10 | Session Manager | Create summary | Build SessionSummary object | Summary data |
| 11 | API Layer | Return success | 200 OK<br/>Body: `{sessionId, endTime, playerCount, finalLeaderboard}` | HTTP Response |

**Preconditions:** Session exists and not already ended  
**Postconditions:** Session ENDED, Data persisted, All clients notified, No further updates allowed  
**Retry Logic:** Max 3 retries with exponential backoff (100ms, 200ms, 400ms)


---

## 5. Session Creation Flow

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Request new session | POST `/sessions`<br/>Body: `{}` | HTTP Request |
| 2 | API Layer | Route request | No parameters needed | Call Session Manager |
| 3 | Session Manager | Generate ID | Create UUID v4 (cryptographically random) | SessionId |
| 4 | Session Manager | Create Session | Set: id, status=ACTIVE, startTime=now(), endTime=null, players={} | Session object |
| 5 | Session Manager | Store in memory | Add to session store | Session available |
| 6 | Session Manager | Return ID | Return SessionId | SessionId |
| 7 | API Layer | Return success | 201 Created<br/>Body: `{sessionId, status: "ACTIVE", startTime}` | HTTP Response |

**Preconditions:** None (any authorized client can create)  
**Postconditions:** New ACTIVE session with unique ID, Empty player list, Ready for registrations  
**Session ID:** UUID v4 format, guaranteed unique

---

## 6. Get Leaderboard Flow

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Request leaderboard | GET `/sessions/{sessionId}/leaderboard` | HTTP Request |
| 2 | API Layer | Route request | Extract sessionId | Call Session Manager |
| 3 | Session Manager | Validate session | Check session exists | Continue or SESSION_NOT_FOUND |
| 4 | Session Manager | Get players | Retrieve all players from session | Players map |
| 5 | Leaderboard Service | Sort players | Primary: score DESC<br/>Secondary: name ASC | Sorted list |
| 6 | Leaderboard Service | Assign ranks | Iterate, assign ranks<br/>Same score = same rank | Ranked entries |
| 7 | Leaderboard Service | Build entries | Create LeaderboardEntry for each player | Entry list |
| 8 | Leaderboard Service | Return leaderboard | Leaderboard object with rankings | Leaderboard |
| 9 | API Layer | Return success | 200 OK<br/>Body: `{sessionId, rankings: [{rank, studentId, name, score}, ...]}` | HTTP Response |

**Preconditions:** Session exists  
**Postconditions:** Client receives current rankings, No state changes (read-only)  
**Ranking Rules:** Score DESC → Name ASC, Ties get same rank

---

## 7. WebSocket Connection Flow

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Initiate connection | Connect to `ws://host/ws/sessions/{sessionId}`<br/>Include auth token in headers | WebSocket request |
| 2 | WebSocket Handler | Receive request | Extract sessionId and auth token | Connection attempt |
| 3 | WebSocket Handler | Authenticate | Validate token, check expiry | Continue or 401 |
| 4 | WebSocket Handler | Validate session | Check session exists and ACTIVE | Continue or close |
| 5 | Message Broadcaster | Register client | Add to session's broadcast list | Client subscribed |
| 6 | WebSocket Handler | Confirm connection | Send connection established message | Connection upgraded |
| 7 | WebSocket Handler | Send current state | Retrieve and send session state | Initial sync |
| 8 | System | Maintain connection | Keep connection open for updates | Active connection |

**Preconditions:** Session exists and ACTIVE, Valid authentication  
**Postconditions:** Client connected, Registered for updates, Synchronized with current state  
**Message Types:** SCORE_UPDATE, LEADERBOARD_UPDATE, SESSION_ENDED

---

## 8. Error Handling - Database Timeout

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Client | Attempt registration | POST `/sessions/{sessionId}/players`<br/>Body: `{studentId: "STU123456"}` | HTTP Request |
| 2 | Session Manager | Validate | Session and duplicate checks pass | Continue |
| 3 | Student DB Client | Initiate query | GET `{baseUrl}/students/STU123456`<br/>Set timeout: 2000ms | HTTP request sent |
| 4 | Student Database | No response | Database slow/unresponsive | Waiting... |
| 5 | Student DB Client | Timeout occurs | 2000ms elapsed, no response | Timeout detected |
| 6 | Student DB Client | Cancel request | Abort HTTP request | Request cancelled |
| 7 | Student DB Client | Return error | Create Error(DATABASE_UNAVAILABLE) | Error object |
| 8 | Session Manager | Log error | Log: timestamp, sessionId, studentId, error | Error logged |
| 9 | Session Manager | Maintain state | No changes to session | State unchanged |
| 10 | API Layer | Format error | Create error response | Error response |
| 11 | Client | Receive error | 503 Service Unavailable<br/>Body: `{error: "Student database unavailable", code: "DATABASE_UNAVAILABLE"}` | HTTP Response |

**Preconditions:** Session ACTIVE, Student Database slow/unavailable  
**Postconditions:** Registration failed, Session unchanged, Error logged, Client informed  
**Timeout:** 2000ms (2 seconds), No retry (fail fast)

---

## 9. Error Handling - Network Failure During Broadcast

| Step | Actor | Action | Details | Output |
|------|-------|--------|---------|--------|
| 1 | Session Manager | Trigger broadcast | Score update completed | Call Message Broadcaster |
| 2 | Message Broadcaster | Get client list | Retrieve all WebSocket connections for session | Client list |
| 3 | Message Broadcaster | Send to Client 1 | Send SCORE_UPDATE via WebSocket | Success, ACK received |
| 4 | Message Broadcaster | Send to Client 2 | Attempt send | Network error detected |
| 5 | Message Broadcaster | Detect failure | Catch network exception | Failure logged |
| 6 | Message Broadcaster | Queue message | Create retry entry: message, clientId, attemptCount=1 | Message queued |
| 7 | Message Broadcaster | Schedule retry | Calculate backoff: 100ms<br/>Set nextRetryTime | Retry scheduled |
| 8 | Message Broadcaster | Send to Client 3 | Send SCORE_UPDATE via WebSocket | Success, ACK received |
| 9a | Message Broadcaster | Retry 1 (100ms) | Wait 100ms, attempt send | Success or queue again |
| 9b | Message Broadcaster | Retry 2 (200ms) | Wait 200ms, attempt send | Success or queue again |
| 9c | Message Broadcaster | Retry N | Continue with exponential backoff | Until success |
| 10 | Client 2 | Reconnect | Re-establish WebSocket | Connection restored |
| 11 | Message Broadcaster | Send queued | Deliver queued message | Message received |
| 12 | Message Broadcaster | Remove from queue | Message delivered successfully | Queue cleared |

**Preconditions:** Multiple clients connected, Network failure occurs  
**Postconditions:** All reachable clients receive update, Failed messages queued, No messages lost  
**Retry Strategy:** Exponential backoff (100ms, 200ms, 400ms, 800ms, max 5000ms), No retry limit


---

## 10. Complete Quiz Session Flow (End-to-End)

### Phase 1: Session Setup

| Step | Actor | Action | Details | State After |
|------|-------|--------|---------|-------------|
| 1 | Administrator | Create session | POST `/sessions` | Session: `{id: "abc-123", status: ACTIVE, players: {}}` |
| 2 | Alice | Register | POST with `{studentId: "STU001"}` | Alice: `{score: 0, streak: 0}` |
| 3 | Bob | Register | POST with `{studentId: "STU002"}` | Bob: `{score: 0, streak: 0}` |
| 4 | System | Initial leaderboard | Both at score 0 | Rankings: [Alice: 0, Bob: 0] |

### Phase 2: Quiz Questions

| Step | Player | Question | Correct? | Points | Calculation | New State | Leaderboard |
|------|--------|----------|----------|--------|-------------|-----------|-------------|
| 5 | Alice | Q1 | ✓ Yes | 10 | streak=1, mult=1.1<br/>points=11 | score: 11, streak: 1 | Alice: 11, Bob: 0 |
| 6 | Bob | Q1 | ✓ Yes | 10 | streak=1, mult=1.1<br/>points=11 | score: 11, streak: 1 | Alice: 11, Bob: 11 |
| 7 | Alice | Q2 | ✓ Yes | 10 | streak=2, mult=1.2<br/>points=12 | score: 23, streak: 2 | Alice: 23, Bob: 11 |
| 8 | Bob | Q2 | ✗ No | 10 | streak=0, mult=0.0<br/>points=0 | score: 11, streak: 0 | Alice: 23, Bob: 11 |
| 9 | Alice | Q3 | ✓ Yes | 10 | streak=3, mult=1.3<br/>points=13 | score: 36, streak: 3 | Alice: 36, Bob: 11 |
| 10 | Bob | Q3 | ✓ Yes | 10 | streak=1, mult=1.1<br/>points=11 | score: 22, streak: 1 | Alice: 36, Bob: 22 |

### Phase 3: Session End

| Step | Actor | Action | Details | Result |
|------|-------|--------|---------|--------|
| 11 | Administrator | End session | POST `/sessions/abc-123/end` | Status: ENDED, endTime: "10:15:00" |
| 12 | Leaderboard Service | Final rankings | Sort and rank | Rank 1: Alice (36), Rank 2: Bob (22) |
| 13 | Persistence Layer | Save results | Attempt 1: Success | Data persisted to database |
| 14 | Message Broadcaster | Notify clients | Broadcast SESSION_ENDED | All clients receive final results |
| 15 | Administrator | Receive confirmation | 200 OK response | Session summary returned |

### Final Results Summary

| Player | Final Score | Final Streak | Questions Correct | Questions Incorrect | Rank | Points Breakdown |
|--------|-------------|--------------|-------------------|---------------------|------|------------------|
| Alice | 36 | 3 | 3 | 0 | 1 | Q1: 11 (1.1x), Q2: 12 (1.2x), Q3: 13 (1.3x) |
| Bob | 22 | 1 | 2 | 1 | 2 | Q1: 11 (1.1x), Q2: 0 (miss), Q3: 11 (1.1x) |

**Session Duration:** 15 minutes (10:00:00 - 10:15:00)  
**Total Players:** 2  
**Status:** ENDED  
**Data:** Persisted successfully

---

## Summary Tables

### All System Flows Overview

| Flow # | Flow Name | Type | Primary Components | Performance Target |
|--------|-----------|------|-------------------|-------------------|
| 1 | Player Registration | Operational | API Layer, Session Manager, Student DB Client | < 2100ms (includes DB timeout) |
| 2 | Score Update (Correct) | Operational | Session Manager, Score Calculator, Leaderboard, Broadcaster | < 100ms |
| 3 | Score Update (Incorrect) | Operational | Session Manager, Score Calculator, Broadcaster | < 100ms |
| 4 | Session End | Operational | Session Manager, Persistence Layer, Broadcaster | < 5000ms (includes retries) |
| 5 | Session Creation | Operational | API Layer, Session Manager | < 50ms |
| 6 | Get Leaderboard | Query | Session Manager, Leaderboard Service | < 200ms |
| 7 | WebSocket Connection | Real-time | WebSocket Handler, Message Broadcaster | < 500ms |
| 8 | Database Timeout | Error Handling | Student DB Client | 2000ms timeout |
| 9 | Network Failure | Error Handling | Message Broadcaster | Retry until success |
| 10 | Complete Session | End-to-End | All components | 15 minutes (example) |

---

## Error Code Reference Table

| Error Code | HTTP Status | Trigger Condition | User Impact | Recovery Action | Retry Recommended |
|------------|-------------|-------------------|-------------|-----------------|-------------------|
| SESSION_NOT_FOUND | 404 | Session ID doesn't exist | Cannot perform operation | Verify session ID | No |
| SESSION_ENDED | 410 | Session already ended | Cannot modify session | View final results only | No |
| PLAYER_NOT_FOUND | 404 | Player not registered | Cannot submit answers | Register player first | No |
| DUPLICATE_PLAYER | 409 | Student already in session | Cannot register again | Use existing registration | No |
| STUDENT_NOT_FOUND | 404 | Student not in database | Cannot register | Verify student ID | No |
| DATABASE_UNAVAILABLE | 503 | DB timeout or network error | Cannot register players | Wait and retry | Yes (after delay) |
| INVALID_INPUT | 400 | Invalid request format/data | Request rejected | Fix request data | No |
| PERSISTENCE_FAILED | 500 | Storage failed after 3 retries | Session data not saved | Contact administrator | No (already retried) |

---

## Component Interaction Matrix

| Operation | API Layer | Session Manager | Score Calculator | Leaderboard Service | Message Broadcaster | Persistence Layer | Student DB Client |
|-----------|-----------|-----------------|------------------|---------------------|---------------------|-------------------|-------------------|
| Create Session | ✓ Route | ✓ Create | - | - | - | - | - |
| Register Player | ✓ Route | ✓ Validate | - | - | ✓ Notify | - | ✓ Query |
| Submit Answer (Correct) | ✓ Route | ✓ Orchestrate | ✓ Calculate | ✓ Update | ✓ Broadcast | - | - |
| Submit Answer (Incorrect) | ✓ Route | ✓ Orchestrate | ✓ Calculate | ✓ Update | ✓ Broadcast | - | - |
| End Session | ✓ Route | ✓ Finalize | - | ✓ Final Rank | ✓ Notify | ✓ Save | - |
| Get Leaderboard | ✓ Route | ✓ Retrieve | - | ✓ Generate | - | - | - |
| WebSocket Connect | ✓ Upgrade | ✓ Validate | - | - | ✓ Register | - | - |

**Legend:** ✓ = Component involved, - = Not involved

---

## Performance Characteristics Table

| Metric | Target | Measurement Point | Critical Path |
|--------|--------|-------------------|---------------|
| Score Update Latency | < 100ms | Client request → All clients notified | Yes |
| Database Query Timeout | 2000ms | HTTP request → Response or timeout | Yes |
| Broadcast Latency | < 50ms | Score update → WebSocket send | Yes |
| Leaderboard Generation | < 200ms | Request → Response (100 players) | No |
| Session Creation | < 50ms | Request → Response | No |
| Persistence Retry Backoff | 100ms, 200ms, 400ms | Between retry attempts | Yes |
| WebSocket Connection | < 500ms | Connect → Authenticated & Synced | No |

---

## State Transition Table

### Session States

| Current State | Event | Validation | Next State | Side Effects |
|---------------|-------|------------|------------|--------------|
| - (None) | Create Session | None | ACTIVE | Generate UUID, initialize empty players |
| ACTIVE | Register Player | Student exists, no duplicate | ACTIVE | Add player with score=0, streak=0 |
| ACTIVE | Submit Answer | Player registered | ACTIVE | Update score/streak, broadcast updates |
| ACTIVE | End Session | Has players | ENDED | Persist data, broadcast final results |
| ENDED | Any operation | - | ENDED | Reject with SESSION_ENDED error |

### Player States (per session)

| Current State | Event | Validation | Next State | Score Change | Streak Change |
|---------------|-------|------------|------------|--------------|---------------|
| Unregistered | Register | Student in DB | Registered | Set to 0 | Set to 0 |
| Registered | Correct Answer | Session ACTIVE | Registered | +points × multiplier | +1 |
| Registered | Incorrect Answer | Session ACTIVE | Registered | No change | Reset to 0 |
| Registered | Session End | - | Final | No change | No change |

### Streak States

| Current Streak | Event | Next Streak | Multiplier | Points Awarded (base=10) |
|----------------|-------|-------------|------------|--------------------------|
| 0 | Correct | 1 | 1.1 | 11 |
| 1 | Correct | 2 | 1.2 | 12 |
| 2 | Correct | 3 | 1.3 | 13 |
| 5 | Correct | 6 | 1.6 | 16 |
| 10 | Correct | 11 | 2.1 | 21 |
| 19 | Correct | 20 | 3.0 (max) | 30 |
| 20+ | Correct | 21+ | 3.0 (capped) | 30 |
| Any | Incorrect | 0 | 0.0 | 0 |


---

## Validation Rules Table

### Input Validation

| Field | Format | Min Length | Max Length | Pattern | Example Valid | Example Invalid |
|-------|--------|------------|------------|---------|---------------|-----------------|
| Student ID | Alphanumeric + hyphen | 6 | 12 | `^[a-zA-Z0-9-]{6,12}$` | STU123456 | STU@123, ABC |
| Session ID | UUID v4 | 36 | 36 | UUID format | 550e8400-e29b-41d4-a716-446655440000 | abc-123 |
| Base Points | Integer | 1 | - | Positive integer | 10, 100 | 0, -5, 3.14 |
| Player Name | String | 1 | 255 | Non-empty | John Doe | (empty) |
| Score | Integer | 0 | 2147483647 | Non-negative | 0, 150 | -10 |
| Streak | Integer | 0 | - | Non-negative | 0, 5 | -1 |

### Business Rule Validation

| Rule | Validation Check | Error if Violated | Impact |
|------|------------------|-------------------|--------|
| Session must exist | Check session in store | SESSION_NOT_FOUND | Operation rejected |
| Session must be ACTIVE | Check status = ACTIVE | SESSION_ENDED | Operation rejected |
| Player must be registered | Check studentId in players map | PLAYER_NOT_FOUND | Operation rejected |
| No duplicate players | Check studentId not in players map | DUPLICATE_PLAYER | Registration rejected |
| Student must exist in DB | Query returns 200 OK | STUDENT_NOT_FOUND | Registration rejected |
| Score must be non-negative | score >= 0 | INVALID_INPUT | Operation rejected |
| Multiplier must be 1.0-3.0 | 1.0 <= mult <= 3.0 | Calculation error | Score calculation fails |

---

## Data Persistence Table

### Session Record Schema

| Field | Type | Required | Source | Example | Notes |
|-------|------|----------|--------|---------|-------|
| sessionId | UUID | Yes | Generated on create | 550e8400-e29b-41d4-a716-446655440000 | Primary key |
| startTime | Timestamp | Yes | Set on create | 2026-02-09T10:00:00Z | ISO 8601 format |
| endTime | Timestamp | Yes | Set on end | 2026-02-09T10:15:00Z | Null until ended |
| status | Enum | Yes | ACTIVE → ENDED | ENDED | ACTIVE or ENDED |
| playerCount | Integer | No | Calculated | 2 | Derived from players |

### Player Record Schema

| Field | Type | Required | Source | Example | Notes |
|-------|------|----------|--------|---------|-------|
| sessionId | UUID | Yes | From session | 550e8400-e29b-41d4-a716-446655440000 | Foreign key |
| studentId | String | Yes | From registration | STU123456 | 6-12 chars |
| playerName | String | Yes | From Student DB | John Doe | 1-255 chars |
| finalScore | Integer | Yes | From PlayerState | 150 | Non-negative |
| finalStreak | Integer | No | From PlayerState | 5 | Optional field |

### Retry Configuration

| Attempt | Wait Before | Total Elapsed | Action on Failure |
|---------|-------------|---------------|-------------------|
| 1 | 0ms | 0ms | Retry with 100ms wait |
| 2 | 100ms | 100ms | Retry with 200ms wait |
| 3 | 200ms | 300ms | Retry with 400ms wait |
| 4 | 400ms | 700ms | Return PERSISTENCE_FAILED |

**Max Total Time:** 700ms  
**Max Retries:** 3  
**Backoff Strategy:** Exponential (100ms × 2^attempt)  
**Max Backoff:** 5000ms (not reached in 3 retries)

---

## WebSocket Message Types Table

### Client → Server Messages

| Message Type | Purpose | Required Fields | Example | Response |
|--------------|---------|-----------------|---------|----------|
| JOIN_SESSION | Subscribe to session updates | sessionId | `{type: "JOIN_SESSION", sessionId: "abc-123"}` | Connection confirmed + current state |
| HEARTBEAT | Keep connection alive | timestamp | `{type: "HEARTBEAT", timestamp: 1234567890}` | HEARTBEAT_ACK |
| DISCONNECT | Graceful disconnect | reason (optional) | `{type: "DISCONNECT", reason: "User left"}` | Connection closed |

### Server → Client Messages

| Message Type | Trigger | Required Fields | Example | Frequency |
|--------------|---------|-----------------|---------|-----------|
| SCORE_UPDATE | Player score changes | sessionId, studentId, newScore, newStreak, pointsAwarded, multiplierApplied | `{type: "SCORE_UPDATE", sessionId: "abc-123", studentId: "STU001", newScore: 65, newStreak: 5, pointsAwarded: 15, multiplierApplied: 1.5}` | Per answer submission |
| LEADERBOARD_UPDATE | Rankings change | sessionId, leaderboard | `{type: "LEADERBOARD_UPDATE", sessionId: "abc-123", leaderboard: {rankings: [...]}}` | When rankings change |
| SESSION_ENDED | Session ends | sessionId, finalLeaderboard | `{type: "SESSION_ENDED", sessionId: "abc-123", finalLeaderboard: {...}}` | Once per session end |
| PLAYER_JOINED | New player registers | sessionId, studentId, name | `{type: "PLAYER_JOINED", sessionId: "abc-123", studentId: "STU001", name: "Alice"}` | Per registration |
| ERROR | Error occurs | code, message | `{type: "ERROR", code: "SESSION_ENDED", message: "Session has ended"}` | On error conditions |
| HEARTBEAT_ACK | Response to heartbeat | timestamp | `{type: "HEARTBEAT_ACK", timestamp: 1234567890}` | Per heartbeat |

---

## Score Calculation Examples Table

| Current Score | Current Streak | Answer | Base Points | New Streak | Multiplier | Points Awarded | New Score | Calculation |
|---------------|----------------|--------|-------------|------------|------------|----------------|-----------|-------------|
| 0 | 0 | Correct | 10 | 1 | 1.1 | 11 | 11 | floor(10 × 1.1) = 11 |
| 11 | 1 | Correct | 10 | 2 | 1.2 | 12 | 23 | floor(10 × 1.2) = 12 |
| 23 | 2 | Correct | 10 | 3 | 1.3 | 13 | 36 | floor(10 × 1.3) = 13 |
| 36 | 3 | Incorrect | 10 | 0 | 0.0 | 0 | 36 | No points, streak reset |
| 36 | 0 | Correct | 10 | 1 | 1.1 | 11 | 47 | floor(10 × 1.1) = 11 |
| 100 | 10 | Correct | 10 | 11 | 2.1 | 21 | 121 | floor(10 × 2.1) = 21 |
| 200 | 19 | Correct | 10 | 20 | 3.0 | 30 | 230 | floor(10 × 3.0) = 30 (max) |
| 230 | 20 | Correct | 10 | 21 | 3.0 | 30 | 260 | floor(10 × 3.0) = 30 (capped) |
| 50 | 5 | Correct | 25 | 6 | 1.6 | 40 | 90 | floor(25 × 1.6) = 40 |
| 75 | 8 | Incorrect | 50 | 0 | 0.0 | 0 | 75 | No points, streak reset |

**Formula:** `multiplier = min(1.0 + (newStreak × 0.1), 3.0)`  
**Points:** `floor(basePoints × multiplier)`  
**Max Multiplier:** 3.0 (reached at streak 20)

---

## Leaderboard Ranking Examples Table

### Example 1: No Ties

| Player | Score | Name | Sort Order | Rank |
|--------|-------|------|------------|------|
| Alice | 150 | Alice | 1 (highest score) | 1 |
| Bob | 140 | Bob | 2 | 2 |
| Charlie | 130 | Charlie | 3 | 3 |
| Diana | 120 | Diana | 4 (lowest score) | 4 |

### Example 2: Two-Way Tie

| Player | Score | Name | Sort Order | Rank |
|--------|-------|------|------------|------|
| Alice | 150 | Alice | 1 (highest score) | 1 |
| Bob | 140 | Bob | 2 (tied, alphabetical first) | 2 |
| Charlie | 140 | Charlie | 3 (tied, alphabetical second) | 2 |
| Diana | 120 | Diana | 4 (lowest score) | 4 |

**Note:** Bob and Charlie both rank 2, next rank is 4 (not 3)

### Example 3: Three-Way Tie

| Player | Score | Name | Sort Order | Rank |
|--------|-------|------|------------|------|
| Alice | 150 | Alice | 1 (highest score) | 1 |
| Bob | 140 | Bob | 2 (tied, alphabetical: B) | 2 |
| Charlie | 140 | Charlie | 3 (tied, alphabetical: C) | 2 |
| Diana | 140 | Diana | 4 (tied, alphabetical: D) | 2 |
| Eve | 120 | Eve | 5 (lowest score) | 5 |

**Note:** Bob, Charlie, Diana all rank 2, next rank is 5 (not 3 or 4)

### Example 4: All Tied

| Player | Score | Name | Sort Order | Rank |
|--------|-------|------|------------|------|
| Alice | 100 | Alice | 1 (alphabetical: A) | 1 |
| Bob | 100 | Bob | 2 (alphabetical: B) | 1 |
| Charlie | 100 | Charlie | 3 (alphabetical: C) | 1 |

**Note:** All players rank 1 (tied for first place)

---

## End of Flow Steps - Table Format
