# Flow Steps Documentation

## Overview

This document provides detailed step-by-step descriptions of each flow in the scoring system, complementing the sequence diagrams.

---

## 1. Player Registration Flow

### Purpose
Register a new player in an active session by validating their student ID against the external student database.

### Preconditions
- Session must exist and be in ACTIVE status
- Student ID must not already be registered in the session

### Steps

1. **Client initiates registration**
   - Client sends POST request to `/sessions/{sessionId}/players`
   - Request body contains: `{studentId: "STU123456"}`

2. **API Layer receives request**
   - API Layer validates request format
   - Extracts sessionId from URL path
   - Extracts studentId from request body
   - Calls Session Manager's `registerPlayer(sessionId, studentId)`

3. **Session Manager validates session**
   - Checks if session exists in memory
   - Verifies session status is ACTIVE (not ENDED)
   - If session not found or ended, returns error

4. **Session Manager checks for duplicates**
   - Searches players map for existing studentId
   - If studentId already exists, returns DUPLICATE_PLAYER error

5. **Student Database Client validates format**
   - Checks studentId matches pattern: `^[a-zA-Z0-9-]{6,12}$`
   - If invalid format, returns INVALID_INPUT error immediately


6. **Student Database Client queries external database**
   - Makes HTTP GET request to `{baseUrl}/students/{studentId}`
   - Sets timeout to 2000ms
   - Waits for response

7. **Handle Student Database response**
   
   **Success Path (200 OK):**
   - Receives JSON: `{studentId: "STU123456", name: "John Doe"}`
   - Validates both studentId and name fields are present
   - Returns Success(StudentInfo) to Session Manager
   
   **Student Not Found (404):**
   - Returns Error(STUDENT_NOT_FOUND) to Session Manager
   - Session Manager returns error to API Layer
   - API Layer returns 404 to client
   
   **Database Unavailable (Timeout/Network Error):**
   - Timeout occurs after 2000ms
   - Returns Error(DATABASE_UNAVAILABLE) to Session Manager
   - Session Manager returns error to API Layer
   - API Layer returns 503 to client

8. **Session Manager creates PlayerState**
   - Creates new PlayerState object
   - Sets studentId from request
   - Sets name from Student Database response
   - Initializes score to 0
   - Initializes streak to 0

9. **Session Manager adds player to session**
   - Adds PlayerState to session's players map
   - Key: studentId, Value: PlayerState

10. **Message Broadcaster notifies all clients**
    - Broadcasts PLAYER_JOINED event via WebSocket
    - All connected clients in session receive notification

11. **Return success to client**
    - Session Manager returns Success(PlayerState) to API Layer
    - API Layer formats response
    - Returns 201 Created with player data: `{studentId, name, score: 0, streak: 0}`

### Postconditions
- Player is registered in session with score=0, streak=0
- All session clients notified of new player
- Player can now submit answers

### Error Scenarios
- **INVALID_INPUT**: Student ID format invalid
- **SESSION_NOT_FOUND**: Session doesn't exist
- **SESSION_ENDED**: Session already ended
- **DUPLICATE_PLAYER**: Student ID already registered
- **STUDENT_NOT_FOUND**: Student not in database
- **DATABASE_UNAVAILABLE**: Database timeout or network error


---

## 2. Score Update Flow (Correct Answer)

### Purpose
Process a correct answer submission, calculate points with streak multiplier, update player state, and broadcast changes to all clients.

### Preconditions
- Session must exist and be ACTIVE
- Player must be registered in the session
- Base points must be positive integer

### Steps

1. **Client submits answer**
   - Client sends POST to `/sessions/{sessionId}/answers`
   - Request body: `{studentId: "STU123456", isCorrect: true, basePoints: 10}`

2. **API Layer validates and routes**
   - Validates request format
   - Extracts sessionId, studentId, isCorrect, basePoints
   - Calls Session Manager's `submitAnswer(sessionId, studentId, true, 10)`

3. **Session Manager validates session**
   - Checks session exists
   - Verifies session status is ACTIVE
   - If session ended, returns SESSION_ENDED error

4. **Session Manager validates player**
   - Checks if studentId exists in session's players map
   - If not found, returns PLAYER_NOT_FOUND error

5. **Session Manager retrieves current state**
   - Gets PlayerState from players map
   - Example current state: `{score: 50, streak: 4}`

