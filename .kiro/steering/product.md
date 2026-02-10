# Product Overview

This project implements a **real-time scoring system** for educational quiz and game sessions. The system tracks multiple students simultaneously, awards points for correct answers with streak-based multipliers, and displays live leaderboards.

## Core Features

- **Player registration** via student database integration
- **Dynamic scoring** with streak multipliers (up to 3x bonus)
- **Real-time leaderboard** updates within 100ms
- **Session management** with data persistence
- **Account API integration** for retrieving account balances and currency information

## Domain Context

The system is designed for educational environments where:
- Students participate in quiz/game sessions
- Performance is tracked and rewarded
- Real-time feedback enhances engagement
- Results are persisted for analysis

## Integration Points

- **Student Database**: External system for student identification
- **Account API**: REST API for retrieving account details (customer/account numbers → balance/currency)
- **Data Mapping**: Defines input/output transformations for account operations
