DOPPLER_PROJECT := ndobaak
DOPPLER_CONFIG  := dev

.PHONY: dev dev-build dev-down backend-reload dev-logs dev-ps infra infra-down backend

dev:
	doppler run --project $(DOPPLER_PROJECT) --config $(DOPPLER_CONFIG) -- docker compose up

dev-build:
	doppler run --project $(DOPPLER_PROJECT) --config $(DOPPLER_CONFIG) -- docker compose up --build

dev-down:
	docker compose down

backend-reload:
	docker compose build backend
	doppler run --project $(DOPPLER_PROJECT) --config $(DOPPLER_CONFIG) -- docker compose up -d --no-deps backend

dev-logs:
	docker compose logs -f backend

dev-ps:
	docker compose ps

infra:
	docker compose up postgres mailtrap minio -d

infra-down:
	docker compose stop postgres mailtrap minio

backend:
	cd backend && doppler run -- ./mvnw spring-boot:run
