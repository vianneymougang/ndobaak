# ADR: Migration JWT vers HttpOnly Cookies

## Statut

Proposé — planifié pour une phase ultérieure.

## Contexte

Les tokens JWT sont actuellement stockés dans `localStorage` côté frontend (`frontend/src/app/core/services/auth.ts`, lignes 91-97). Ce stockage est vulnérable aux attaques XSS : tout script injecté dans la page peut lire et exfiltrer les tokens.

## Décision

Migrer vers un stockage HttpOnly Cookie pour les tokens JWT.

## Changements requis

### Backend

1. **Login/Refresh endpoints** : retourner les tokens via `Set-Cookie` au lieu du body JSON.
   - `Set-Cookie: access_token=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=900`
   - `Set-Cookie: refresh_token=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=604800`

2. **JwtAuthenticationFilter** : lire le token depuis le cookie `access_token` au lieu du header `Authorization: Bearer`.

3. **CSRF protection** : activer la protection CSRF Spring Security avec token synchronisé.
   - Endpoint `GET /api/v1/auth/csrf` retournant le token CSRF.
   - Le frontend l'envoie dans un header `X-XSRF-TOKEN` à chaque requête mutante.

4. **Logout** : endpoint qui invalide les cookies (`Max-Age=0`).

### Frontend

1. **AuthService** : supprimer tout accès à `localStorage` pour les tokens.
2. **HTTP Interceptor** : supprimer l'ajout du header `Authorization`. Le cookie est envoyé automatiquement.
3. **CSRF Interceptor** : ajouter un intercepteur qui lit le CSRF token depuis un cookie `XSRF-TOKEN` et l'envoie en header.
4. **Credentials** : ajouter `withCredentials: true` sur toutes les requêtes API.

### Risques et considérations

- **CSRF** : obligatoire dès qu'on utilise des cookies pour l'auth. Bien tester les routes mutantes.
- **Cross-origin** : si le frontend et le backend sont sur des domaines différents, `SameSite=Strict` bloquera les cookies. Adapter selon l'infrastructure de déploiement.
- **Refresh flow** : le `Path` restrictif sur le refresh token empêche son envoi sur les routes normales.
- **Tests** : tous les tests d'intégration doivent être adaptés pour vérifier les cookies au lieu des headers.

## Conséquences

- Les tokens ne sont plus accessibles par JavaScript (protection XSS complète).
- La surface d'attaque CSRF est gérée par le token synchronisé.
- Le flux de refresh token est plus robuste (cookie isolé par path).
