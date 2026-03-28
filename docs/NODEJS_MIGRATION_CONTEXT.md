# ERP Java-to-Node.js Migration Context

## Purpose

This document is the working source of truth for converting this ERP backend from Java Spring Boot microservices to Node.js microservices.

Use this document for two things:

1. To guide the actual migration work.
2. To restore context later if the chat context window is lost.

If a future prompt includes this document, the expected behavior is:

- understand the current Java architecture
- preserve business behavior and API contracts
- follow the target Node.js architecture defined here
- migrate service by service without breaking the system

## Current Backend Summary

This repository is a Java microservices ERP platform with:

- Spring Boot services
- PostgreSQL databases
- Supabase storage
- Eureka service discovery
- Spring Cloud Gateway
- JWT-based authentication
- internal service-to-service APIs
- email sending
- file uploads
- Flyway in some services
- WebSocket/realtime features in chat and project modules

Top-level services found in this repo:

- `auth-service`
- `gateway-service`
- `employee-service`
- `client-service`
- `lead-service`
- `project_service`
- `chat-service`
- `finance-servic`
- `eureka-server`
- `nginx`

## Service Inventory

### 1. Auth Service

Purpose:

- login
- refresh token
- logout
- forgot password
- OTP verification
- password reset
- internal employee auth operations

Main endpoints seen:

- `/auth/**`
- `/internal/auth/**`

Important code patterns:

- JWT generation and validation
- refresh token table
- internal API key filter
- mail templates

### 2. Gateway Service

Purpose:

- entry point for all services
- JWT validation/filtering
- route forwarding to downstream services

Current route groups in gateway config:

- `/auth/**`, `/internal/auth/**` -> auth-service
- `/employee/**`, `/admin/**` -> employee-service
- `/clients/**`, `/internal/clients/**` -> client-service
- `/leads/**`, `/deals/**`, `/stages/**` -> lead-service
- `/project/**`, `/status/**`, `/task/**`, `/tasks/**`, `/timesheets/**`, `/api/projects/**`, `/api/labels/**`, `/projects/**`, `/files/**`, `/notes/**`, `/me/**`, `/weekly-timesheets/**`, `/api/recurring-tasks/**`, `/stages/**` -> project-service
- `/api/chat/**`, `/ws-chat/**` and demo websocket paths -> chat-service
- `/api/invoices/**`, `/api/credit-notes/**`, `/api/payments/**`, `/api/payment-gateways/**`, `/api/invoice/**` -> finance-service

### 3. Employee Service

Purpose:

- employee profile management
- company management
- attendance
- leave management
- holiday management
- promotion
- appreciation and awards
- notifications
- push notification
- file/document handling

Observed module size:

- controllers: 21
- entities: 23
- repositories: 19
- services: 30

Integrations:

- auth-service internal APIs
- Supabase
- SMTP/email
- Expo push API

### 4. Client Service

Purpose:

- client CRUD
- company association
- categories and subcategories
- client notes
- client documents
- CSV import

Observed module size:

- controllers: 5
- entities: 7
- repositories: 6
- services: 12

Integrations:

- Supabase
- mail
- internal existence checks
- application events

### 5. Lead Service

Purpose:

- leads
- deals
- stages
- lead notes
- deal notes
- comments
- tags
- priorities
- followups
- deal documents
- CSV import
- lead-to-client/deal conversion flows

Observed module size:

- controllers: 14
- entities: 18
- repositories: 13
- services: 29

Integrations:

- employee-service
- client-service
- notification calls
- application events/listeners
- Supabase

### 6. Project Service

Purpose:

- projects
- project categories
- milestones
- tasks
- subtasks
- labels
- notes
- file uploads
- timesheets/time logs
- discussion rooms/messages
- websocket collaboration

Observed module size:

- controllers: 26
- entities: 24
- repositories: 19
- services: 36

Integrations:

- employee-service
- client-service
- notification-service behavior through employee endpoints
- Supabase
- WebSocket messaging
- Flyway migrations

### 7. Chat Service

Purpose:

- direct chat
- message history
- rooms list
- mark-read
- message deletion
- file upload
- search
- websocket realtime events

Observed module size:

- controllers: 4
- entities: 5
- repositories: 3
- services: 6

Integrations:

