# Project Service Migration Status

## Current State

`project-service` now has a working Node/TypeScript backend slice with Prisma, Cloudinary-backed uploads, gateway routing, and Docker wiring.

## Implemented in Node

- `GET /health`
- admin project CRUD
- employee/admin project list + project detail
- project metrics
- project counts
- project assignment + project admin assignment
- project status/progress update
- project pin/archive state
- project activity
- project category CRUD
- project CSV import
- client/employee project stats endpoints
- milestone CRUD + status update
- project files + task files
- project notes + task notes
- task category CRUD
- task stage CRUD
- label CRUD
- admin task CRUD
- employee task create
- task list/detail/counts
- waiting/approve/duplicate/copy-link task admin flows
- subtasks CRUD + toggle
- timesheet CRUD + weekly create + summaries
- discussion categories
- discussion rooms
- discussion messages + replies + best reply

## Implemented Infrastructure

- Prisma schema for projects, tasks, files, notes, subtasks, timesheets, and discussions
- Dockerfile for container runtime
- docker-compose service wiring on port `8086`
- gateway routes for `/projects`, `/api/projects`, `/files`, `/notes`, `/timesheets`, `/status`, `/task`, `/tasks`, and `/api/labels`
- employee-service + client-service metadata integration

## Still Pending From Full Java Parity

- task pin flow parity
- task reminder endpoints / scheduler parity
- any websocket/live discussion parity beyond HTTP APIs
- final live Docker/Postman verification of every route

## Recommended Next Work

1. Start `project-service` in Docker and run smoke tests.
2. Add project/work requests to the shared Postman collection.
3. Then move to the frontend integration for `/work/project`.