6. **Score Calculator computes new score**
   - Receives: currentScore=50, currentStreak=4, isCorrect=true, basePoints=10
   - Calculates newStreak: `4 + 1 = 5`
   - Calculates multiplier: `min(1.0 + (5 * 0.1), 3.0) = 1.5`
   - Calculates pointsAwarded: `floor(10 * 1.5) = 15`
   - Calculates newScore: `50 + 15 = 65`
   - Returns ScoreResult: `{newScore: 65, newStreak: 5, pointsAwarded: 15, multiplierApplied: 1.5}`

7. **Session Manager updates PlayerState**
   - Updates player's score to 65
   - Updates player's streak to 5
   - State is now: `{score: 65, streak: 5}`


8. **Parallel operations begin**

   **8a. Leaderboard Service updates rankings:**
   - Receives all players from session
   - Sorts by score descending, then name ascending
   - Assigns ranks (ties get same rank)
   - Returns updated Leaderboard object
   
   **8b. Message Broadcaster sends score update:**
   - Creates ScoreUpdateMessage with new score, streak, points, multiplier
   - Broadcasts to all WebSocket clients in session
   - Message: `{type: "SCORE_UPDATE", sessionId, studentId, newScore: 65, newStreak: 5, pointsAwarded: 15, multiplierApplied: 1.5}`
   
   **8c. Message Broadcaster sends leaderboard update:**
   - Creates LeaderboardUpdateMessage with new rankings
   - Broadcasts to all WebSocket clients in session
   - Message: `{type: "LEADERBOARD_UPDATE", sessionId, leaderboard: {rankings: [...]}}`

9. **Session Manager returns success**
   - Returns Success(ScoreUpdate) to API Layer
   - Contains: newScore, newStreak, pointsAwarded, multiplierApplied

10. **API Layer responds to client**
    - Formats response as JSON
    - Returns 200 OK with score update data

### Postconditions
- Player's score increased by calculated points
- Player's streak incremented by 1
- All session clients receive score and leaderboard updates
- Updates complete within 100ms

### Performance Requirements
- Total flow must complete in < 100ms
- Score calculation is pure function (no I/O)
- Broadcasting is asynchronous (non-blocking)

---

## 3. Score Update Flow (Incorrect Answer)

### Purpose
Process an incorrect answer submission, reset streak to zero, maintain current score, and broadcast changes.

### Preconditions
- Session must exist and be ACTIVE
- Player must be registered in the session

### Steps

1. **Client submits incorrect answer**
   - POST to `/sessions/{sessionId}/answers`
   - Request body: `{studentId: "STU123456", isCorrect: false, basePoints: 10}`

2. **API Layer routes to Session Manager**
   - Calls `submitAnswer(sessionId, studentId, false, 10)`

3. **Session Manager validates session and player**
   - Verifies session is ACTIVE
   - Verifies player is registered

4. **Session Manager retrieves current state**
   - Gets PlayerState: `{score: 65, streak: 5}`


5. **Score Calculator processes incorrect answer**
   - Receives: currentScore=65, currentStreak=5, isCorrect=false, basePoints=10
   - Detects incorrect answer
   - Sets newScore = 65 (unchanged)
   - Sets newStreak = 0 (reset)
   - Sets pointsAwarded = 0
   - Sets multiplierApplied = 0.0
   - Returns ScoreResult: `{newScore: 65, newStreak: 0, pointsAwarded: 0, multiplierApplied: 0.0}`

6. **Session Manager updates PlayerState**
   - Score remains 65 (unchanged)
   - Streak reset to 0
   - State is now: `{score: 65, streak: 0}`

7. **Parallel operations**
   
   **7a. Leaderboard Service updates:**
   - Rankings may not change if score unchanged
   - Still generates updated leaderboard for consistency
   
   **7b. Message Broadcaster sends updates:**
   - Broadcasts SCORE_UPDATE with streak reset
   - Message shows pointsAwarded: 0, multiplierApplied: 0.0
   - May skip leaderboard broadcast if rankings unchanged (optimization)

8. **Return success to client**
   - Returns 200 OK with: `{newScore: 65, newStreak: 0, pointsAwarded: 0, multiplierApplied: 0.0}`

### Postconditions
- Player's score unchanged
- Player's streak reset to 0
- All clients notified of streak reset
- Next correct answer will have multiplier 1.1 (streak of 1)

