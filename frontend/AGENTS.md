# Frontend — Rules

Expert in TypeScript, Angular 21, and scalable web application development. Write functional, maintainable, performant, and accessible code.

## TypeScript

- Use strict type checking.
- Prefer type inference when the type is obvious.
- Avoid `any`; use `unknown` when type is uncertain.

## Angular

- Always use standalone components — do NOT set `standalone: true` (implicit in Angular v20+).
- Use signals for state management, `computed()` for derived state.
- Implement lazy loading for feature routes.
- Do NOT use `@HostBinding` / `@HostListener` — use the `host` object in `@Component` / `@Directive` instead.
- Use `NgOptimizedImage` for all static images (does not work for inline base64).
- Set `changeDetection: ChangeDetectionStrategy.OnPush` on every component.
- Use `input()` and `output()` functions instead of decorators.
- Do NOT use `ngClass` — use `class` bindings. Do NOT use `ngStyle` — use `style` bindings.
- Use native control flow (`@if`, `@for`, `@switch`) instead of `*ngIf`, `*ngFor`, `*ngSwitch`.
- Do not write arrow functions in templates.
- Use `inject()` function instead of constructor injection in services.
- Services: `providedIn: 'root'` for singletons.
- Prefer Reactive forms over template-driven forms.
- Do NOT use `mutate` on signals — use `update` or `set`.

## Angular CLI — Code Generation

**Absolute rule**: Never create Angular artifacts manually. Always use `ng generate` (or `ng g`).

| Artifact | Command |
|---|---|
| Component | `ng g component path/name` |
| Service | `ng g service path/name` |
| Directive | `ng g directive path/name` |
| Pipe | `ng g pipe path/name` |
| Guard | `ng g guard path/name` |
| Interceptor | `ng g interceptor path/name` |
| Interface | `ng g interface path/name` |
| Enum | `ng g enum path/name` |

Always specify the full feature path: `ng g component features/listing/listing-card` — not just `listing-card`.

## Accessibility

- Must pass all AXE checks.
- Must follow WCAG AA minimums: focus management, color contrast, ARIA attributes.

## PrimeNG — Component-First Policy

Before building any UI element, always verify whether PrimeNG already provides it. Custom code is the last resort.

### Decision hierarchy

1. PrimeNG native — use as-is.
2. PrimeNG configured — use props and template slots.
3. PrimeNG styled — use Pass Through (`pt`) to reach internal elements. Never override via external CSS selectors targeting `.p-*` class names.
4. PrimeNG wrapped — wrap components to encapsulate a recurring combination.
5. Custom — only when PrimeNG provably has no relevant component.

### Theming

- Use PrimeNG design tokens (CSS custom properties) for all overrides — never hardcode colors or spacing that duplicate a token.
- Use `pt` (Pass Through) API for styling internal elements.
- Use PrimeIcons exclusively for iconography within PrimeNG components.

### Forms with PrimeNG

- PrimeNG form controls bind directly to Angular Reactive Forms via `formControlName`.
- Use `p-floatlabel` and `p-iftalabel` for label positioning.
- For global/API errors: use `p-message`. For field-level validation: use `<small class="field-error">` — never `p-message` for individual fields.

## Forms — Validation UX

- When all fields are required: add a single legend `<p class="form-legend">* Champs obligatoires</p>` — no asterisk on each field.
- When mixed required/optional: mark optional fields with `(facultatif)` in the label — never use asterisks.
- Bind `[invalid]="ctrl.invalid && ctrl.touched"` on every PrimeNG input.
- Show `<small class="field-error">` only when `ctrl.invalid && ctrl.touched`.
- Error messages must be specific: `"Email invalide"`, `"Minimum 8 caractères"` — never `"Champ invalide"`.
- On submit with invalid form: call `form.markAllAsTouched()`, then scroll to first invalid field.
- Form containers with `p-floatlabel`: minimum `gap-7` between fields, `pt-3` on the container.

## Connectivity-Aware Design

Poor connectivity is a first-class constraint.

