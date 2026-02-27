# Ndobaâk

Plateforme de logement étudiant pour la zone Eyang près du campus IUSJC, Yaoundé, Cameroun. Elle met en relation étudiants et propriétaires avec recherche géolocalisée, gestion des annonces et validation administrative.

## Stack technique

| Couche      | Technologie                                        |
|-------------|----------------------------------------------------|
| Frontend    | Angular 21 · PrimeNG 21 · Tailwind CSS 4 · Bun    |
| Backend     | Spring Boot 3.5 · Java 21 · Maven                 |
| Base de données | PostgreSQL 18 · Flyway (migrations versionées) |
| Stockage    | MinIO (S3-compatible, auto-hébergé)               |
| Auth        | JWT stateless · OAuth2 Google (prévu)             |
| API Docs    | SpringDoc OpenAPI (Swagger UI)                    |
| Infra local | Docker Compose                                    |

## Démarrage rapide

### Prérequis

- Java 21+
- Bun 1.3+
- Docker & Docker Compose

### 1 — Infrastructure locale

```bash
docker compose up -d
```

| Service   | URL                                              | Identifiants par défaut      |
|-----------|--------------------------------------------------|------------------------------|
| pgAdmin   | http://localhost:5050                            | admin@ndobaak.com / admin    |
| Mailtrap  | http://localhost:1080                            | —                            |
| MinIO     | http://localhost:9001                            | ndobaak / ndobaak_minio_2026 |

### 2 — Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway applique automatiquement les migrations au démarrage.

| Endpoint  | URL                                              |
|-----------|--------------------------------------------------|
| API       | http://localhost:8080/api/v1                     |
| Swagger   | http://localhost:8080/api/v1/swagger-ui.html     |

### 3 — Frontend

```bash
cd frontend
bun install
bun start
```

Application accessible sur http://localhost:4200.

### Compte administrateur par défaut

| Champ         | Valeur               |
|---------------|----------------------|
| Email         | admin@ndobaak.com    |
| Mot de passe  | Admin@ndobaak2026    |
| Rôle          | SUPER_ADMIN          |

> Créé par la migration Flyway V3, mot de passe corrigé par V4.

## Structure du projet

```
ndobaak/
├── backend/
│   └── src/main/java/com/ndobaak/backend/
│       ├── admin/          # Validation des annonces (approve/reject)
│       ├── auth/           # Authentification JWT, entités User/Student/Landlord/Admin
│       ├── common/         # BaseEntity, enums, exceptions, sécurité JWT
│       ├── config/         # SecurityConfig, CorsConfig, StorageConfig
│       ├── listing/        # Annonces, recherche, localisation, photos, avis
│       └── media/          # Traitement d'images (thumbnailator) + MinIO
├── frontend/
│   └── src/app/
│       ├── core/           # Services (auth, listing, api), guards, interceptor, models
│       ├── features/       # Pages : auth, home, listing, search
│       └── shared/         # Composants : navbar, footer, layout, listing-card, star-rating
├── docs/                   # Cahiers d'analyse/conception, backlog, diagrammes, maquettes
└── docker-compose.yml
```

## Base de données — Migrations Flyway

| Version | Description                                      | Statut    |
|---------|--------------------------------------------------|-----------|
| V1      | Schéma initial (structure complète en français)  | ✅ Appliqué |
| V2      | Renommage schéma → anglais (tables + colonnes + enums) | ✅ Appliqué |
| V3      | Seed admin par défaut                            | ✅ Appliqué |
| V4      | Correction hash BCrypt admin                     | ✅ Appliqué |

## Avancement Sprint 1

### Backend — Implémenté

| Module         | Contenu                                                                 |
|----------------|-------------------------------------------------------------------------|
| `common`       | BaseEntity, enums (UserRole, UserStatus, ListingStatus, RoomType, PaymentStatus), exceptions métier, ApiError |
| `auth`         | Entités User/Student/Landlord/Admin (SINGLE_TABLE), JWT (provider + filter + properties), AuthService, AuthController (7 endpoints) |
| `listing`      | Entités Listing/Location/Photo/Amenity/Review, ListingService, ListingSearchService (filtres multi-critères + pagination), ListingController |
| `admin`        | AdminListingService, AdminController (approve/reject)                   |
| `media`        | StorageService (MinIO), ImageProcessingService (multi-résolution)       |
| `config`       | SecurityConfig (JWT + routes), CorsConfig, StorageConfig               |

### Frontend — Implémenté

| Couche       | Contenu                                                                 |
|--------------|-------------------------------------------------------------------------|
| `core`       | AuthService (signals), ListingService, ApiService, AuthGuard, RoleGuard, AuthInterceptor, modèles TypeScript |
| `shared`     | Navbar, Footer, Layout, ListingCard, StarRating                        |
| `features`   | Login (email + téléphone), Register, Home, Search, ListingDetail, ListingCreate, MyListings |
| Routing      | Routes lazy-loaded avec guards auth + rôle                             |
| Design system| Tokens PrimeNG (palette orange + navy), Tailwind CSS 4, Poppins/Inter  |

### Tests — Stubs présents

- `AuthServiceTest`, `ListingServiceTest`, `ListingSearchServiceTest`, `AdminListingServiceTest`
- Tests d'intégration conditionnels (`SPRING_INTEGRATION_TESTS=true`)

## Endpoints API principaux

| Méthode | Route                              | Accès         |
|---------|------------------------------------|---------------|
| POST    | `/api/v1/auth/register/student`    | Public        |
| POST    | `/api/v1/auth/register/landlord`   | Public        |
| POST    | `/api/v1/auth/login`               | Public        |
| POST    | `/api/v1/auth/refresh`             | Public        |
| GET     | `/api/v1/auth/me`                  | Authentifié   |
| GET     | `/api/v1/listings/search`          | Public        |
| GET     | `/api/v1/listings/{id}`            | Public        |
| POST    | `/api/v1/listings`                 | Landlord      |
| GET     | `/api/v1/admin/listings/pending`   | Admin         |
| PUT     | `/api/v1/admin/listings/{id}/approve` | Admin      |
| PUT     | `/api/v1/admin/listings/{id}/reject`  | Admin      |

Documentation complète : http://localhost:8080/api/v1/swagger-ui.html
