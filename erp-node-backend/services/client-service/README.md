# Client Service

Current Node migration slice for the Java `client-service`.

Implemented:

- client CRUD
- multipart client create/update
- internal exists by email
- internal get by clientId
- category CRUD
- subcategory CRUD
- notes CRUD
- documents CRUD
- client CSV import
- Cloudinary-backed uploads

Remaining parity work:

- search and response-shape refinement against Java
- client created mail/event side effects
- document download streaming parity
- full Postman coverage and live endpoint verification

See [CLIENT_SERVICE_MIGRATION_STATUS.md](/Users/hemantsharma/Downloads/ERP%20Testing/erp-node-backend/services/client-service/CLIENT_SERVICE_MIGRATION_STATUS.md) for the working checklist.