- Use `p-skeleton` for all content taking >300ms to load.
- Optimistic UI updates: reflect actions immediately, rollback on server error.
- Detect offline state; show a non-intrusive banner: "Vous êtes hors ligne — certaines fonctionnalités sont indisponibles."
- Never show a blank page — always provide a meaningful fallback with a retry affordance.
- Persist all form drafts continuously to localStorage/IndexedDB.
- Always show upload progress as a percentage (`p-progressbar`) — never an indeterminate spinner.
- Friendly French error messages: what happened + what to do: "Impossible de charger les annonces. Vérifiez votre connexion puis réessayez."

## Multi-Step Forms (Airbnb Pattern)

- Each step occupies the full screen.
- Persistent linear progress bar at the top.
- Primary CTA fixed at the bottom, always visible, disabled until current step is valid.
- Transitions: directional slide (forward = left, back = right) using Angular animations.
- Back navigation always available.
- All form state persisted to localStorage/IndexedDB.
- Summary/review step before final submission.
- Use `p-stepper` for navigation state management.

## Notifications & Overlays

- `p-toast` via `MessageService` for transient feedback — auto-dismiss in 4s.
- Single `<p-toast>` in the root layout.
- For modal workflows: use `DialogService` from `primeng/dynamicdialog`.

## Animations

- Purposeful only — no animation for aesthetics alone.
- Page transitions: directional slide, 300–400ms.
- Micro-interactions: 200–300ms.
- Always respect `prefers-reduced-motion`.
- Prefer CSS transitions over JS-driven animations.

## ng-motion — Animation Library

Use `@scripttype/ng-motion` for all non-trivial animations. It wraps `motion-dom` (Framer Motion engine) in Angular directives and signals.

### Core directive

```ts
import { NgmMotionDirective } from '@scripttype/ng-motion';

// In imports array, then in template:
<div ngmMotion
  [initial]="{ opacity: 0, y: 24 }"
  [animate]="{ opacity: 1, y: 0 }"
  [transition]="{ type: 'spring', stiffness: 280, damping: 24 }"
>
```

### Exit animations

`[exit]` works automatically with `@if` / `@for` — no wrapper needed:
```html
@if (visible()) {
  <div ngmMotion
    [initial]="{ opacity: 0 }"
    [animate]="{ opacity: 1 }"
    [exit]="{ opacity: 0 }"
    [transition]="{ type: 'spring', stiffness: 280, damping: 24 }"
  >
}
```

Add `height` to `[exit]` only when surrounding items need to collapse (list items, accordions) — it causes layout reflow. Omit it for modals/panels (GPU compositor, zero layout cost).

### Reactivity

Directive inputs accept plain values — Angular reactivity comes from the binding:
```html
[animate]="open() ? 'open' : 'closed'"
[layoutDependency]="expanded()"
```

### Motion values (high-frequency state)

```ts
import { useMotionValue, useTransform, useSpring, useScroll } from '@scripttype/ng-motion';

readonly x = useMotionValue(0);
readonly opacity = useTransform(this.x, [-200, 0, 200], [0.2, 1, 0.2]);
readonly motionStyle: MotionStyle = { x: this.x, opacity: this.opacity };
// Template: <div ngmMotion [style]="motionStyle">
```

All hooks must run in an Angular injection context (field initializer or constructor).

### Route transitions

```ts
import { withMotionTransitions } from '@scripttype/ng-motion';
provideRouter(routes, withMotionTransitions('slide-left'))
// Presets: 'fade' | 'slide-left' | 'slide-right' | 'slide-up' | 'slide-down' | 'scale'
```

### Performance rules

- Never use `[animate]` with `repeat: Infinity` for decorative animations — use `[whileInView]` instead (pauses when off-screen).
- Do NOT mirror MotionValues into Angular signals on every frame — keep hot paths in MotionValue land.
- Do NOT add `will-change: transform` manually — WAAPI handles GPU promotion automatically.
- Always clean up `setTimeout`/`setInterval` via `DestroyRef.onDestroy()`.

## Visual Standards

- Spacing: 4px grid strictly.
- Images: always with defined aspect ratio, skeleton placeholder while loading, fallback on error.
- Typography hierarchy: maximum 3 levels in a single view.
- One primary action color per screen.
