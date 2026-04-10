# Ndobaak — Transversal Rules

## Product Vision

Ndobaak is a student housing platform for the Eyang area (IUSJC, Yaoundé). Brand identity is warm, human, and premium. Premium does not mean cold or corporate — it means polished, reliable, and kind.

Trust is not assumed: it is built through every detail. The platform is backed by a licensed real estate agency (TANK'IMMO SAS) that legitimizes activities, provides professional support, handles disputes, and assists users in complex cases.

Two personas deserve equal craft:
- Student: discovery, search, and booking must be fluid, guided, and reassuring.
- Landlord: onboarding, listing management, and reservation tracking must be equally polished.

## Agency Partner Presence

Discreet but visible: a sober mention in the footer and About page. No co-branding, no logo. Message: "Activités encadrées par TANK'IMMO SAS, agence immobilière agréée."

## API ↔ Frontend Contract

### Error responses

```json
{
  "code": "LISTING_NOT_FOUND",
  "message": "Ce logement n'existe pas ou n'est plus disponible.",
  "retryable": false
}
```

- `message` always in French, always user-facing, never technical
- `retryable: true` signals the frontend that an automatic retry is appropriate
- Never include stack traces, Java class names, or SQL in error responses

### Pagination

All list endpoints are paginated — no unbounded lists ever.

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8,
  "hasNext": true
}
```

### Payload discipline

- List routes return summary DTOs only
- Detail routes (`/{id}`) return full DTOs
- Long operations (document verification, image processing) are async — respond `202 Accepted` immediately, notify via notification system when done

### Timeouts

- File upload endpoints: minimum 30s
- Standard API endpoints: respond in <2s under normal load

## Account Verification Workflow

### Landlord

States: `PENDING_VERIFICATION` → `VERIFIED` | `REJECTED`

1. Landlord registers and submits documents (ID + land title)
2. Admin notified via in-app + email
3. Admin verifies
4. Result: `VERIFIED` or `REJECTED` with reason
5. Landlord notified via in-app + email + SMS

A rejected landlord can resubmit — always include rejection reason.

### Student

States: `UNVERIFIED` → `PENDING_VERIFICATION` → `VERIFIED`

Verification triggered at first contact/booking request — not at registration.

## Notification Strategy

| Event | In-app | Email | SMS |
|---|:---:|:---:|:---:|
| Account verified / rejected | ✓ | ✓ | ✓ |
| New booking request | ✓ | ✓ | — |
| Booking confirmed | ✓ | ✓ | ✓ |
| New message received | ✓ | — | — |
| Document submitted (confirmation) | ✓ | ✓ | — |
| Booking reminder (D-1) | ✓ | ✓ | ✓ |

- In-app notifications are persistent (notification center)
- Toasts are ephemeral (4s auto-dismiss)
- Never send SMS for non-critical events

## Language

- Source code (classes, methods, variables, constants, file names): English exclusively
- User-facing messages (API responses, frontend): French exclusively
- Business terms: Annonce → `Listing`, Propriétaire → `Landlord`, Étudiant → `Student`, Réservation → `Booking`