### Key Invariants
- Score never decreases
- Incorrect answer always resets streak to 0
- No points awarded for incorrect answers

---

## 4. Session End Flow

### Purpose
End an active session, persist all player data, and notify all clients with final results.

### Preconditions
- Session must exist
- Session must not already be ended

### Steps

1. **Administrator initiates session end**
   - Client sends POST to `/sessions/{sessionId}/end`
   - Empty request body: `{}`

2. **API Layer routes to Session Manager**
   - Calls `endSession(sessionId)`

3. **Session Manager validates session**
   - Checks session exists
   - Verifies session is not already ENDED
   - If already ended, returns SESSION_ALREADY_ENDED error


4. **Session Manager changes session status**
   - Sets session.status = ENDED
   - Sets session.endTime = current timestamp
   - Session now immutable (no further score updates allowed)

5. **Leaderboard Service generates final rankings**
   - Receives all players from session
   - Sorts by score descending, name ascending
   - Assigns final ranks
   - Returns final Leaderboard object

6. **Persistence Layer saves session results**
   - Session Manager calls `saveSessionResults(session)`
   - Persistence Layer begins retry loop

7. **Persistence retry loop (up to 3 attempts)**
   
   **Attempt 1:**
   - Validates session data (all required fields present)
   - Attempts to store to database
   - If success: returns Success, exit loop
   - If failure: wait 100ms (exponential backoff), continue to attempt 2
   
   **Attempt 2:**
   - Validates session data again
   - Attempts to store to database
   - If success: returns Success, exit loop
   - If failure: wait 200ms, continue to attempt 3
   
   **Attempt 3:**
   - Final validation
   - Final attempt to store
   - If success: returns Success, exit loop
   - If failure: returns Error(PERSISTENCE_FAILED)

8. **Handle persistence result**
   
   **Success Path:**
   - Persistence Layer returns Success
   - Session Manager proceeds to broadcast
   
   **Failure Path:**
   - Persistence Layer returns Error after 3 attempts
   - Session Manager returns error to API Layer
   - API Layer returns 500 Internal Server Error
   - Flow ends (no broadcast)

9. **Message Broadcaster notifies all clients (success path)**
   - Creates SessionEndedMessage
   - Includes sessionId and finalLeaderboard
   - Broadcasts to all WebSocket clients in session
   - Message: `{type: "SESSION_ENDED", sessionId, finalLeaderboard: {rankings: [...]}}`

10. **Session Manager returns summary**
    - Creates SessionSummary object
    - Includes: sessionId, endTime, playerCount, finalLeaderboard
    - Returns Success(SessionSummary) to API Layer

11. **API Layer responds to client**
    - Returns 200 OK with session summary


### Postconditions
- Session status is ENDED
- All player data persisted to database
- All clients notified of session end
- No further score updates accepted for this session

### Retry Logic Details
- **Exponential backoff**: 100ms, 200ms, 400ms (capped at 5000ms)
- **Max retries**: 3 attempts
- **Validation**: Performed before each storage attempt
- **Failure handling**: Error returned only after all retries exhausted

### Data Persisted
- Session ID (UUID)
- End timestamp
- For each player:
  - Student ID
  - Player name
  - Final score

---

## 5. Session Creation Flow

### Purpose
Create a new quiz/game session with a unique identifier.

### Preconditions
- None (any authorized client can create a session)

### Steps

1. **Client requests new session**
   - Client sends POST to `/sessions`
   - Empty request body: `{}`

2. **API Layer routes to Session Manager**
   - Calls `createSession()`

3. **Session Manager generates unique ID**
   - Generates UUID v4 (cryptographically random)
   - Creates SessionId object
   - Example: `"550e8400-e29b-41d4-a716-446655440000"`

4. **Session Manager creates Session object**
   - Sets id = generated SessionId
   - Sets status = ACTIVE
   - Sets startTime = current timestamp
   - Sets endTime = null
   - Initializes empty players map: `{}`

5. **Session Manager stores session in memory**
   - Adds session to in-memory session store
   - Session is now available for player registration

6. **Session Manager returns SessionId**
   - Returns SessionId to API Layer

7. **API Layer responds to client**
   - Returns 201 Created
   - Response body: `{sessionId, status: "ACTIVE", startTime}`

