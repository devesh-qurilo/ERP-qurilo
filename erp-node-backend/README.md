# ERP Node Backend

This is the new Node.js and TypeScript workspace for the ERP backend migration.

## Goals

- keep microservice boundaries
- preserve Java API contracts
- use Neon Postgres for database hosting
- use Cloudinary for file storage in phase 1
- move service by service instead of rewriting everything at once

## Current Scaffolding

- root workspace config
- shared packages
- `gateway-service` starter
- `auth-service` starter
- Docker local development setup
- local PostgreSQL container bootstrap

## Planned Services

- `gateway-service`
- `auth-service`
- `employee-service`
- `client-service`
- `lead-service`
- `project-service`
- `chat-service`
- `finance-service`

## Next Steps

1. install workspace dependencies
2. wire a real framework baseline
3. add auth database schema and migrations
4. migrate gateway route behavior from Java
5. migrate auth endpoints from Java

## Local Docker Usage

```bash
docker compose up --build
```

This starts:

- local PostgreSQL on `localhost:5432`
- auth-service on `localhost:8081`
- gateway-service on `localhost:8080`
