# API Error Logger

A web application for systematically logging, validating, and tracking API errors encountered during integration and testing.

## Project Structure

```
api-error-logger/
├── src/                          # Backend Java source code
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fnb/apierrorlogger/
│   │   │       └── ApiErrorLoggerApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                     # Backend tests
├── frontend/                     # Frontend React application
│   ├── src/
│   │   ├── api/                  # API client configuration
│   │   ├── App.tsx               # Main application component
│   │   ├── main.tsx              # Application entry point
│   │   └── index.css             # Global styles with Tailwind
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── tailwind.config.js
├── pom.xml                       # Maven configuration
└── README.md
```

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Web** - REST API
- **Spring Data JPA** - Database access
- **PostgreSQL** - Database
- **JavaMail** - Email notifications
- **Swagger Parser** - OpenAPI validation
- **Lombok** - Boilerplate reduction
- **jqwik** - Property-based testing

### Frontend
- **React 18** with TypeScript
- **Vite** - Build tool
- **React Router** - Routing
- **Axios** - HTTP client
- **TailwindCSS** - Styling with FNB branding

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Node.js 18+ and npm
- PostgreSQL 14+

## Setup Instructions

### Database Setup

1. Install PostgreSQL
2. Create database:
```sql
CREATE DATABASE api_error_logger;
```

3. Update database credentials in `src/main/resources/application.properties`

### Backend Setup

1. Build the project:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:3000`

## Configuration

### Email Configuration

Update SMTP settings in `application.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### FNB Color Scheme

The application uses FNB brand colors defined in `frontend/tailwind.config.js`:
- Primary Blue: `#005EB8`
- Secondary Blue: `#00A3E0`
- Accent Gold: `#FFB81C`
- Dark Blue: `#002855`
- Success Green: `#00A651`
- Error Red: `#E31837`

## Development

### Running Tests

Backend tests:
```bash
mvn test
```

Frontend tests:
```bash
cd frontend
npm test
```

### Building for Production

Backend:
```bash
mvn clean package
```

Frontend:
```bash
cd frontend
npm run build
```

## API Endpoints

- `POST /api/errors` - Create error request
- `GET /api/errors` - List error requests
- `GET /api/errors/{id}` - Get error request details
- `GET /api/environments` - List environments
- `PUT /api/environments/{id}` - Update environment status
- `POST /api/openapi` - Upload OpenAPI specification
- `GET /api/openapi` - List OpenAPI specifications

## License

Copyright © 2024 FNB
