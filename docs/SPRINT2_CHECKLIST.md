# Sprint 2 — Implementation Checklist

> Updated at each commit. Format: `- [x] Task (commit: conventional message)`

---

## Phase 0 — Domain Foundation
- [x] 0.1 — Enums: BookingStatus, PaymentStatus, PaymentMethod, MobileMoneyOperator, PaymentType, ScheduleFrequency, InstallmentStatus, ReviewStatus, NotificationType, NotificationChannel
- [x] 0.2 — Entity: Booking (maps to existing `booking` table)
- [x] 0.3 — Entities: Payment, PaymentSchedule, Installment (maps to existing tables)
- [x] 0.4 — Entity: Review update (add status, booking reference, photos)
- [x] 0.5 — Entity: Notification
- [x] 0.6 — Repositories: BookingRepository, PaymentRepository, PaymentScheduleRepository, InstallmentRepository, NotificationRepository
- [x] 0.7 — Flyway V7: schema adjustments (missing columns, indexes)

## Phase 1 — E5: Reservation System

### Backend
- [x] 1.1 — Booking DTOs & Mapper (CreateBookingRequest, BookingResponse, BookingStatusUpdateRequest)
- [x] 1.2 — BookingService (create, confirm, reject, cancel, list by student/landlord)
- [x] 1.3 — BookingController (7 endpoints: POST create, GET by id, GET my, GET landlord, PUT confirm, PUT reject, PUT cancel)

### Frontend
- [x] 1.4 — BookingService + booking models (core/services + core/models)
- [x] 1.5 — Listing Detail: booking panel (date picker, price calc, reserve button)
- [x] 1.6 — My Bookings page — student view (status tabs, card list, actions)
- [x] 1.7 — Landlord Bookings page (pending requests, confirm/reject actions)
- [x] 1.8 — Booking Confirmation Dialog (summary modal, terms checkbox)
- [x] 1.9 — Navigation updates (My Bookings / Bookings links, route guards)

## Phase 2 — E6: Mobile Money Payment

### Backend
- [x] 2.1 — Payment DTOs & Mapper (InitiatePaymentRequest, PaymentResponse, ReceiptResponse, etc.)
- [x] 2.2 — PaymentGateway interface + MockPaymentGateway + PaymentGatewayRouter (multi-provider abstraction)
- [x] 2.3 — PaymentService (initiate, webhook, installments, receipt, transaction history)
- [x] 2.4 — PaymentController (7 endpoints: POST initiate, POST webhook/{provider}, GET by booking, GET history, GET receipt, POST pay installment, GET schedule)
- [x] 2.5 — NotchPayGateway (RestClient, HMAC-SHA256 webhook, operator channel mapping)
- [x] 2.6 — Webhook security (signature verification, idempotent processing, provider-specific endpoints)

### Frontend
- [x] 2.7 — Payment Flow page (payment type selector, operator selector, phone input, price breakdown)
- [x] 2.8 — Payment Success/Failure pages
- [x] 2.9 — Digital Receipt page (receipt details, download PDF, share)
- [x] 2.10 — Transaction History page (paginated list, status filters)
- [x] 2.11 — Installment Payment Tracking (progress bar, schedule table, pay buttons)

## Phase 3 — E7: Reviews & Ratings

### Backend
- [x] 3.1 — Review DTOs & Mapper (CreateReviewRequest, ReviewResponse, RatingStatsResponse, ReviewMapper)
- [x] 3.2 — ReviewService (create with booking check, list, rating stats, delete)
- [x] 3.3 — ReviewController (5 endpoints: POST create, GET by listing, GET my, DELETE, GET rating-stats)

### Frontend
- [x] 3.4 — Review List component (rating summary header, distribution bars, review cards, photos)
- [x] 3.5 — Review Form component (star selector, textarea, photo upload)
- [x] 3.6 — Listing Detail integration (reviews section, write review button)

## Phase 4 — E4: Enhanced Geolocation

### Backend
- [x] 4.1 — Distance calculation endpoint (Haversine formula, nearby listings query)
- [x] 4.2 — Distance field in search results DTO

