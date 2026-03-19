# Sprint 3 — Implementation Checklist

> Updated at each commit. Format: `- [x] Task (commit: conventional message)`

---

## Phase 0 — Domain Foundation
- [x] 0.1 — Enum: SubscriptionStatus (ACTIVE, EXPIRED, CANCELLED, PENDING_PAYMENT) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.2 — Enum: RejectionMotif (INACCURATE_PHOTOS, MISLEADING_DESCRIPTION, INVALID_PRICE, INCOMPLETE_INFORMATION, DUPLICATE_LISTING) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.3 — Enum updates: PaymentType (+SUBSCRIPTION), NotificationType (+7 new values) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.4 — Entity: Subscription (landlord subscription, 100K FCFA/year) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.5 — Entity: NotificationPreference (per-type channel toggles) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.6 — Entity: ReviewReport (flagged reviews) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.7 — Entity: ListingView (view tracking per listing per day) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.8 — Entity update: User (+deactivated, +deactivationReason) (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.9 — Repositories: SubscriptionRepository, NotificationPreferenceRepository, ReviewReportRepository, ListingViewRepository (commit: feat(domain): add sprint 3 domain foundation)
- [x] 0.10 — Flyway V13: sprint 3 schema adjustments (listing_view, review_report tables, user columns, enum values) (commit: feat(domain): add sprint 3 domain foundation)

## Phase 1 — E8: Subscription System (US-801, US-803)

### Backend
- [x] 1.1 — Subscription DTOs & Mapper (SubscribeRequest, SubscriptionResponse, SubscriptionHistoryResponse) (commit: feat(subscription): add landlord subscription system)
- [x] 1.2 — SubscriptionService (subscribe, renew, cancel, check active, auto-deactivate expired, expiry reminders) (commit: feat(subscription): add landlord subscription system)
- [x] 1.3 — SubscriptionController (5 endpoints: POST subscribe, GET my, PUT renew, PUT cancel, GET history) (commit: feat(subscription): add landlord subscription system)
- [x] 1.4 — ListingService integration: subscription guard on createListing + maxListings check (commit: feat(subscription): add landlord subscription system)

### Frontend
- [x] 1.5 — SubscriptionService + subscription.models.ts (commit: feat(subscription): add frontend service and models)
- [x] 1.6 — Subscription Page (subscribe CTA, active status, renewal, cancel auto-renew) (commit: feat(subscription): add subscription page with 3 states and MoMo dialog)
- [x] 1.7 — Landlord Dashboard: subscription status widget (commit: feat(subscription): add subscription widget to landlord dashboard)
- [x] 1.8 — Listing Creation: subscription guard (redirect to /subscription if no active sub) (commit: feat(subscription): add subscription guard on listing creation)

## Phase 2 — E8: Enhanced Landlord Dashboard (US-802)

### Backend
- [x] 2.1 — ListingViewService (record view async, count by period, daily breakdown) (commit: feat(dashboard): add enhanced landlord dashboard with view tracking)
- [x] 2.2 — Enhanced DashboardService (KPIs + chart data + occupancy rate + period comparison) (commit: feat(dashboard): add enhanced landlord dashboard with view tracking)
- [x] 2.3 — Enhanced DashboardController (add period params, revenue-chart, views-chart endpoints) (commit: feat(dashboard): add enhanced landlord dashboard with view tracking)
- [x] 2.4 — ListingController: record view on getListingById (async) (commit: feat(dashboard): add enhanced landlord dashboard with view tracking)

### Frontend
- [x] 2.5 — Enhanced Landlord Dashboard (KPI cards with trends, period selector, revenue bar chart, views line chart, bookings pie chart, pending actions) (commit: feat(dashboard): add enhanced landlord dashboard with charts and KPIs)
- [x] 2.6 — Dashboard Service enhancement (chart data methods) (commit: feat(dashboard): add enhanced landlord dashboard with charts and KPIs)

## Phase 3 — E9: Admin Panel — Backend

- [x] 3.1 — AdminUserService (list with filters, detail with stats, deactivate, reactivate) (commit: feat(admin): add admin panel backend)
- [x] 3.2 — AdminReviewService (pending queue, flagged queue, approve, reject, reports, resolve) (commit: feat(admin): add admin panel backend)
- [x] 3.3 — AdminStatisticsService (global stats, monthly chart data, CSV export) (commit: feat(admin): add admin panel backend)
- [x] 3.4 — AdminTransactionService (list with filters, detail, anomalies detection) (commit: feat(admin): add admin panel backend)
- [x] 3.5 — Enhanced AdminController (new endpoints: users, reviews, stats, transactions) (commit: feat(admin): add admin panel backend)
- [x] 3.6 — ReviewController: add POST /reviews/{id}/report endpoint (commit: feat(admin): add admin panel backend)
- [x] 3.7 — Listing rejection enhancement: predefined motifs + free text (US-902) (commit: feat(admin): add admin panel backend)
- [x] 3.8 — Admin DTOs (AdminUserListResponse, AdminUserDetailResponse, AdminReviewResponse, GlobalStatsResponse, AdminTransactionResponse, etc.) (commit: feat(admin): add admin panel backend)

## Phase 4 — E9: Admin Panel — Frontend

- [x] 4.1 — Admin Layout component (sidebar + header + router-outlet) (commit: feat(admin): implement admin panel frontend)
- [x] 4.2 — Admin routes (lazy-loaded, ADMIN role guard) (commit: feat(admin): implement admin panel frontend)
- [x] 4.3 — Admin Service (all admin API methods) (commit: feat(admin): implement admin panel frontend)
- [x] 4.4 — Admin Dashboard page (6 KPIs, charts, quick actions, CSV export) (commit: feat(admin): implement admin panel frontend)
- [x] 4.5 — Admin Listings page (table, status tabs, search) (commit: feat(admin): implement admin panel frontend)
- [x] 4.6 — Admin Listing Detail page (read-only view, validation checklist, approve/reject actions) (commit: feat(admin): implement admin panel frontend)
- [x] 4.7 — Admin Users page (table, role/status filters, search, deactivate/reactivate) (commit: feat(admin): implement admin panel frontend)
- [x] 4.8 — Admin User Detail page (profile, stats, recent activity, deactivation section) (commit: feat(admin): implement admin panel frontend)
- [x] 4.9 — Admin Reviews page (tabs: pending/flagged/all, approve/reject actions) (commit: feat(admin): implement admin panel frontend)
- [x] 4.10 — Admin Transactions page (table, filters, anomalies alerts, color-coded status) (commit: feat(admin): implement admin panel frontend)
- [x] 4.11 — Navbar: admin link (visible for ADMIN role only) (commit: feat(admin): implement admin panel frontend)

## Phase 5 — E10: Multi-Channel Notifications (US-1001, US-1002)

### Backend
- [x] 5.1 — EmailNotificationService (Thymeleaf templates, send with retry x3) (commit: feat(notifications): add multi-channel notification system)
- [x] 5.2 — SmsNotificationService (Twilio integration, send with retry x3, disabled for dev) (commit: feat(notifications): add multi-channel notification system)
- [x] 5.3 — NotificationDispatcher (orchestrate in-app + email + SMS, critical override, async sends) (commit: feat(notifications): add multi-channel notification system)
- [x] 5.4 — NotificationPreferenceService (get, update, create defaults, critical type guards) (commit: feat(notifications): add multi-channel notification system)
- [x] 5.5 — NotificationPreferenceController (GET preferences, PUT preference by type) (commit: feat(notifications): add multi-channel notification system)
- [x] 5.6 — Integrate dispatcher across services (BookingService, PaymentService, ReviewService, AdminListingService, AdminUserService, AdminReviewService, SubscriptionService) (commit: feat(notifications): add multi-channel notification system)

### Frontend
- [x] 5.7 — Notification Preferences page (type list, email/SMS toggles, critical type indicators, auto-save) (commit: merge(sprint3): integrate multi-channel notifications and preferences)
- [x] 5.8 — Profile page: link to notification preferences (commit: merge(sprint3): integrate multi-channel notifications and preferences)

## Phase 6 — Testing & Quality Assurance

### Backend Unit Tests
- [ ] 6.1 — SubscriptionServiceTest (~15 tests)
- [ ] 6.2 — AdminUserServiceTest (~10 tests)
- [ ] 6.3 — AdminReviewServiceTest (~10 tests)
- [ ] 6.4 — AdminStatisticsServiceTest (~8 tests)
- [ ] 6.5 — AdminTransactionServiceTest (~8 tests)
- [ ] 6.6 — NotificationDispatcherTest (~12 tests)
- [ ] 6.7 — NotificationPreferenceServiceTest (~8 tests)
- [ ] 6.8 — EmailNotificationServiceTest (~5 tests)
- [ ] 6.9 — SmsNotificationServiceTest (~5 tests)
- [ ] 6.10 — ListingViewServiceTest (~5 tests)

### Backend Controller Tests
- [ ] 6.11 — SubscriptionControllerTest (~8 tests)
- [x] 6.12 — AdminController extended tests — existing tests fixed for Sprint 3 APIs (commit: fix(tests): update existing tests to use Sprint 3 refactored service APIs)
- [ ] 6.13 — NotificationPreferenceControllerTest (~5 tests)

### Frontend Component Tests
- [x] 6.14 — Admin Dashboard component test (commit: feat(admin): implement admin panel frontend)
- [x] 6.15 — Admin Listings page component test (commit: feat(admin): implement admin panel frontend)
- [x] 6.16 — Admin Users page component test (commit: feat(admin): implement admin panel frontend)
- [x] 6.17 — Admin Reviews page component test (commit: feat(admin): implement admin panel frontend)
- [x] 6.18 — Admin Transactions page component test (commit: feat(admin): implement admin panel frontend)
- [x] 6.19 — Subscription page component test (commit: feat(subscription): add frontend service and models)
- [ ] 6.20 — Notification Preferences page component test
- [ ] 6.21 — Landlord Dashboard enhanced component test

### E2E Tests (Playwright)
- [ ] 6.22 — Admin listing validation E2E
- [ ] 6.23 — Admin listing rejection E2E
- [ ] 6.24 — Admin user management E2E
- [ ] 6.25 — Admin review moderation E2E
- [ ] 6.26 — Subscription flow E2E
- [ ] 6.27 — Notification preferences E2E

## Phase 7 — Sprint 2 Remaining Tests (Backlog)

### Frontend Component Tests
- [ ] 7.1 — BookingConfirmDialog + MyBookings component tests
- [ ] 7.2 — PaymentFlow + Receipt component tests
- [ ] 7.3 — ReviewForm + ReviewList component tests
- [ ] 7.4 — NotificationBell component tests

### E2E Tests
- [ ] 7.5 — Auth E2E (login/logout flows)
- [ ] 7.6 — Bookings E2E (student + landlord flows)
- [ ] 7.7 — Payment E2E (payment form, processing, success/failure)
- [ ] 7.8 — Transactions E2E (history page)

---

## Progress Summary

| Phase | Total | Done | % |
|-------|:-----:|:----:|:-:|
| 0 — Domain Foundation | 10 | 10 | 100% |
| 1 — Subscription System | 8 | 8 | 100% |
| 2 — Enhanced Dashboard | 6 | 6 | 100% |
| 3 — Admin Backend | 8 | 8 | 100% |
| 4 — Admin Frontend | 11 | 11 | 100% |
| 5 — Notifications | 8 | 8 | 100% |
| 6 — Testing | 27 | 8 | 30% |
| 7 — Sprint 2 Backlog | 8 | 0 | 0% |
| **Total** | **86** | **59** | **69%** |