### Postconditions
- New session exists in ACTIVE state
- Session has unique ID
- Session ready to accept player registrations
- Empty player list

### Session ID Properties
- Format: UUID v4
- Guaranteed unique across all sessions
- Cryptographically random (secure)
- Used in all subsequent operations


---

## 6. Get Leaderboard Flow

### Purpose
Retrieve current leaderboard rankings for a session.

### Preconditions
- Session must exist

### Steps

1. **Client requests leaderboard**
   - Client sends GET to `/sessions/{sessionId}/leaderboard`
   - No request body

2. **API Layer routes to Session Manager**
   - Extracts sessionId from URL
   - Calls `getSessionState(sessionId)`

3. **Session Manager validates session**
   - Checks if session exists
   - If not found, returns SESSION_NOT_FOUND error

4. **Session Manager retrieves all players**
   - Gets players map from session
   - Example: `{STU001: {name: "Alice", score: 150, streak: 5}, STU002: {name: "Bob", score: 140, streak: 2}}`

5. **Leaderboard Service processes players**
   - Receives sessionId and players map
   - Begins ranking algorithm

6. **Leaderboard Service sorts players**
   - Primary sort: score descending (highest first)
   - Secondary sort: name ascending (alphabetical)
   - Example after sort: Alice (150), Bob (140)

7. **Leaderboard Service assigns ranks**
   - Iterates through sorted list
   - Assigns rank based on position
   - If scores equal, assigns same rank
   - Example: Rank 1: Alice (150), Rank 2: Bob (140)

8. **Leaderboard Service builds entry list**
   - Creates LeaderboardEntry for each player
   - Each entry contains: rank, studentId, name, score
   - Returns Leaderboard object with rankings list

9. **Session Manager returns leaderboard**
   - Returns Leaderboard to API Layer

10. **API Layer responds to client**
    - Returns 200 OK
    - Response body: `{sessionId, rankings: [{rank: 1, studentId: "STU001", name: "Alice", score: 150}, ...]}`

### Postconditions
- Client receives current rankings
- No state changes (read-only operation)

### Ranking Rules
- **Primary**: Score (descending)
- **Secondary**: Name (ascending, alphabetical)
- **Ties**: Same score = same rank
- **Example tie**: Alice 100, Bob 100, Charlie 90 → Ranks: 1, 1, 3 (not 1, 2, 3)


---

## 7. WebSocket Connection Flow

### Purpose
Establish real-time bidirectional communication between client and server for live updates.

### Preconditions
- Session must exist and be ACTIVE
- Client must have valid authentication credentials

### Steps

1. **Client initiates WebSocket connection**
   - Client connects to `ws://host:port/ws/sessions/{sessionId}`
   - Includes authentication token in connection headers

2. **WebSocket Handler receives connection request**
   - Extracts sessionId from URL path
   - Extracts authentication token from headers

3. **WebSocket Handler authenticates client**
   - Validates authentication token
   - Checks token is valid and not expired
   
   **If authentication fails:**
   - Returns 401 Unauthorized
   - Closes connection immediately
   - Flow ends

4. **WebSocket Handler validates session (success path)**
   - Calls Session Manager to check session exists
   - Verifies session status is ACTIVE
   - If session not found or ended, closes connection with error

5. **Message Broadcaster registers client**
   - Adds client to session's broadcast list
   - Associates client WebSocket connection with sessionId
   - Client now subscribed to session updates

6. **WebSocket Handler confirms connection**
   - Sends connection established message to client
   - Connection upgrade complete (HTTP → WebSocket)

7. **WebSocket Handler sends current state**
   - Retrieves current session state from Session Manager
   - Sends initial state to client (current scores, leaderboard)
   - Client now synchronized with session

8. **Real-time update loop begins**
   - Client connection remains open
   - Message Broadcaster can now send updates to this client
   - Updates include: SCORE_UPDATE, LEADERBOARD_UPDATE, SESSION_ENDED

9. **Client remains connected**
   - Receives real-time updates as they occur
   - Can send messages to server (e.g., heartbeat, acknowledgments)
   - Connection maintained until client disconnects or session ends

### Postconditions
- Client has active WebSocket connection
- Client registered for real-time updates
- Client synchronized with current session state


### Connection Lifecycle
- **Established**: Client successfully connected and authenticated
- **Active**: Receiving real-time updates
- **Disconnected**: Client closes connection or network failure
- **Cleanup**: Message Broadcaster removes client from broadcast list