- employee-service
- notification endpoint
- Supabase S3-compatible storage
- WebSocket/STOMP style realtime flow

### 8. Finance Service

Purpose:

- invoices
- payments
- payment gateways
- credit notes
- invoice receipt PDF
- reminder emails

Observed module size:

- controllers: 5
- entities: 8
- repositories: 5
- services: 10

Integrations:

- client-service
- project-service
- Supabase
- mail
- PDF generation

## Platform Patterns Found in the Java Code

These are the patterns the Node.js version must preserve.

### 1. Each business domain is a separate service

Do not collapse all services into one monolith unless explicitly decided later.

Default target should remain microservice-oriented:

- auth
- gateway/api edge
- employee
- client
- lead
- project
- chat
- finance

### 2. Each service owns its own database/schema responsibility

The `.env` shows separate database URLs:

- `DB_URL_AUTH`
- `DB_URL_EMPLOYEE`
- `DB_URL_CLIENT`
- `DB_URL_LEAD`
- `DB_URL_PROJECT`
- `DB_URL_CHAT`
- `DB_URL_FINANCE`

Migration rule:

- preserve service data ownership
- avoid cross-service direct table access
- communicate via HTTP/event/realtime interfaces

### 3. Security is shared across services

Current shared security patterns:

- JWT secret used across services
- gateway-level JWT validation
- per-service JWT parsing/authorization
- internal API key for service-to-service protected endpoints

Migration rule:

- centralize token issuing in auth service
- validate JWT consistently in gateway and services
- keep internal service auth separate from end-user auth

### 4. Supabase is used mainly for object/file storage

Migration rule:

- keep file binary/object storage in Supabase
- keep metadata in PostgreSQL
- preserve bucket/path conventions per module

### 5. Some services use Flyway, some rely on JPA auto-update

This is important.

Java currently mixes:

- `ddl-auto: update`
- Flyway-managed migrations

Migration rule:

- Node.js target must standardize on explicit SQL migrations only
- do not rely on ORM auto-sync in production

### 6. There are async/event-driven flows

Examples seen:

- application events/listeners
- employee created listener
- lead conversion listeners
- startup event logic

Migration rule:

- first preserve behavior using in-process domain events where needed
- later introduce a real broker only if required

### 7. Realtime exists in chat and project

Migration rule:

- chat and discussion features must not be rewritten as plain polling-only systems
- Node target should support WebSocket-based realtime delivery

## Recommended Node.js Target Architecture

## Core Technology Choices

Use this stack unless there is a strong reason to change:

- runtime: Node.js 22+
- language: TypeScript
- framework: NestJS
- ORM: Prisma
- validation: Zod or Nest DTO validation with `class-validator`
- auth token library: `jsonwebtoken` or `jose`
- password hashing: `bcrypt`
- HTTP client: `axios` or `undici`
- file upload: `multer` via Nest platform adapters
- realtime: Socket.IO or native WebSocket gateway in NestJS
- email: `nodemailer`
- queue/events later if needed: BullMQ or RabbitMQ
- migration tool: Prisma migrations or SQL migrations
- API docs: Swagger/OpenAPI
- testing: Jest + Supertest
- lint/format: ESLint + Prettier

## Service Structure Standard

Every Node service should follow this structure:

```text
services/
  service-name/
    src/
      main.ts
      app.module.ts
      config/
      common/
      modules/
        auth/
        users/
        ...
      database/
      integrations/
      events/
      jobs/
      websocket/
      utils/
    prisma/
      schema.prisma
      migrations/
    test/
    package.json
    tsconfig.json
    .env.example
```

## Required Internal Layering

For every migrated module follow:

- controller layer: transport only
- service layer: business rules
- repository/data-access layer: Prisma queries only
- mapper/serializer layer: response shaping
- integration clients: other microservice calls
- guards/middleware/interceptors: auth, logging, request context

Important rule:

- controllers must stay thin
- business logic must not leak into controllers
- raw Prisma calls should not be spread all over the codebase

## API Contract Rules

While migrating:

- keep existing endpoint paths unless there is an explicit decision to version/change them
- preserve request and response payload shapes as much as possible
- preserve auth header behavior
- preserve multipart upload behavior
- preserve internal endpoint semantics

If an API contract must change:

