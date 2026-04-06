# Hostel Management System

This repository contains a full-stack hostel management application with:

- `HostelManagement_Frontend` - React + Vite frontend
- `HostelManagment_Backend` - Spring Boot backend

## Tech Stack

- Frontend: React 18, Vite, Tailwind CSS, Axios, React Router
- Backend: Java 17, Spring Boot 3, Spring Security, Spring Data JPA
- Databases: PostgreSQL and MongoDB

## Project Structure

```text
.
├── HostelManagement_Frontend/
├── HostelManagment_Backend/
└── README.md
```

## Requirements

Install the following before running the project:

- Node.js 18+ recommended
- npm 9+ recommended
- Java 17
- Maven 3.9+ or use the included Maven wrapper
- PostgreSQL 14+ recommended
- MongoDB 6+ recommended

## Backend Requirements

The backend uses:

- PostgreSQL for relational data
- MongoDB for agreement-related documents

Default backend configuration is currently set in [application.properties](d:/Last%20Project/HostelManagment_Backend/src/main/resources/application.properties):

- PostgreSQL URL: `jdbc:postgresql://localhost:5432/hostel_management`
- PostgreSQL username: `postgres`
- MongoDB URL: `mongodb://localhost:27017/hostel_management`
- Default Spring Boot port: `8080`

Before running the backend, make sure:

1. PostgreSQL is running
2. A database named `hostel_management` exists
3. MongoDB is running locally on port `27017`
4. The credentials in `application.properties` match your local setup

## Optional Email Configuration

If you want email features enabled, use the example file at [application-email.properties.example](d:/Last%20Project/HostelManagment_Backend/src/main/resources/application-email.properties.example) and copy the values you need into the backend application properties.

Example values included in the project:

- `app.frontend.url=http://localhost:3000`
- `app.email.enabled=false`

## Frontend Requirements

The frontend runs with Vite and starts on port `3000`.

API requests use:

- `VITE_API_URL` if provided
- otherwise `http://localhost:8080`

The frontend API base is defined in [apiClient.js](d:/Last%20Project/HostelManagement_Frontend/src/services/apiClient.js).

## How To Run The Project

### 1. Start the backend

Open a terminal in `HostelManagment_Backend` and run:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend should start at:

```text
http://localhost:8080
```

### 2. Start the frontend

Open another terminal in `HostelManagement_Frontend` and run:

```bash
npm install
npm run dev
```

The frontend should start at:

```text
http://localhost:3000
```

## Recommended Local Setup Flow

1. Start PostgreSQL
2. Start MongoDB
3. Run the Spring Boot backend
4. Run the Vite frontend
5. Open `http://localhost:3000`

## Build Commands

### Frontend

```bash
npm run build
```

### Backend

```bash
./mvnw clean package
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

## Useful Notes

- The backend uses `spring.jpa.hibernate.ddl-auto=update`, so tables can be created or updated automatically in PostgreSQL.
- The frontend Vite config is in [vite.config.js](d:/Last%20Project/HostelManagement_Frontend/vite.config.js).
- The backend currently stores configuration directly in `application.properties`; update these values before sharing or deploying the project.

## Existing Module Readmes

- Frontend details: [HostelManagement_Frontend/README.md](d:/Last%20Project/HostelManagement_Frontend/README.md)