### Message Types Received
- **SCORE_UPDATE**: When any player's score changes
- **LEADERBOARD_UPDATE**: When rankings change
- **SESSION_ENDED**: When session is ended by administrator

---

## 8. Error Handling Flow - Database Timeout

### Purpose
Demonstrate graceful handling of external service failures during player registration.

### Preconditions
- Session exists and is ACTIVE
- Student Database is slow or unresponsive

### Steps

1. **Client attempts registration**
   - POST to `/sessions/{sessionId}/players`
   - Request body: `{studentId: "STU123456"}`

2. **API Layer routes to Session Manager**
   - Calls `registerPlayer(sessionId, studentId)`

3. **Session Manager validates and delegates**
   - Session validation passes
   - Duplicate check passes
   - Calls Student Database Client

4. **Student Database Client initiates query**
   - Makes HTTP GET to `{baseUrl}/students/STU123456`
   - Sets timeout to 2000ms (2 seconds)
   - Starts timer

5. **Student Database is unresponsive**
   - Database server slow or overloaded
   - No response received
   - Client continues waiting

6. **Timeout occurs**
   - 2000ms elapsed with no response
   - Student Database Client detects timeout
   - Cancels HTTP request

7. **Student Database Client returns error**
   - Creates Error object with code DATABASE_UNAVAILABLE
   - Returns Error(DATABASE_UNAVAILABLE) to Session Manager

8. **Session Manager logs error**
   - Logs error details for monitoring/debugging
   - Error includes: timestamp, sessionId, studentId, error code

9. **Session Manager maintains state**
   - No changes made to session
   - Player NOT added to session
   - Session remains in valid state


10. **Session Manager returns error to API Layer**
    - Returns Error(DATABASE_UNAVAILABLE)
    - Includes error message: "Student database unavailable"

11. **API Layer formats error response**
    - Creates error response object
    - Sets HTTP status code: 503 Service Unavailable
    - Includes error details: code, message, timestamp

12. **Client receives error**
    - Response: 503 Service Unavailable
    - Body: `{error: "Student database unavailable", code: "DATABASE_UNAVAILABLE", timestamp: "..."}`

### Postconditions
- Player NOT registered (registration failed)
- Session state unchanged (no side effects)
- Error logged for monitoring
- Client informed of failure reason

### Error Recovery
- Client can retry registration after delay
- System remains operational for other operations
- No data corruption or inconsistent state
- Other sessions unaffected

### Timeout Configuration
- **Student Database**: 2000ms (2 seconds)
- **Rationale**: Balance between responsiveness and allowing slow responses
- **No retry**: Fail fast to maintain user experience

---

## 9. Error Handling Flow - Network Failure During Broadcast

### Purpose
Demonstrate message queuing and retry mechanism when WebSocket broadcast fails.

### Preconditions
- Session has multiple connected clients
- Score update occurs
- One or more clients have network issues

### Steps

1. **Session Manager triggers broadcast**
   - Score update completed successfully
   - Calls Message Broadcaster with ScoreUpdateMessage
   - Message contains: sessionId, studentId, newScore, newStreak, pointsAwarded, multiplier

2. **Message Broadcaster retrieves client list**
   - Gets all WebSocket connections for sessionId
   - Example: Client1, Client2 (disconnected), Client3

3. **Broadcast to Client 1**
   - Sends SCORE_UPDATE message via WebSocket
   - Client1 receives message successfully
   - Client1 sends ACK (acknowledgment)
   - Success logged


4. **Broadcast to Client 2 (network failure)**
   - Attempts to send SCORE_UPDATE message
   - Network error detected (connection lost, timeout, etc.)
   - Send operation fails
   - No ACK received

5. **Message Broadcaster detects failure**
   - Catches network exception
   - Identifies Client2 as failed recipient
   - Logs failure event

6. **Message Broadcaster queues message**
   - Creates retry queue entry for Client2
   - Stores: message, clientId, attemptCount=1, nextRetryTime
   - Message preserved for retry

7. **Message Broadcaster schedules retry**
   - Calculates backoff delay: 100ms (first retry)
   - Sets nextRetryTime = now + 100ms
   - Adds to retry scheduler