### Frontend
- [x] 4.3 — GeolocationService (browser geolocation, caching, fallback to IUSJC)
- [x] 4.4 — Distance display in search results ("X km from you" on listing cards)
- [x] 4.5 — Radius search on map (radius selector, circle overlay, "search this area")

## Phase 5 — Notification System & Dashboard

### Backend
- [ ] 5.1 — NotificationService (create, list, unread count, mark read)
- [ ] 5.2 — NotificationController (4 endpoints: GET list, GET unread-count, PUT read, PUT read-all)

### Frontend
- [ ] 5.3 — Notification Bell component (navbar icon, badge count, dropdown panel)
- [ ] 5.4 — Notifications Page (full paginated list, deep links)
- [ ] 5.5 — Landlord Dashboard page (KPI cards, pending actions, recent activity)
- [ ] 5.6 — Navigation & routing updates (final route structure, role-based menus)

## Phase 6 — Testing & Integration

### Backend Tests
- [x] 6.1 — BookingService unit tests (23 tests: create, confirm, reject, cancel, list, access control)
- [x] 6.2 — PaymentService unit tests (23 tests: initiate integral/installment, webhook, receipt, history, schedule)
- [x] 6.3 — ReviewService unit tests (17 tests: create, photos, booking check, delete, rating stats)
- [x] 6.4 — NotificationService unit tests (8 tests: create, list, mark read, unread count, access control)
- [x] 6.5 — BookingController WebMvc tests (14 tests: all 7 endpoints, validation, auth, error cases)
- [x] 6.6 — PaymentController WebMvc tests (13 tests: all 7 endpoints, webhook, validation, error cases)
- [x] 6.7 — ReviewController WebMvc tests (11 tests: all 5 endpoints, multipart, validation, error cases)
- [x] 6.8 — NotificationController WebMvc tests (6 tests: list, pagination, unread count, mark read)
- [x] 6.9 — GeoService unit tests (8 tests: mapping, ratings, null thumbnail, validation)
- [x] 6.10 — GeoController WebMvc tests (5 tests: happy path, defaults, missing/invalid params)
- [x] 6.11 — ListingSearchService distanceFromUser tests (3 tests: with/without coords, maxDistance filter)

### Frontend Tests
- [ ] 6.12 — BookingConfirmDialog + MyBookings component tests
- [ ] 6.13 — PaymentFlow + Receipt component tests
- [ ] 6.14 — ReviewForm + ReviewList component tests
- [ ] 6.15 — NotificationBell component tests

### End-to-End
- [x] 6.16 — Search page E2E (8 tests: filter bar, map, results, keyword, geolocation, radius, search area, distance)
- [x] 6.17 — Listing detail page E2E (3 tests: navigation, map tiles, reviews section)
- [ ] 6.18 — Auth E2E (login/logout flows)
- [ ] 6.19 — Bookings E2E (student + landlord flows)
- [ ] 6.20 — Payment E2E (payment form, processing, success/failure)
- [ ] 6.21 — Transactions E2E (history page)

---

## Issues & Blockers
_(tracked during implementation)_

---

## Progress Summary
- **Total tasks**: 53
- **Completed**: 43
- **Remaining**: 10

### Test Summary
| Suite | Tests | Status |
|---|---|---|
| Backend Unit (BookingService) | 23 | Passing |
| Backend Unit (PaymentService) | 23 | Passing |
| Backend Unit (ReviewService) | 17 | Passing |
| Backend Unit (NotificationService) | 8 | Passing |
| Backend Controller (BookingController) | 14 | Passing |
| Backend Controller (PaymentController) | 13 | Passing |
| Backend Controller (ReviewController) | 11 | Passing |
| Backend Controller (NotificationController) | 6 | Passing |
| Backend Unit (GeoService) | 8 | Passing |
| Backend Controller (GeoController) | 5 | Passing |
| Backend Unit (ListingSearchService complement) | 3 | Passing |
| Frontend E2E (Search) | 8 | Passing |
| Frontend E2E (Detail) | 3 | Passing |
| **Backend Total** | **198** | **Passing** |
| **Frontend E2E Total** | **11** | **Passing** |