- document it first
- add a compatibility layer if possible
- migrate consumers one by one

## Database Migration Rules

### Non-negotiable rules

- do not use `prisma db push` as the production migration strategy
- create explicit migrations
- compare Java entities, repository queries, and existing tables before changing schema
- preserve enum values carefully
- preserve column names when practical
- preserve foreign key behavior
- preserve auditing timestamps and ownership fields

### Database approach

For each service:

1. inspect Java entities
2. inspect repository methods
3. inspect DTOs and controllers
4. inspect Flyway SQL if present
5. produce Prisma schema
6. produce migration SQL
7. verify sample read/write flows

## File and Storage Migration Rules

Supabase behavior to preserve:

- upload file
- generate public URL or signed URL as required
- store metadata in database
- delete remote object when local metadata is deleted
- keep bucket and object path conventions stable

Node design rule:

- create one storage adapter per service
- hide Supabase details behind a storage interface

Example:

- `StorageService.upload(...)`
- `StorageService.delete(...)`
- `StorageService.getPublicUrl(...)`

## Storage Decision

Chosen storage for the Node.js migration:

- `Cloudinary`

Reason for current decision:

- starting team size is about 20 people
- expected early-stage file volume is manageable
- simpler startup path is more important right now than building the cheapest long-term storage stack
- Cloudinary is sufficient for the current stage

Migration rule:

- replace Supabase Storage integrations with a `CloudinaryStorageAdapter`
- keep database metadata in PostgreSQL
- do not expose Cloudinary-specific logic throughout the business layer

Required abstraction:

- `StorageService.upload(...)`
- `StorageService.delete(...)`
- `StorageService.getPublicUrl(...)`
- `StorageService.getSignedUrl(...)` if private access is needed later

Important caution:

- Cloudinary is best for light-to-medium file usage at this stage
- if document volume or download volume grows a lot later, reevaluate `Cloudflare R2` or `MinIO`
- write the code so storage provider can be swapped later without rewriting business modules

Recommended usage in this ERP:

- use Cloudinary for employee files, client documents, deal documents, invoice attachments, chat attachments, and project uploads in the first phase
- keep a provider-neutral storage interface so future migration away from Cloudinary remains easy

## Authentication and Authorization Rules

Auth migration must preserve:

- login
- refresh
- logout
- password reset
- OTP flow
- internal auth endpoints for employee lifecycle integration
- admin-sensitive routes

Node auth design:

- `auth-service` remains token issuer
- gateway validates JWT for public traffic
- downstream services still verify claims needed for authorization
- internal service calls use either:
  - internal API key, or
  - signed service token

Preferred long-term approach:

- user JWT for user-facing traffic
- signed service token for service-to-service calls
- remove hard-coded shared internal secrets from code

## Realtime Migration Rules

### Chat Service

Must preserve:

- send message
- history
- rooms list
- read state
- file attachments
- realtime delivery
- typing/online signals if currently used

Recommended Node design:

- NestJS WebSocket gateway
- Redis adapter later if horizontally scaled
- separate REST endpoints for history/search/uploads
- websocket for realtime events only

### Project Discussion Realtime

Must preserve:

- discussion room events
- new messages
- updates/deletes
- notification-style push to active clients

## External Integration Rules

The new Node services must isolate external dependencies behind adapters:

- `AuthClient`
- `EmployeeClient`
- `ClientClient`
- `ProjectClient`
- `NotificationClient`
- `SupabaseStorageAdapter`
- `MailAdapter`

Rule:

- no direct HTTP calls scattered in domain services
- wrap each external service in a dedicated integration client

## Recommended Migration Order

Do not migrate everything at once.

Follow this order:

1. Gateway service
2. Auth service
3. Client service
4. Lead service
5. Employee service
6. Finance service
7. Chat service
8. Project service

Why this order:

- gateway and auth define cross-cutting security and routing
- client and lead are medium complexity and good pattern setters
- employee is large and central
- finance is bounded but integration-heavy
- chat and project are the most realtime/complex modules

Alternative safer order if we want easier wins first:

1. Auth
2. Gateway
3. Client
4. Finance
5. Lead
6. Employee
7. Chat
8. Project

## Service-by-Service Deliverables

For each Java service converted to Node, produce these artifacts:

