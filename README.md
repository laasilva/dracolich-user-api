# dracolich-user-api

Authentication and user management service for the Dracolich platform. Built with Spring Boot 4 (WebFlux), MongoDB, and JWT (ES384).

## Prerequisites

- Java 25
- MongoDB running on `localhost:27017`
- Maven 3.9+
- ES384 key pair (see [Key Generation](#key-generation))

## Quick Start

```bash
# Generate dev keys (one-time)
mkdir -p user-api-web/src/main/resources/keys
openssl ecparam -genkey -name secp384r1 -noout -out user-api-web/src/main/resources/keys/ec-private.pem
openssl ec -in user-api-web/src/main/resources/keys/ec-private.pem -pubout -out user-api-web/src/main/resources/keys/ec-public.pem

# Build
mvn clean install -s ~/.m2/settings-personal.xml

# Run
mvn spring-boot:run -pl user-api-web -s ~/.m2/settings-personal.xml
```

The API starts on `http://localhost:8081/dracolich-user/api/v0/`.

Swagger UI is available at `http://localhost:8081/dracolich-user/api/v0/swagger-ui.html`.

## Configuration

All configuration is in `user-api-web/src/main/resources/application.yml`. Key environment variables:

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8081` | Server port |
| `MONGODB_URI` | `mongodb://localhost:27017/dracolich-user-db` | MongoDB connection |
| `MONGODB_DATABASE` | `dracolich-user-db` | Database name |
| `JWT_PRIVATE_KEY` | `classpath:keys/ec-private.pem` | ES384 private key path |
| `JWT_PUBLIC_KEY` | `classpath:keys/ec-public.pem` | ES384 public key path |
| `JWT_ACCESS_EXPIRATION` | `900` | Access token TTL (seconds) |
| `JWT_REFRESH_EXPIRATION` | `604800` | Refresh token TTL (seconds) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP host |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password |
| `CONFIRMATION_URL` | `http://localhost:8081/dracolich-user/api/v0/auth/confirm` | Email confirmation link base URL |

## Key Generation

Generate an ES384 (ECDSA P-384) key pair:

```bash
openssl ecparam -genkey -name secp384r1 -noout -out ec-private.pem
openssl ec -in ec-private.pem -pubout -out ec-public.pem
```

For cross-service token verification, distribute `ec-public.pem` to other services (library-api, action-api). Only user-api needs the private key.

## API Endpoints

Base path: `/dracolich-user/api/v0/`

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
