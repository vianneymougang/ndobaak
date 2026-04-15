# Backend — Rules

Expert in Java 21, Spring Boot 3.5, and production-grade API design. Write clean, maintainable, and secure backend code following Spring Boot and Java best practices.

## Java & Spring Boot

- Use Java 21 features where appropriate: records for DTOs, sealed classes, pattern matching, text blocks.
- Prefer constructor injection via `@RequiredArgsConstructor` (Lombok) over field injection.
- Use `@Transactional` at the service layer, never at the repository layer.
- Validation with Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.) on all incoming DTOs.
- Never expose JPA entities directly in API responses — always map to DTOs.

## Architecture

Package structure by feature slice: `auth`, `listing`, `booking`, `media`, `notification`, `review`, `admin`, `geo`, `payment`, `dashboard`, `common`.

Each feature contains: controller, service, repository, domain (entities + enums), dto (request + response). Shared utilities go in `common`.

## API Design

### Response structure

All error responses:
```json
{
  "code": "SNAKE_CASE_ERROR_CODE",
  "message": "Message en français affiché directement à l'utilisateur.",
  "retryable": false
}
```

HTTP status codes used semantically: 200, 201, 202, 400, 401, 403, 404, 409, 422, 500.

### Pagination

Every list endpoint is paginated. Default page size: 20. Maximum: 50. Use Spring Data's `Pageable`.

### Payload discipline

- List routes return summary DTOs (id, title, price, thumbnail, status, key metadata only).
- Detail routes (`/{id}`) return full DTOs.
- Use `@JsonInclude(NON_NULL)` on response DTOs.

### Async processing

Long operations (document verification, image resizing) must be asynchronous:
- Return `202 Accepted` immediately.
- Process in background (`@Async` or event-driven).
- Notify via notification system.

### HTTP caching

- Static assets (MinIO): `Cache-Control: public, max-age=31536000, immutable`
- Semi-static (thumbnails, avatars): `Cache-Control: public, max-age=3600`
- User-specific or mutable: `Cache-Control: no-store`
- Set `ETag` on listing detail responses.

## File Upload & Media

- Validate MIME type (not just extension) and size before processing.
- Accepted formats: JPEG, PNG, WebP. Max size: 10MB.
- Always process images asynchronously: thumbnail + medium + original.
- Return `202 Accepted` immediately — notify when processing is complete.

## Security

- JWT authentication — stateless.
- Role-based access control: `STUDENT`, `LANDLORD`, `ADMIN`.
- Never expose internal IDs in predictable sequences — use UUIDs for public-facing identifiers.
- Sanitize all user-supplied strings before persisting.
- Rate-limit sensitive endpoints: authentication, document submission, contact requests.
- Swagger/OpenAPI endpoints whitelisted in `SecurityConfig` — not accessible in production profile.

## Error Handling

- Global exception handler via `@RestControllerAdvice`.
- Log the full exception server-side (with correlation ID); return only the user-facing message to the client.
- Use a correlation ID (`X-Correlation-ID`) on all requests for traceability.

## Database

- Flyway for all schema migrations — never modify applied migrations.
- New migration files: `V{n}__{description}.sql` — sequential, atomic.
- Never use `schema.sql` or `data.sql` in production profile.
- Use `SINGLE_TABLE` inheritance for the `User` hierarchy (Student, Landlord, Admin).
- Index all foreign keys and frequently-filtered columns (status, availability, city).

## Notification Service

- In-app notifications are persistent (stored in DB, retrieved via API).
- Email via Mailtrap (dev) / SMTP provider (prod).
- SMS via a provider supporting Cameroon numbers.
- Never send SMS for non-critical events.
- All notification sends are fire-and-forget and asynchronous — never block the main request thread.
