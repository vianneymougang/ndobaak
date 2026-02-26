# Ndobaâk

Plateforme de mise en relation entre propriétaires et locataires au Cameroun.

## Stack technique

| Couche     | Technologie                          |
|------------|--------------------------------------|
| Frontend   | Angular 21, SCSS, Bun               |
| Backend    | Spring Boot 3.5, Java 21            |
| Base       | PostgreSQL 18, Flyway               |
| Stockage   | MinIO (S3-compatible)               |
| Auth       | JWT + OAuth2 Google                  |
| API Docs   | SpringDoc OpenAPI (Swagger)          |

## Démarrage rapide

### Prérequis

- Java 21
- Node.js 22+ / Bun 1.3+
- Docker & Docker Compose

### Infrastructure locale

```bash
docker compose up -d
```

Services disponibles :

| Service  | URL                        |
|----------|----------------------------|
| Backend  | http://localhost:8080       |
| Swagger  | http://localhost:8080/api/v1/swagger-ui.html |
| pgAdmin  | http://localhost:5050       |
| Mailtrap | http://localhost:1080       |
| MinIO    | http://localhost:9001       |

### Backend

```bash
cd backend
cp .env.example .env   # Configurer les variables
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
bun install
bun start
```

L'application est accessible sur http://localhost:4200.

## Structure du projet

```
ndobaak/
├── backend/       # API Spring Boot
├── frontend/      # Application Angular
├── docs/          # Cahiers d'analyse, conception, diagrammes
└── docker-compose.yml
```
