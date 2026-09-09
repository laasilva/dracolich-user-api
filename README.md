# dracolich-user-api

Authentication and user management service for the Dracolich platform. Built with Spring Boot 4 (WebFlux), MongoDB, and JWT (ES384).

## Prerequisites

- Java 25
- Maven 3.9+
- `~/.m2/settings-personal.xml` with GitHub Packages credentials (for `dm.dracolich.*` artifacts)
- MongoDB and an ES384 key pair (see [Key Generation](#key-generation)) — only if you intend to *run* it

## Build

```bash
mvn clean install -s ~/.m2/settings-personal.xml
```

## Running

The service is deployed to the `dracolich-dev` cluster and reached through
`https://dev.dracolich.app/dracolich/user/api/v0/`. Backend changes reach it through the CI pipeline
(see the workspace `CLAUDE.md`), so `mvn clean install` is the verification step for code work.

Running it locally is expected when developing a feature or chasing a bug here. Create an uncommitted
`user-api-web/src/main/resources/application-local.yml` and run with `SPRING_PROFILES_ACTIVE=local`;
`application-dev.yml.example` is a starting point. Both the local config and the keys are gitignored
and must stay that way — **never commit local config or a private key**.

The keys are not in the repo, so generate a pair first (see [Key Generation](#key-generation)) and
either set `jwt.private-key` / `jwt.public-key` in your local config or drop the files in
`user-api-web/src/main/resources/keys/`.

Swagger UI: `http://<host>/dracolich/user/api/v0/swagger-ui.html`.

## Configuration

All configuration is in `user-api-web/src/main/resources/application.yml`. Key environment variables:

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | Server port. Actuator listens separately on `7980`. |
| `MONGODB_URI` | _(required)_ | MongoDB connection |
| `MONGODB_DATABASE` | _(required)_ | Database name |
| `DRACOLICH_JWT_PRIVATE_KEY` | `classpath:keys/ec-private.pem` | ES384 private key path (this is the **only** service that gets it) |
| `DRACOLICH_JWT_PUBLIC_KEY` | `classpath:keys/ec-public.pem` | ES384 public key path |
| `JWT_ACCESS_EXPIRATION` | `900` | Access token TTL (seconds) |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh token TTL (seconds) |
| `JWT_CONFIRMATION_EXPIRATION` | `86400` | Email confirmation token TTL (seconds) |
| `MAIL_HOST` | _(empty)_ | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password |
| `MAIL_FROM` | `no-reply@dracolich.app` | From address on confirmation email |
| `CONFIRMATION_URL` | `https://dev.dracolich.app/dracolich/user/api/v0/auth/confirm` | Email confirmation link base URL |
| `CORS_ALLOWED_ORIGINS` | _(empty)_ | Allowed CORS origins |

## Key Generation

Generate an ES384 (ECDSA P-384) key pair:

```bash
openssl ecparam -genkey -name secp384r1 -noout -out ec-private.pem
openssl ec -in ec-private.pem -pubout -out ec-public.pem
```

For cross-service token verification, distribute `ec-public.pem` to the other services
(mtg-library-api, ai-api, mtg-deck-builder-api), which verify with forge's `EcPublicKeyJwtValidator`.
**Only user-api ever gets the private key** — never copy it into another repo or image.

## API Endpoints

Base path: `/dracolich/user/api/v0/`

All responses are wrapped in the standard `DmdResponse` envelope:

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": { ... },
  "errors": null
}
```

---

### POST `/auth/register`

Creates a new user account and sends a confirmation email.

**Request:**

```json
{
  "email": "player@example.com",
  "username": "playerOne",
  "password": "securePassword123",
  "displayName": "Player One"
}
```

**Success (201):**

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": "Registration successful. Please check your email to confirm your account.",
  "errors": null
}
```

**Errors:**

| Status | Code | Condition |
|---|---|---|
| 400 | DMD019 | Invalid email address |
| 400 | DMD018 | Password shorter than 8 characters |
| 400 | DMD013 | Blank username |
| 409 | DMD012 | Email already registered |
| 409 | DMD013 | Username already taken |

---

### GET `/auth/confirm?token={token}`

Activates a user account using the token from the confirmation email.

**Parameters:**

| Name | Type | Description |
|---|---|---|
| `token` | query string | JWT confirmation token from email |

**Success (200):**

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": "Account confirmed successfully.",
  "errors": null
}
```

**Errors:**

| Status | Code | Condition |
|---|---|---|
| 400 | DMD014 | Invalid, expired, or already-used token |

---

### POST `/auth/login`

Authenticates a user and returns JWT tokens.

**Request:**

```json
{
  "username": "playerOne",
  "password": "securePassword123"
}
```

**Success (200):**

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": {
    "accessToken": "eyJhbGciOiJFUzM4NCIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "expiresIn": 900
  },
  "errors": null
}
```

**Access token claims:**

```json
{
  "sub": "user-id",
  "username": "playerOne",
  "accessLevel": "COMMON_USER",
  "iat": 1711234567,
  "exp": 1711235467
}
```

**Errors:**

| Status | Code | Condition |
|---|---|---|
| 401 | DMD016 | Invalid username or password |
| 403 | DMD015 | Account not active (PENDING, SUSPENDED, DELETED) |

---

### POST `/auth/refresh`

Issues a new access token using a valid refresh token.

**Request:**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Success (200):**

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": {
    "accessToken": "eyJhbGciOiJFUzM4NCIs...",
    "refreshToken": "bmV3IHJlZnJlc2ggdG9rZW4...",
    "expiresIn": 900
  },
  "errors": null
}
```

**Errors:**

| Status | Code | Condition |
|---|---|---|
| 401 | DMD017 | Invalid, expired, or revoked refresh token |

---

### POST `/auth/logout`

Revokes a refresh token.

**Request:**

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Success (200):**

```json
{
  "success": true,
  "httpStatus": "OK",
  "message": "Request processed successfully",
  "payload": "Logged out successfully.",
  "errors": null
}
```

---

## Using the Access Token

Include the access token in the `Authorization` header for protected endpoints across all Dracolich services:

```
Authorization: Bearer eyJhbGciOiJFUzM4NCIs...
```

**Unauthenticated request (401):**

```json
{
  "success": false,
  "httpStatus": "UNAUTHORIZED",
  "message": "Authentication required.",
  "payload": null,
  "errors": null
}
```

## Access Levels

| Level | Description |
|---|---|
| `ADMIN` | Full access to all endpoints |
| `COMMON_USER` | Access to public endpoints |
| `DEV_USER` | GET, POST, and authorized deletes (user and custom character data) |

## Error Codes

| Code | Message |
|---|---|
| DMD012 | Email address already registered |
| DMD013 | Username already taken |
| DMD014 | Invalid or expired confirmation token |
| DMD015 | Account is not active. Current status: %s |
| DMD016 | Invalid username or password |
| DMD017 | Invalid or expired refresh token |
| DMD018 | Password does not meet minimum requirements |
| DMD019 | Email address is not valid |
| DMD020 | Failed to send confirmation email |

## Running Tests

```bash
mvn test -s ~/.m2/settings-personal.xml
```

Coverage is thin — 3 test classes against 31 main classes.

---

Part of the [Dracolich](https://github.com/laasilva?tab=repositories&q=dracolich) platform. For the
cross-repo picture — service topology, release pipeline, shared conventions — see the workspace guide
at `~/Dev/Dracolich/CLAUDE.md`.
