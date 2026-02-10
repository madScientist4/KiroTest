# Activity Diagrams
# Scoring System Workflows

**Version:** 1.0  
**Date:** February 9, 2026  

---

## Table of Contents

1. [Complete Session Lifecycle](#1-complete-session-lifecycle)
2. [Player Registration Flow](#2-player-registration-flow)
3. [Answer Submission and Score Update Flow](#3-answer-submission-and-score-update-flow)
4. [Leaderboard Update Flow](#4-leaderboard-update-flow)
5. [Session End and Persistence Flow](#5-session-end-and-persistence-flow)
6. [Error Handling Flow](#6-error-handling-flow)

---

## 1. Complete Session Lifecycle

This diagram shows the end-to-end flow from session creation to completion.

```mermaid
graph TD
    Start([Administrator Starts Session]) --> CreateSession[Create Session]
    CreateSession --> GenerateID[Generate Unique Session ID]
    GenerateID --> InitState[Initialize Session State<br/>Status: ACTIVE<br/>Players: Empty Map]
    InitState --> SessionActive{Session Active}
    
    SessionActive -->|Yes| WaitForPlayers[Wait for Players to Join]
    WaitForPlayers --> PlayerJoins{Player Joins?}
    
    PlayerJoins -->|Yes| RegisterPlayer[Register Player]
    RegisterPlayer --> PlayerRegistered{Registration<br/>Successful?}
    
    PlayerRegistered -->|Yes| AddToSession[Add Player to Session<br/>Score: 0, Streak: 0]
    PlayerRegistered -->|No| RegError[Return Error]
    RegError --> WaitForPlayers
    
    AddToSession --> BroadcastJoin[Broadcast Player Joined]
    BroadcastJoin --> WaitForActivity
    
    PlayerJoins -->|No| WaitForActivity[Wait for Activity]
    
    WaitForActivity --> Activity{Activity Type?}
    
    Activity -->|Answer Submitted| ProcessAnswer[Process Answer]
    ProcessAnswer --> CalcScore[Calculate Score]
    CalcScore --> UpdateState[Update Player State]
    UpdateState --> BroadcastScore[Broadcast Score Update]
    BroadcastScore --> UpdateLeaderboard[Update Leaderboard]
    UpdateLeaderboard --> BroadcastLB[Broadcast Leaderboard]
    BroadcastLB --> WaitForActivity
    
    Activity -->|More Players| PlayerJoins
    Activity -->|End Session| EndSession[End Session Request]
    
    EndSession --> ChangeStatus[Change Status to ENDED]
    ChangeStatus --> PersistData[Persist Session Data]
    PersistData --> RetryLogic{Persistence<br/>Successful?}
    
    RetryLogic -->|Yes| FinalLB[Generate Final Leaderboard]
    RetryLogic -->|No, Retry < 3| Wait[Exponential Backoff]
    Wait --> PersistData
    RetryLogic -->|No, Retry = 3| PersistError[Log Persistence Error]
    
    FinalLB --> BroadcastEnd[Broadcast Session Ended]
    PersistError --> BroadcastEnd
    BroadcastEnd --> End([Session Complete])
    
    SessionActive -->|No| SessionEndedError[Return Session Ended Error]
    SessionEndedError --> End
```

---

## 2. Player Registration Flow

Detailed flow for registering a player in an active session.

```mermaid
graph TD
    Start([Player Registration Request]) --> ValidateSession{Session<br/>Exists?}
    
    ValidateSession -->|No| Error1[Return SESSION_NOT_FOUND]
    Error1 --> End([End])
    
    ValidateSession -->|Yes| CheckStatus{Session<br/>Status?}
    
    CheckStatus -->|ENDED| Error2[Return SESSION_ENDED]
    Error2 --> End
    
    CheckStatus -->|ACTIVE| ValidateStudentID[Validate Student ID Format]
    ValidateStudentID --> FormatValid{Format<br/>Valid?}
    
    FormatValid -->|No| Error3[Return INVALID_INPUT<br/>Invalid student ID format]
    Error3 --> End
    
    FormatValid -->|Yes| CheckDuplicate{Student ID<br/>Already in<br/>Session?}
    
    CheckDuplicate -->|Yes| Error4[Return DUPLICATE_PLAYER]
    Error4 --> End
    
    CheckDuplicate -->|No| QueryDB[Query Student Database<br/>GET /students/{studentId}]
    QueryDB --> SetTimeout[Set 2-second Timeout]
    SetTimeout --> DBResponse{Database<br/>Response?}
    
    DBResponse -->|Timeout| Error5[Return DATABASE_UNAVAILABLE<br/>Student database timeout]
    Error5 --> End
    
    DBResponse -->|Network Error| Error6[Return DATABASE_UNAVAILABLE<br/>Network error]
    Error6 --> End
    
    DBResponse -->|404 Not Found| Error7[Return STUDENT_NOT_FOUND]
    Error7 --> End
    
    DBResponse -->|200 OK| ValidateResponse[Validate Response<br/>Has studentId and name?]
    ValidateResponse --> ResponseValid{Valid?}
    
    ResponseValid -->|No| Error8[Return DATABASE_UNAVAILABLE<br/>Invalid response format]
    Error8 --> End
    
    ResponseValid -->|Yes| CreatePlayerState[Create Player State<br/>studentId: from DB<br/>name: from DB<br/>score: 0<br/>streak: 0]
    
    CreatePlayerState --> AddToMap[Add to Session Players Map]
    AddToMap --> BroadcastJoin[Broadcast Player Joined<br/>to all clients]
    BroadcastJoin --> UpdateLB[Update Leaderboard]
    UpdateLB --> BroadcastLB[Broadcast Leaderboard Update]
    BroadcastLB --> Success[Return PlayerState]
    Success --> End
```

---

## 3. Answer Submission and Score Update Flow

Flow for processing an answer and updating the player's score.

```mermaid
graph TD
    Start([Answer Submission Request]) --> ValidateSession{Session<br/>Exists?}
    
    ValidateSession -->|No| Error1[Return SESSION_NOT_FOUND]
    Error1 --> End([End])
    
    ValidateSession -->|Yes| CheckStatus{Session<br/>Status?}
    
    CheckStatus -->|ENDED| Error2[Return SESSION_ENDED<br/>Cannot update ended session]
    Error2 --> End
    
    CheckStatus -->|ACTIVE| ValidatePlayer{Player<br/>Registered?}
    
    ValidatePlayer -->|No| Error3[Return PLAYER_NOT_FOUND]
    Error3 --> End
    
    ValidatePlayer -->|Yes| ValidateInput[Validate Input<br/>basePoints > 0]
    ValidateInput --> InputValid{Valid?}
    
    InputValid -->|No| Error4[Return INVALID_INPUT]
    Error4 --> End
    
    InputValid -->|Yes| GetPlayerState[Get Current Player State<br/>currentScore, currentStreak]
    
    GetPlayerState --> CheckAnswer{Answer<br/>Correct?}
    
    CheckAnswer -->|No| ResetStreak[Calculate Score Result<br/>newScore = currentScore<br/>newStreak = 0<br/>pointsAwarded = 0<br/>multiplier = 0.0]
    
    CheckAnswer -->|Yes| CalcStreak[Calculate New Streak<br/>newStreak = currentStreak + 1]
    CalcStreak --> CalcMultiplier[Calculate Multiplier<br/>multiplier = min1.0 + newStreak × 0.1, 3.0]
    CalcMultiplier --> CalcPoints[Calculate Points Awarded<br/>pointsAwarded = floorbasePoints × multiplier]
    CalcPoints --> CalcScore[Calculate New Score<br/>newScore = currentScore + pointsAwarded]
    
    CalcScore --> UpdatePlayer[Update Player State<br/>score = newScore<br/>streak = newStreak]
    ResetStreak --> UpdatePlayer
    
    UpdatePlayer --> CreateMessage[Create Score Update Message<br/>studentId, newScore, newStreak<br/>pointsAwarded, multiplier]
    
    CreateMessage --> BroadcastScore[Broadcast Score Update<br/>to all session clients]
    BroadcastScore --> BroadcastSuccess{Broadcast<br/>Successful?}
    
    BroadcastSuccess -->|No| QueueMessage[Queue Message for Retry<br/>Exponential Backoff]
    QueueMessage --> RetryBroadcast[Retry Broadcast]
    RetryBroadcast --> CheckRanking
    
    BroadcastSuccess -->|Yes| CheckRanking{Score Change<br/>Affects<br/>Rankings?}
    
    CheckRanking -->|Yes| UpdateLB[Update Leaderboard<br/>Re-sort and re-rank]
    UpdateLB --> BroadcastLB[Broadcast Leaderboard Update]
    BroadcastLB --> ReturnResult
    
    CheckRanking -->|No| ReturnResult[Return Score Update Result]
    ReturnResult --> End
```

---

## 4. Leaderboard Update Flow

Flow for updating and broadcasting the leaderboard.

```mermaid
graph TD
    Start([Leaderboard Update Triggered]) --> GetPlayers[Get All Players from Session]
    
    GetPlayers --> CreateEntries[Create Leaderboard Entries<br/>For each player:<br/>- studentId<br/>- name<br/>- score]
    
    CreateEntries --> SortEntries[Sort Entries<br/>1. By score descending<br/>2. By name ascending alphabetical]
    
    SortEntries --> AssignRanks[Assign Ranks]
    AssignRanks --> InitRank[currentRank = 1]
    InitRank --> LoopEntries{More<br/>Entries?}
    
    LoopEntries -->|Yes| CheckPrevious{i > 0 AND<br/>score < previous<br/>score?}
    
    CheckPrevious -->|Yes| UpdateRank[currentRank = i + 1]
    UpdateRank --> SetRank[Set entry.rank = currentRank]
    
    CheckPrevious -->|No| SetRank
    SetRank --> NextEntry[Move to next entry]
    NextEntry --> LoopEntries
    
    LoopEntries -->|No| CreateLB[Create Leaderboard Object<br/>sessionId, rankings list]
    
    CreateLB --> ValidateComplete{All Players<br/>Included?}
    
    ValidateComplete -->|No| LogError[Log Completeness Error]
    LogError --> BroadcastLB
    
    ValidateComplete -->|Yes| BroadcastLB[Broadcast Leaderboard Update<br/>to all session clients]
    
    BroadcastLB --> GetClients[Get All Connected Clients<br/>for Session]
    GetClients --> LoopClients{More<br/>Clients?}
    
    LoopClients -->|Yes| SendMessage[Send Leaderboard Message<br/>to Client]
    SendMessage --> SendSuccess{Send<br/>Successful?}
    
    SendSuccess -->|No| QueueForClient[Queue Message for Client<br/>Add to retry queue]
    QueueForClient --> NextClient
    
    SendSuccess -->|Yes| NextClient[Move to next client]
    NextClient --> LoopClients
    
    LoopClients -->|No| CheckQueue{Retry Queue<br/>Empty?}
    
    CheckQueue -->|No| ProcessRetry[Process Retry Queue<br/>Exponential Backoff]
    ProcessRetry --> End([End])
    
    CheckQueue -->|Yes| End
```

---

## 5. Session End and Persistence Flow

Flow for ending a session and persisting results.

```mermaid
graph TD
    Start([End Session Request]) --> ValidateSession{Session<br/>Exists?}
    
    ValidateSession -->|No| Error1[Return SESSION_NOT_FOUND]
    Error1 --> End([End])
    
    ValidateSession -->|Yes| CheckStatus{Current<br/>Status?}
    
    CheckStatus -->|ENDED| Error2[Return SESSION_ENDED<br/>Already ended]
    Error2 --> End
    
    CheckStatus -->|ACTIVE| ChangeStatus[Change Session Status<br/>status = ENDED<br/>endTime = current timestamp]
    
    ChangeStatus --> PreventUpdates[Prevent Further Updates<br/>Reject new registrations<br/>Reject score updates]
    
    PreventUpdates --> CreateRecord[Create Session Record<br/>sessionId<br/>endTime<br/>player list]
    
    CreateRecord --> LoopPlayers{More<br/>Players?}
    
    LoopPlayers -->|Yes| CreatePlayerRecord[Create Player Record<br/>studentId<br/>name<br/>finalScore]
    CreatePlayerRecord --> AddToList[Add to players list]
    AddToList --> LoopPlayers
    
    LoopPlayers -->|No| ValidateData[Validate Session Data<br/>- sessionId is UUID<br/>- endTime present<br/>- all studentIds valid<br/>- all scores non-negative]
    
    ValidateData --> DataValid{Data<br/>Valid?}
    
    DataValid -->|No| ValidationError[Return PERSISTENCE_FAILED<br/>Invalid data]
    ValidationError --> End
    
    DataValid -->|Yes| InitRetry[Initialize Retry Counter<br/>attempt = 0<br/>maxRetries = 3]
    
    InitRetry --> AttemptSave[Attempt to Save to Database]
    AttemptSave --> SaveSuccess{Save<br/>Successful?}
    
    SaveSuccess -->|Yes| GenerateFinal[Generate Final Leaderboard]
    GenerateFinal --> CreateEndMessage[Create Session Ended Message<br/>sessionId<br/>finalLeaderboard]
    CreateEndMessage --> BroadcastEnd[Broadcast Session Ended<br/>to all clients]
    BroadcastEnd --> CloseConnections[Close WebSocket Connections]
    CloseConnections --> ReturnSummary[Return Session Summary<br/>sessionId, endTime<br/>playerCount, finalLeaderboard]
    ReturnSummary --> End
    
    SaveSuccess -->|No| IncrementRetry[Increment attempt counter]
    IncrementRetry --> CheckRetry{attempt <<br/>maxRetries?}
    
    CheckRetry -->|Yes| CalcBackoff[Calculate Exponential Backoff<br/>delay = min100 × 2^attempt, 5000ms]
    CalcBackoff --> Wait[Wait for delay]
    Wait --> AttemptSave
    
    CheckRetry -->|No| LogFailure[Log Persistence Failure<br/>Failed after 3 attempts]
    LogFailure --> ReturnError[Return PERSISTENCE_FAILED<br/>with error details]
    ReturnError --> BroadcastEnd
```

---

## 6. Error Handling Flow

General error handling and recovery flow.

```mermaid
graph TD
    Start([Error Occurs]) --> ClassifyError{Error<br/>Type?}
    
    ClassifyError -->|Validation Error| ValidationFlow[Validation Error Flow]
    ValidationFlow --> LogValidation[Log Validation Error<br/>Level: WARN]
    LogValidation --> ReturnImmediate[Return Error Response<br/>Immediately]
    ReturnImmediate --> End([End])
    
    ClassifyError -->|State Error| StateFlow[State Error Flow]
    StateFlow --> LogState[Log State Error<br/>Level: WARN]
    LogState --> CheckState{Can<br/>Recover?}
    CheckState -->|No| ReturnStateError[Return Error Response]
    ReturnStateError --> End
    CheckState -->|Yes| AttemptRecovery[Attempt State Recovery]
    AttemptRecovery --> RetryOperation[Retry Operation]
    RetryOperation --> End
    
    ClassifyError -->|External Service Error| ExternalFlow[External Service Error Flow]
    ExternalFlow --> IdentifyService{Which<br/>Service?}
    
    IdentifyService -->|Student Database| DBErrorFlow[Database Error Flow]
    DBErrorFlow --> LogDBError[Log Database Error<br/>Level: ERROR]
    LogDBError --> CheckDBRetry{Retry<br/>Allowed?}
    CheckDBRetry -->|No| ReturnDBError[Return DATABASE_UNAVAILABLE]
    ReturnDBError --> End
    CheckDBRetry -->|Yes| RetryDB[Retry with Timeout]
    RetryDB --> End
    
    IdentifyService -->|Persistence| PersistErrorFlow[Persistence Error Flow]
    PersistErrorFlow --> LogPersistError[Log Persistence Error<br/>Level: ERROR]
    LogPersistError --> CheckPersistRetry{Retry Count<br/>< 3?}
    CheckPersistRetry -->|Yes| BackoffWait[Exponential Backoff Wait]
    BackoffWait --> RetryPersist[Retry Persistence]
    RetryPersist --> End
    CheckPersistRetry -->|No| ReturnPersistError[Return PERSISTENCE_FAILED]
    ReturnPersistError --> End
    
    ClassifyError -->|Network Error| NetworkFlow[Network Error Flow]
    NetworkFlow --> LogNetwork[Log Network Error<br/>Level: ERROR]
    LogNetwork --> QueueMessage[Queue Message for Retry]
    QueueMessage --> ScheduleRetry[Schedule Retry<br/>Exponential Backoff]
    ScheduleRetry --> End
    
    ClassifyError -->|Calculation Error| CalcFlow[Calculation Error Flow]
    CalcFlow --> LogCalc[Log Calculation Error<br/>Level: ERROR]
    LogCalc --> MaintainState[Maintain Previous Valid State<br/>Do not update score]
    MaintainState --> ReturnCalcError[Return Error Response<br/>Score unchanged]
    ReturnCalcError --> End
    
    ClassifyError -->|Unknown Error| UnknownFlow[Unknown Error Flow]
    UnknownFlow --> LogUnknown[Log Unknown Error<br/>Level: CRITICAL<br/>Include stack trace]
    LogUnknown --> AlertOps[Alert Operations Team]
    AlertOps --> ReturnGeneric[Return Generic Error<br/>Internal server error]
    ReturnGeneric --> End
```

---

## Legend

**Shapes:**
- `([Text])` - Start/End points
- `[Text]` - Process/Action
- `{Text?}` - Decision point
- `-->` - Flow direction
- `-->|Label|` - Conditional flow

**Color Coding (if rendered):**
- Green paths: Success flows
- Red paths: Error flows
- Yellow paths: Retry/Recovery flows

---

**End of Activity Diagrams Document**
