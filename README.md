# Ndobaâk

A student housing platform for the Eyang area, around a university campus in Yaoundé, Cameroon. Students search for housing on a map, book it and pay by mobile money. Landlords publish listings and follow the whole rental cycle, from the application to handing over the keys.

My role: project lead and sole developer.

Status: built, not deployed.

## Features

For students:

- Search with filters, as a paged list or on a map (Leaflet and OpenStreetMap)
- Booking requests, accepted or declined by the landlord
- Mobile money payment through NotchPay
- Tenant application file, lease and move-in inspection
- Reviews with photos

For landlords:

- Listings created in several steps, with 3 to 10 photos resized to three sizes
- Identity and land title checks
- Booking management and a dashboard with their figures
- The rental cycle: application, lease, inspection, key handover

For admins: moderation of listings and accounts, finances and a dashboard.

Notifications go in-app, by email or by SMS, depending on how urgent the event is.

## Stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 21, PrimeNG 21, Tailwind CSS 4, Bun |
| Backend | Spring Boot 3.5, Java 21, Maven |
| Database | PostgreSQL, Flyway migrations |
| File storage | MinIO locally, AWS S3 in production |
| Authentication | Stateless JWT, Google OAuth2 |
| Payments | NotchPay (a mock gateway in development) |
| SMS | Twilio |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| CI/CD | GitLab CI, Docker images on AWS ECR |

Booking states change in one place, `BookingStateMachine`. Long tasks such as document checks and image processing answer `202 Accepted` and notify the result later. Every API error returns `{ code, message, retryable }`.

## How to run it locally

You need Docker with Docker Compose, Bun 1.3 or later, and Java 21 if you want to run the backend outside Docker.

Start the backend with PostgreSQL, pgAdmin, Mailpit and MinIO:

```bash
docker compose up -d
```

With the default values from `docker-compose.yml`, payments go through the mock gateway and Google sign-in is off. Flyway runs the migrations when the backend starts.

Start the frontend:

```bash
cd frontend
bun install
bun start
```

The app runs on http://localhost:4200 and sends `/api` calls to `localhost:8080`.

| Service | URL |
| --- | --- |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| pgAdmin | http://localhost:5050 |
| Mailpit (catches all emails) | http://localhost:1080 |
| MinIO console | http://localhost:9001 |

Migrations create local test accounts for each role (admin, student, landlord). You can find them in the Flyway migrations under `backend/src/main/resources/db/migration`.

The `Makefile` has shortcuts: `make infra` starts only PostgreSQL, Mailpit and MinIO, and `make backend` then runs the backend with Maven.

## Tests

```bash
cd backend && ./mvnw test          # unit and integration tests
cd frontend && bun run test        # frontend unit tests
cd frontend && bunx playwright test   # end-to-end tests, with backend and frontend running
```

## Repository layout

```
backend/src/main/java/com/ndobaak/backend/
  admin/  auth/  booking/  dashboard/  geo/  listing/  media/
  notification/  payment/  rentalcycle/  review/  subscription/
frontend/src/app/
  core/  features/  shared/
frontend/e2e/        Playwright scenarios
docs/                analysis and design documents, diagrams (in French)
```

The code is in English. The user interface is in French.