8. **Broadcast to Client 3**
   - Sends SCORE_UPDATE message via WebSocket
   - Client3 receives successfully
   - Client3 sends ACK
   - Success logged

9. **Retry loop for Client 2**
   
   **First retry (after 100ms):**
   - Waits 100ms
   - Attempts to send message to Client2
   - If still disconnected: queue again with backoff 200ms
   - If reconnected: send succeeds, remove from queue
   
   **Second retry (after 200ms):**
   - Waits additional 200ms
   - Attempts to send message
   - If still disconnected: queue again with backoff 400ms
   - If reconnected: send succeeds, remove from queue
   
   **Subsequent retries:**
   - Continue with exponential backoff: 400ms, 800ms, 1600ms, etc.
   - Max backoff capped at 5000ms (5 seconds)
   - Retries continue until success or client permanently removed

10. **Client 2 reconnects (success scenario)**
    - Client2 re-establishes WebSocket connection
    - Message Broadcaster detects reconnection
    - Queued message sent successfully
    - Client2 receives SCORE_UPDATE
    - Message removed from retry queue

### Postconditions
- All reachable clients receive the update
- Failed messages queued for retry
- No messages lost due to transient failures
- System continues operating normally


### Retry Strategy
- **Exponential backoff**: 100ms, 200ms, 400ms, 800ms, 1600ms, 3200ms, 5000ms (max)
- **No retry limit**: Continues until success or client removed
- **Message preservation**: All messages queued, none dropped
- **Order preservation**: Messages delivered in order when client reconnects

### Graceful Degradation
- Broadcast failure doesn't block score calculation
- Other clients receive updates normally
- Failed client catches up when reconnected
- System remains responsive

---

## 10. Complete Quiz Session Flow (End-to-End)

### Purpose
Demonstrate a complete quiz session from creation to completion with multiple players.

### Preconditions
- Administrator has access to create sessions
- Two students (Alice: STU001, Bob: STU002) ready to participate
- Student Database contains both student records

### Steps

**Phase 1: Session Setup**

1. **Administrator creates session**
   - POST to `/sessions`
   - System generates SessionId: `"abc-123"`
   - Response: `{sessionId: "abc-123", status: "ACTIVE", startTime: "10:00:00"}`

2. **Alice registers**
   - POST to `/sessions/abc-123/players` with `{studentId: "STU001"}`
   - System queries Student Database → returns "Alice"
   - PlayerState created: `{studentId: "STU001", name: "Alice", score: 0, streak: 0}`
   - Response: 201 Created with player data
   - Broadcast: PLAYER_JOINED to all clients

3. **Bob registers**
   - POST to `/sessions/abc-123/players` with `{studentId: "STU002"}`
   - System queries Student Database → returns "Bob"
   - PlayerState created: `{studentId: "STU002", name: "Bob", score: 0, streak: 0}`
   - Response: 201 Created with player data
   - Broadcast: PLAYER_JOINED to all clients

4. **Initial leaderboard**
   - Both players at score 0
   - Rankings: [Alice: 0, Bob: 0] (alphabetical order for ties)

**Phase 2: Quiz Begins**

5. **Alice answers Question 1 correctly (10 points)**
   - POST to `/sessions/abc-123/answers` with `{studentId: "STU001", isCorrect: true, basePoints: 10}`
   - Score Calculator: currentScore=0, streak=0, isCorrect=true
   - Calculation: newStreak=1, multiplier=1.1, pointsAwarded=11, newScore=11
   - Alice's state: `{score: 11, streak: 1}`
   - Response: `{newScore: 11, newStreak: 1, pointsAwarded: 11, multiplierApplied: 1.1}`
   - Broadcast: SCORE_UPDATE and LEADERBOARD_UPDATE
   - Leaderboard: [Alice: 11, Bob: 0]


6. **Bob answers Question 1 correctly (10 points)**
   - POST with `{studentId: "STU002", isCorrect: true, basePoints: 10}`
   - Score Calculator: currentScore=0, streak=0, isCorrect=true
   - Calculation: newStreak=1, multiplier=1.1, pointsAwarded=11, newScore=11
   - Bob's state: `{score: 11, streak: 1}`
   - Response: `{newScore: 11, newStreak: 1, pointsAwarded: 11, multiplierApplied: 1.1}`
   - Broadcast: SCORE_UPDATE and LEADERBOARD_UPDATE
   - Leaderboard: [Alice: 11, Bob: 11] (tied, alphabetical order)