1. architecture note
2. endpoint inventory
3. entity-to-schema mapping
4. Prisma schema
5. migration SQL
6. module structure
7. integration clients
8. auth/role rules
9. test plan
10. cutover checklist

## Definition of Done For One Migrated Service

A service migration is complete only when all of these are true:

- endpoints implemented
- database schema migrated safely
- auth and authorization preserved
- file upload behavior preserved if applicable
- integration calls work
- tests cover critical flows
- `.env.example` created
- Swagger/OpenAPI available
- Dockerfile works
- service can run locally
- gateway route updated
- migration notes written

## Cross-Service Conventions For Node

These conventions must be followed across all new Node services.

### Naming

- folder names: kebab-case
- TypeScript classes: PascalCase
- variables/functions: camelCase
- environment variables: UPPER_SNAKE_CASE

### Error handling

- use consistent error response structure
- distinguish validation, auth, not-found, conflict, and internal errors
- do not leak stack traces in production responses

### Logging

- add structured logging
- include request id / correlation id
- log service-to-service failures with target service and endpoint

### Configuration

- every service gets `.env.example`
- never commit real secrets
- use a typed config loader

### Testing

- unit tests for business logic
- integration tests for controllers
- contract tests for important service-to-service APIs

## Important Risks To Watch During Migration

### 1. Hidden business logic inside service implementations

The Java project uses many service classes and listeners. Business rules may be spread across:

- services
- event listeners
- controllers
- helper utilities

Migration rule:

- inspect all three before rewriting a feature

### 2. Inconsistent schema management

Because some services use Flyway and some use JPA auto-update, the real database may not perfectly match Java entities.

Migration rule:

- treat the running database as a source of truth to verify against

### 3. Shared JWT assumptions across services

Some services parse JWT directly with the same secret.

Migration rule:

- document claim expectations service by service before rewriting auth guards

### 4. File deletion and public URL logic

Supabase path parsing and delete behavior differ per service.

Migration rule:

- inspect every upload/delete implementation before standardizing

### 5. Realtime complexity

Chat and project discussion behavior can break easily if rewritten too aggressively.

Migration rule:

- preserve protocol behavior first, optimize later

## Working Rules For Future Migration Sessions

Whenever continuing this migration in a future session:

1. read this document first
2. inspect the target Java service before changing code
3. preserve endpoint contracts unless explicitly approved otherwise
4. create or update migration notes after each completed service
5. do not perform schema-destructive changes without a rollback path
6. do not replace microservice boundaries with shortcuts unless explicitly approved

## Standard Prompt To Reuse Later

Use the following prompt in a future session along with this document:

```text
Read `docs/NODEJS_MIGRATION_CONTEXT.md` first and use it as the source of truth.

We are migrating this ERP backend from Java Spring Boot microservices to Node.js TypeScript microservices.

Your job:
- inspect the current Java implementation for the target service
- preserve business logic and API contracts
- follow the Node architecture and migration rules from the document
- implement the migration in a production-structured way
- update the migration document if new important patterns are discovered

Current task:
[WRITE THE SPECIFIC TASK HERE]
```

## Short Prompt Version

```text
Read `docs/NODEJS_MIGRATION_CONTEXT.md`, restore the architecture/migration context, then continue the Java-to-Node migration for [SERVICE OR FEATURE NAME] while preserving behavior and API contracts.
```

## Suggested First Execution Plan

The best next practical step is:

1. create a new `node-backend/` or `services-node/` workspace
2. scaffold `auth-service` and `gateway-service` first
3. define shared Node conventions
4. migrate one medium-size domain service to establish the pattern
5. validate the pattern before touching the largest services

## Expected Assistant Behavior After Reading This Document

If this document is provided in a later session, the assistant should:

- understand the ERP is microservice-based
- avoid suggesting a casual full rewrite without service boundaries
- preserve PostgreSQL plus Supabase design
- preserve gateway/auth architecture
- use TypeScript and a production-ready Node structure
- migrate incrementally and document decisions

## Notes

- `finance-servic` and `project_service` use non-standard folder names in the current repo; keep that in mind when locating code.
- Current `.env` contains real-looking secrets. During Node migration, move toward sanitized `.env.example` files and secret management.
- Some current Java services appear to contain duplicated or evolved code sections; inspect carefully before copying logic.
