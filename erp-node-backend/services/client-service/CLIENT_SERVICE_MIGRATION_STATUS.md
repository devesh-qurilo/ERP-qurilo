# Client Service Migration Status

## Current State

`client-service` has a working first migration slice in Node/TypeScript and builds successfully.

## Implemented

- `GET /health`
- `POST /clients`
- `GET /clients`
- `GET /clients/{id}`
- `GET /clients/client/{clientId}`
- `PUT /clients/{id}`
- `DELETE /clients/{id}`
- `GET /clients/internal/exists`
- `GET /clients/internal/client/{clientId}`
- `POST /clients/import/csv`
- `POST /clients/category`
- `POST /clients/category/subcategory`
- `GET /clients/category`
- `GET /clients/category/subcategory`
- `DELETE /clients/category/{id}`
- `DELETE /clients/category/subcategory/{id}`
- `POST /clients/{clientId}/notes`
- `GET /clients/{clientId}/notes`
- `PUT /clients/{clientId}/notes/{noteId}`
- `DELETE /clients/{clientId}/notes/{noteId}`
- `POST /clients/{clientId}/documents`
- `GET /clients/{clientId}/documents`
- `GET /clients/{clientId}/documents/{docId}`
- `DELETE /clients/{clientId}/documents/{docId}`

## Implemented Infrastructure

- Prisma schema for clients, company, categories, notes, and documents
- Cloudinary-backed media storage
- internal API key validation
- JWT-based admin and employee authorization
- Dockerfile for local containerized runtime
- local docker-compose service wiring

## Still Pending

- Java response-shape parity audit
- client created mail/event side effects
- document download streaming parity instead of metadata-only get
- live Docker verification for every endpoint
- Postman collection coverage

## Recommended Next Work

1. Start `client-service` in Docker and run smoke tests.
2. Add client-service requests to the shared Postman collection.
3. Close the remaining parity gaps after live verification.
