# Service Architecture

## Service boundaries

## Application layering

Both `oauth` and `user` use the same application-layer direction:

```text
controller -> service interface -> service/impl -> mapper or client
```

- `controller` handles HTTP input and output only.
- `service` contains interfaces prefixed with `I`.
- `service/impl` contains Spring service implementations.
- `mapper` contains persistence interfaces and is used only by service
  implementations.
- `common` is excluded from this MVC rule because it only provides shared
  contracts and web utilities.

The User service uses MyBatis-Plus for `user_account` and `user_identity`.
Its entities are mapped with `@TableName` and `@TableId`, and its mappers
extend `BaseMapper`. The OAuth service keeps Spring Authorization Server's
official JDBC persistence implementation for OAuth protocol tables.

### OAuth service

The OAuth service owns the authorization protocol:

- OAuth2 authorization and consent endpoints.
- Authorization code, access token, refresh token, and OIDC ID token issuance.
- Browser form login.
- Optional third-party OAuth2 client callbacks.
- Compatibility facade for the existing `/api/third-auth/*` and `/api/user/me` routes.

It does not own user accounts or third-party identity data.

### User service

The User service owns user data and user-domain behavior:

- Password accounts and profiles.
- Roles and account status.
- Third-party provider identities.
- Provider access tokens and refresh tokens.
- First-time social registration, identity binding, unbinding, and token status.
- Resource-server protected user APIs.

OAuth calls the internal User API with `X-Internal-Token`. The internal endpoints
are not public APIs and should be additionally protected by network policy in
production.

### Common module

The Common module contains only cross-service contracts and small shared web
utilities. It must not contain persistence code or service-specific business
logic.

## Request flows

### Password login

1. A browser submits credentials to OAuth `/login`.
2. OAuth `RemoteUserDetailsService` requests password and authorities from User.
3. User validates the account data through its own database.
4. OAuth creates the authenticated browser session.

The session principal is Spring Security's standard `User` type. This is
intentional: authorization records can be safely serialized by Spring Security's
JDBC authorization service.

### Authorization code

1. A client calls `/oauth2/authorize`.
2. OAuth authenticates the browser and stores the authorization transaction.
3. The user grants consent.
4. OAuth redirects the code to the registered client.
5. The client exchanges the code at `/oauth2/token`.
6. OIDC `/userinfo` reads the username from the access token and requests the
   profile from User.

### Third-party login

1. OAuth handles the provider callback and extracts the provider identity.
2. OAuth sends the identity snapshot and provider tokens to User.
3. User finds or creates the local account and identity binding.
4. OAuth creates the local browser session.

## Database ownership

### OAuth database: `super_star_oauth`

Only Spring Authorization Server persistence tables are stored here:

- `oauth2_registered_client`: OAuth client registrations and client settings.
- `oauth2_authorization`: authorization transactions and issued tokens.
- `oauth2_authorization_consent`: user consent records.

### User database: `super_star_user`

- `user_account`: local account, profile, password, roles, and status.
- `user_identity`: provider identity, binding, token snapshot, and profile snapshot.

The names are deliberately domain-oriented and do not use the ambiguous `sys_*`
prefix.

## Production considerations

- Use a secret manager for database credentials and `USER_INTERNAL_TOKEN`.
- Persist or externally manage the OAuth RSA signing key. The current local
  fallback generates an in-memory key on each startup, so old JWTs are invalid
  after a restart.
- Encrypt provider access and refresh tokens at rest.
- Put the internal User API behind service-network authorization.
- Use a real schema migration tool or run the supplied migration script before
  switching an existing deployment to the split databases.
