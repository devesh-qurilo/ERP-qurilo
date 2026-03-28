# Stack Decision

## Infrastructure

- app hosting: cheap VPS
- database: Neon Postgres
- storage: Cloudinary
- source of truth for legacy behavior: Java repo in the parent workspace

## Technical Direction

- language: TypeScript
- workspace style: monorepo
- service structure: microservices
- storage pattern: provider-neutral adapter over Cloudinary
- migration style: incremental, service by service

## Initial Service Priority

1. gateway-service
2. auth-service
3. client-service

