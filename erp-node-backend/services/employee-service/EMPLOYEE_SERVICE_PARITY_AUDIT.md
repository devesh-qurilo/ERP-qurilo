# Employee Service Parity Audit

## Status

Node `employee-service` is at practical feature parity with the Java `employee-service` for the currently identified controller surface.

## Java Controllers Audited

- `EmployeeController`
- `EmployeeInviteController`
- `EmployeeRegistrationController`
- `EmployeeExcelController`
- `NotificationController`
- `PushController`
- `AttendanceController`
- `AttendanceImportController`
- `EmployeeClockController`
- `LeaveController`
- `LeaveQuotaController`
- `HolidayController`
- `CompanyController`
- `DepartmentController`
- `DesignationController`
- `DocumentController`
- `EmergencyContactController`
- `AppreciationController`
- `AwardController`
- `PromotionController`

## Completed In Node

- Employee CRUD
- Employee self profile update
- Employee invite and accept
- Complete registration and employee ID change
- Employee meta lookup and birthday lookup
- Department CRUD and health route
- Designation CRUD
- Attendance CRUD-style admin flows
- Clock in and clock out flows
- Attendance calendar and WFH views
- Attendance CSV import
- Leave apply, admin apply, status updates, calendar, delete, by-employee
- Leave quota endpoints
- Holiday management
- Company management
- Emergency contacts
- Employee documents
- Notifications including internal send routes
- Push token register and unregister
- Awards
- Appreciations
- Promotions
- Employee CSV import
- Employee XLSX import
- Employee XLSX export
- Real Cloudinary-backed multipart upload flows for profile/company/document/leave/award/appreciation paths

## Last Route-Level Gap Closed

- Added `DELETE /employee/notifications/admin/{id}` alias in Node to match Java admin delete route.

## Residual Differences

- Node export now supports real `.xlsx` parity and also still has CSV import support. This is an intentional superset, not a gap.
- Push token storage is implemented, but outbound push delivery is still best-effort stub logic in `src/services/push.service.ts`.
- Some implementation details differ internally from Java:
  - Node uses Prisma instead of Spring Data JPA
  - Node uses Cloudinary adapter instead of Supabase
  - Some multipart endpoints also accept JSON/url fallback for safer rollout

## Remaining Optional Work

- Implement actual Expo/FCM/APNs outbound push delivery in `src/services/push.service.ts`
- Add broader automated parity tests for the full controller matrix
- Add API contract snapshots if frontend regression protection is needed

## Conclusion

For business feature migration, `employee-service` can be treated as complete enough to unblock dependent services.