7. **Alice answers Question 2 correctly (10 points)**
   - POST with `{studentId: "STU001", isCorrect: true, basePoints: 10}`
   - Score Calculator: currentScore=11, streak=1, isCorrect=true
   - Calculation: newStreak=2, multiplier=1.2, pointsAwarded=12, newScore=23
   - Alice's state: `{score: 23, streak: 2}`
   - Response: `{newScore: 23, newStreak: 2, pointsAwarded: 12, multiplierApplied: 1.2}`
   - Broadcast: SCORE_UPDATE and LEADERBOARD_UPDATE
   - Leaderboard: [Alice: 23, Bob: 11] (Alice leads)

8. **Bob answers Question 2 incorrectly**
   - POST with `{studentId: "STU002", isCorrect: false, basePoints: 10}`
   - Score Calculator: currentScore=11, streak=1, isCorrect=false
   - Calculation: newStreak=0, multiplier=0.0, pointsAwarded=0, newScore=11 (unchanged)
   - Bob's state: `{score: 11, streak: 0}` (streak reset)
   - Response: `{newScore: 11, newStreak: 0, pointsAwarded: 0, multiplierApplied: 0.0}`
   - Broadcast: SCORE_UPDATE (leaderboard unchanged)
   - Leaderboard: [Alice: 23, Bob: 11]

9. **Alice answers Question 3 correctly (10 points)**
   - POST with `{studentId: "STU001", isCorrect: true, basePoints: 10}`
   - Score Calculator: currentScore=23, streak=2, isCorrect=true
   - Calculation: newStreak=3, multiplier=1.3, pointsAwarded=13, newScore=36
   - Alice's state: `{score: 36, streak: 3}`
   - Response: `{newScore: 36, newStreak: 3, pointsAwarded: 13, multiplierApplied: 1.3}`
   - Broadcast: SCORE_UPDATE and LEADERBOARD_UPDATE
   - Leaderboard: [Alice: 36, Bob: 11]

10. **Bob answers Question 3 correctly (10 points)**
    - POST with `{studentId: "STU002", isCorrect: true, basePoints: 10}`
    - Score Calculator: currentScore=11, streak=0, isCorrect=true
    - Calculation: newStreak=1, multiplier=1.1, pointsAwarded=11, newScore=22
    - Bob's state: `{score: 22, streak: 1}` (streak rebuilding)
    - Response: `{newScore: 22, newStreak: 1, pointsAwarded: 11, multiplierApplied: 1.1}`
    - Broadcast: SCORE_UPDATE and LEADERBOARD_UPDATE
    - Leaderboard: [Alice: 36, Bob: 22]


**Phase 3: Session End**

11. **Administrator ends session**
    - POST to `/sessions/abc-123/end`
    - Session Manager changes status to ENDED
    - Sets endTime: "10:15:00"

12. **Leaderboard Service generates final rankings**
    - Sorts players: Alice (36), Bob (22)
    - Assigns ranks: Alice=1, Bob=2
    - Creates final Leaderboard object

13. **Persistence Layer saves results**
    - Attempt 1: Validates data, stores to database
    - Success on first attempt
    - Data saved:
      - Session: `{sessionId: "abc-123", endTime: "10:15:00"}`
      - Players: `[{studentId: "STU001", name: "Alice", finalScore: 36}, {studentId: "STU002", name: "Bob", finalScore: 22}]`

14. **Message Broadcaster notifies all clients**
    - Broadcasts SESSION_ENDED message
    - Includes final leaderboard
    - Message: `{type: "SESSION_ENDED", sessionId: "abc-123", finalLeaderboard: {rankings: [{rank: 1, name: "Alice", score: 36}, {rank: 2, name: "Bob", score: 22}]}}`

15. **Administrator receives confirmation**
    - Response: 200 OK
    - Body: `{sessionId: "abc-123", endTime: "10:15:00", playerCount: 2, finalLeaderboard: {...}}`

16. **Session now immutable**
    - Any attempt to submit answers returns SESSION_ENDED error
    - Session data persisted and available for analysis
    - WebSocket connections can be closed

### Final State Summary

**Alice (Winner):**
- Final Score: 36 points
- Final Streak: 3
- Questions: 3 correct, 0 incorrect
- Rank: 1

