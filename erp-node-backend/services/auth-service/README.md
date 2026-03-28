# Auth Service

This service is the Node.js migration target for the Java `auth-service`.

## Java Behavior Being Preserved

- `POST /auth/login`
- `POST /auth/manual-register`
- `POST /auth/forgot-password`
- `POST /auth/verify-otp`
- `POST /auth/reset-password`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/forgot-ticket`
- `GET /auth/health`
- `POST /auth/translate`
- internal user lifecycle endpoints under `/internal/auth/**`

## Current State

- route contracts are scaffolded
- module boundaries are defined
- repositories are in-memory placeholders for now
- Prisma schema has been added for the real database model

## Next Steps

1. replace in-memory repositories with Prisma repositories
2. add real bcrypt password hashing
3. add real JWT signing and validation
4. add SMTP mail integration
5. add Google Translate integration or disable the endpoint intentionally