**Bob (Runner-up):**
- Final Score: 22 points
- Final Streak: 1
- Questions: 2 correct, 1 incorrect
- Rank: 2

**Session:**
- Duration: 15 minutes (10:00:00 - 10:15:00)
- Total Players: 2
- Status: ENDED
- Data: Persisted to database

### Key Observations

1. **Streak Impact**: Alice's consistent correct answers (3 in a row) earned bonus multipliers (1.1x, 1.2x, 1.3x)
2. **Streak Reset**: Bob's incorrect answer reset his streak, requiring him to rebuild
3. **Real-time Updates**: All score changes broadcast immediately to both players
4. **Data Integrity**: All results persisted successfully for future analysis
5. **Performance**: All operations completed within 100ms requirement


---

## Summary of All Flows

### Core Operational Flows
1. **Session Creation** - Initialize new quiz session
2. **Player Registration** - Add students to session via database lookup
3. **Score Update (Correct)** - Award points with streak multiplier
4. **Score Update (Incorrect)** - Reset streak, maintain score
5. **Get Leaderboard** - Retrieve current rankings
6. **Session End** - Finalize and persist results

### Real-Time Communication Flows
7. **WebSocket Connection** - Establish live update channel
8. **Score Broadcast** - Push updates to all clients
9. **Leaderboard Broadcast** - Push ranking changes

### Error Handling Flows
10. **Database Timeout** - Handle external service failures
11. **Network Failure** - Queue and retry failed broadcasts
12. **Validation Errors** - Reject invalid inputs
13. **State Errors** - Prevent invalid operations

### Integration Flows
14. **Student Database Integration** - External API lookup
15. **Persistence with Retry** - Reliable data storage
16. **Message Queuing** - Reliable message delivery

---

## Performance Characteristics

### Timing Requirements
- **Score Update**: < 100ms end-to-end
- **Database Query**: < 2000ms timeout
- **Broadcast Latency**: < 50ms to all clients
- **Persistence Retry**: 100ms, 200ms, 400ms backoff

### Scalability Targets
- **Concurrent Sessions**: 10+ simultaneous sessions
- **Players per Session**: 50+ players
- **Updates per Second**: 100+ score updates
- **WebSocket Connections**: 500+ concurrent clients

### Reliability Metrics
- **Uptime**: 99.5% during active sessions
- **Data Persistence**: 99.9% success rate
- **Message Delivery**: 100% (with retry)
- **State Consistency**: 100% (atomic operations)

---

## Error Code Reference

| Error Code | HTTP Status | Description | Recovery Action |
|------------|-------------|-------------|-----------------|
| SESSION_NOT_FOUND | 404 | Session doesn't exist | Verify session ID |
| SESSION_ENDED | 410 | Session already ended | Cannot modify ended session |
| PLAYER_NOT_FOUND | 404 | Player not registered | Register player first |
| DUPLICATE_PLAYER | 409 | Student already registered | Use existing registration |
| STUDENT_NOT_FOUND | 404 | Student not in database | Verify student ID |
| DATABASE_UNAVAILABLE | 503 | External DB timeout/error | Retry after delay |
| INVALID_INPUT | 400 | Invalid request format | Fix request data |
| PERSISTENCE_FAILED | 500 | Storage failed after retries | Contact administrator |

---

## State Transition Diagram

```
Session States:
  [CREATED] → [ACTIVE] → [ENDED]
  
Player States (per session):
  [UNREGISTERED] → [REGISTERED] → [ACTIVE] → [FINAL]
  
Streak States:
  [0] → [1] → [2] → ... → [20] (max multiplier at streak 20)
       ↑________________________↓
       (incorrect answer resets to 0)
```

---

## Validation Rules Summary

### Session Validation
- Session ID must be valid UUID
- Session must exist for all operations except create
- Session must be ACTIVE for registration and score updates

### Player Validation
- Student ID format: `^[a-zA-Z0-9-]{6,12}$`
- Student must exist in external database
- No duplicate registrations per session
- Player must be registered before submitting answers

### Score Validation
- Base points must be positive integer
- Scores must be non-negative
- Streak must be non-negative
- Multiplier range: 1.0 to 3.0

### Data Persistence Validation
- All required fields must be present
- Session ID must be valid UUID
- End time must be set
- All player scores must be non-negative
- All player names must be non-empty

---

## End of Flow Steps Documentation
