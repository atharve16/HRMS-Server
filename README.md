# HRMS Backend

Production-oriented modular monolith for employee attendance, leave, office geofencing, holidays, notifications, bonuses, deductions, and monthly payroll. The backend is a Spring Boot REST API; no frontend code belongs in this repository.

> Current status: Phases 1–8 are implemented and tested: platform foundation, authentication, employees, organizational assignments, attendance/geofencing, leave management, company holidays, notifications, salary structures, and monthly payroll.

## Technology stack

- Java 21
- Spring Boot 3.5.16 and Maven
- Spring Web, Validation, Security, Mail, Actuator
- Spring Data MongoDB / MongoDB Atlas
- springdoc-openapi 2.10.0
- JUnit 5, AssertJ, Mockito, Spring Boot Test

JWT authentication uses Spring Security OAuth2 JOSE with HS256. Testcontainers MongoDB dependencies are included for replica-set integration tests.

## Architecture

The application is a feature-based modular monolith under `com.hrms`. Shared platform behavior lives under `config` and `common`; each business feature will own its controllers, DTOs, services, repositories, and MongoDB documents.

```text
src/main/java/com/hrms/
├── HrmsApplication.java
├── config/                 # configuration properties, time, MongoDB, security, OpenAPI
├── common/                 # responses, exceptions, validation, utilities, system endpoint
├── auth/                   # Phase 2
├── employee/               # Phase 3
├── department/             # Phase 4
├── office/                 # Phase 4
├── shift/                  # Phase 4
├── attendance/             # Phase 5
├── leave/                  # Phase 6
├── holiday/                # Phase 7
├── notification/           # Phase 7
├── payroll/                # Phase 8 salary structures, calculation, adjustments, lifecycle
└── audit/                  # introduced with audited workflows, finalized in Phase 10
```

Controllers return a consistent envelope:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {},
  "timestamp": "2026-10-31T10:00:00Z"
}
```

Expected business errors are represented by stable error codes. Validation, malformed JSON, unique-index conflicts, domain exceptions, and unexpected failures are handled centrally without exposing stack traces.

## Time and monetary rules

All persisted timestamps will use `Instant` and therefore UTC. `COMPANY_TIMEZONE` controls business dates, shift boundaries, leave days, holidays, and payroll calendars; it defaults to `Asia/Kolkata` and is validated as an IANA time zone. Financial calculations will use `BigDecimal` with scale 2 and `RoundingMode.HALF_UP`.

## MongoDB collection design

The collections currently implemented include `users`, `employees`, `employee_code_counters`, `departments`, `office_locations`, `shifts`, `employee_work_assignments`, `attendance_records`, `leave_balances`, `leave_requests`, `company_holidays`, `notifications`, `salary_structures`, `payroll_records`, `otp_verifications`, `refresh_tokens`, and append-only `audit_logs`.

Indexes will be declared alongside each document in its implementation phase, including compound uniqueness for attendance, balances, and payroll; TTL indexes for OTP and refresh tokens; and a `2dsphere` office-location index. Production has automatic index creation disabled so indexes can be applied as an explicit deployment step.

Attendance uses a unique `employeeId + workDate` index, a descending employee/date query index, and work-date/status indexes. A record stores the shift and office values resolved at check-in time, so later configuration edits do not change historical geofence or calculation evidence. Sessions retain server UTC timestamps, GPS accuracy, calculated office distance, and stable session IDs.

Leave balances have a unique `employeeId + leaveType + year` index. Leave requests have employee/date, employee/status, status/date, and type/status query indexes. A partial unique `employeeId + coveredDates` multikey index applies to pending and approved requests, providing database-backed overlap protection even during concurrent requests.

Company holidays use a partial unique active-date index plus a `date + active` query index. Notifications use descending `userId + createdAt` and `userId + isRead + createdAt` indexes for personal feeds and unread counts.

Salary structures use a unique employee/effective-from index and allow only one open active salary period per employee. Payroll records use a unique employee/month/year index plus month/year/status and employee/year query indexes. Optimistic versions protect salary and payroll transitions.

MongoDB transactions are enabled through `MongoTransactionManager`. Atlas clusters support the required replica-set semantics. Authentication integration tests use a real MongoDB replica-set Testcontainers configuration when Docker is available rather than mocking repositories.

## Security and authentication design

The application is stateless and closed by default. OTP request, OTP verification, refresh, health, and development OpenAPI paths are public. `/api/v1/auth/me` and logout require an access JWT; future `/admin/**` and `/employee/**` routes are role-gated, shared future domain routes require authentication, and all unmatched routes are denied.

Authentication is passwordless email OTP. Email addresses are normalized before lookup. OTPs are six-digit cryptographically random values, stored only as HMAC-SHA256 hashes, expire after five minutes, allow five attempts, and have a configurable resend cooldown. The request response is deliberately identical for missing, inactive, and eligible accounts. OTP values are never logged.

### Employee management

An employee is a profile document linked one-to-one with the authentication `users` document through `Employee.userId` and `User.employeeId`. Employee creation is an authenticated admin operation and saves both documents in one MongoDB transaction. Email changes update both documents in the same transaction; deactivation/reactivation updates both account statuses and takes effect immediately because the JWT filter reloads the current user. Employee responses never include passwords, OTPs, refresh tokens, or other authentication secrets.

Employee codes are generated by an atomic MongoDB counter (`employee_code_counters`) rather than by counting documents. The configured prefix is `EMPLOYEE_CODE_PREFIX` (default `EMP`), and codes are formatted as `EMP-0001`, `EMP-0002`, and so on. The code, email, and user link are immutable/unique at the database level where applicable. Employee list filtering and pagination run in MongoDB, including search across code, name, and email.

Departments, office locations, and shifts use explicit unique codes. Office coordinates are persisted as GeoJSON `Point` values in `[longitude, latitude]` order and have a `2dsphere` index. Shift definitions support overnight work when the end time is equal to or earlier than the start time. Employee work assignments are historical, effective-dated records; active assignment creation rejects overlapping periods and the resolver can retrieve a past assignment after it has been ended.

The existing nullable `Employee.workLocation` field is retained for backward-compatible profile data; Phase 4 work assignments are the authoritative source for shift and office resolution and do not add duplicate assignment fields to `Employee`.

Successful verification issues a 15-minute HS256 access JWT and a 30-day random refresh token. The refresh token is stored only as a SHA-256 hash in `refresh_tokens`, delivered in an HTTP-only cookie, and rotated on every refresh. Reuse of a revoked token revokes its complete token family. Logout revokes the family and clears the cookie. The JWT filter re-checks the current user on every request, so deactivation takes effect immediately.

The initial administrator is created only when `INITIAL_ADMIN_EMAIL` is set and no administrator exists. The configured email is normalized, an existing user is never promoted, and a unique bootstrap marker plus duplicate-key handling makes concurrent startup idempotent. The bootstrap does not create credentials; the administrator signs in through the same OTP flow.

## Domain workflows

### Geofencing and attendance

The server validates coordinate ranges, reported GPS accuracy, and Haversine distance from the employee's assigned active office. Each attendance work date contains multiple sessions and immutable shift and office snapshots. Calculations handle grace, breaks, late minutes, early departure, overtime, overnight shifts, and incomplete sessions. Corrections require an admin reason and append-only audit entry. Attendance does not yet create absence, holiday, or leave records.

### Leave and holidays

Phase 6 supports `CL` (Casual Leave), `SL` (Sick Leave), `EL` (Earned Leave), `LWP` (Leave Without Pay), and `CO` (Compensatory Off). CL, SL, EL, and CO are paid and are manually allocated by an administrator; LWP is unpaid and does not require or change a balance. Automatic accrual is not implemented.

Balances follow `availableDays = allocatedDays - usedDays - pendingDays`. Applying for paid leave reserves working days in `pendingDays`; approval moves the same amount from pending to used, while rejection or employee cancellation releases it. Allocation can never be negative or below used plus pending days. Request transitions are limited to `PENDING -> APPROVED`, `PENDING -> REJECTED`, and employee-owned `PENDING -> CANCELLED`. These request and balance changes, including audit entries, run in MongoDB transactions and use optimistic versions for concurrent updates.

Leave calculation is inclusive and resolves the employee's effective assignment for each date. Only dates present in that assignment's shift working days and not present in the active company holiday calendar are charged, so a Friday-through-Monday request on a Monday-to-Friday shift consumes two days unless either working date is also a holiday. The same holiday exclusion applies to LWP, so paid company holidays never become unpaid leave days. An assignment change inside the range is handled date by date. Missing assignments and ranges containing no chargeable working day are rejected.

Company holidays are always paid, company-wide `LocalDate` values interpreted through the configured company timezone (`Asia/Kolkata` by default). Admins create individual dates, can update them, and deactivate rather than delete them. Only one active holiday can exist on a date. Employees see active holidays without creator/updater metadata. Holiday changes do not rewrite existing approved leave snapshots or balances; maintain the calendar before employees submit leave requests. New leave calculations always read the current active calendar.

### Notifications and email

In-app notification types are `HOLIDAY_ANNOUNCEMENT`, `LEAVE_APPROVED`, and `LEAVE_REJECTED`. Holiday create/update/deactivation creates notifications for all active employees except an employee account performing its own action. Leave decisions notify only the active employee who owns the request. Notification reads are scoped by authenticated `userId`, newest first, and mark-read operations are idempotent.

New holiday announcements and leave approval/rejection also send concise emails through the existing Spring Mail configuration. Email is attempted synchronously after the MongoDB transaction commits. SMTP failures are logged and never roll back or change the successful holiday/leave API result. Completed leave transitions, duplicate holiday creation, no-op holiday updates, and repeated deactivation do not send duplicate email. There is intentionally no queue, scheduler, automatic retry, preference system, or real-time push channel.

### Payroll

Monthly payroll uses scheduled working days within the employee's employment period:

```text
Daily Rate = Monthly Fixed Salary / Scheduled Working Days
Unpaid Days = Approved Unpaid Leave Days + Unexcused Absence Days
Net Salary = Fixed Salary + Approved Bonuses + Overtime
             - Unpaid Deduction - Approved Other Deductions
```

Paid leave, holidays, weekly offs, late arrival, and early departure do not produce deductions. Payroll follows `DRAFT -> REVIEWED -> FINALIZED -> PAID`, prevents employee/month duplicates, refuses unresolved attendance, and keeps finalized snapshots immutable. Statutory taxes are out of scope until rules are supplied.

The denominator is the employee's scheduled working days inside their joining/termination period, resolved against the effective assignment for each date. Active holidays and weekly offs are removed first. A valid completed attendance session—including a completed partial day—counts as attendance. Remaining scheduled dates are classified once as paid leave, LWP, or unexcused absence, which prevents LWP and absence double deductions. Monetary values are rounded to two decimals with `HALF_UP`.

Salary changes close the current period the day before the new effective date and create a new record. Payroll stores salary, employee code/name, department, date breakdown, and adjustment snapshots, so later profile or salary changes do not rewrite history. Draft adjustments are explicit `BONUS`, `OVERTIME_PAY`, `OTHER_EARNING`, or `OTHER_DEDUCTION` amounts; recorded attendance overtime, lateness, and early departure never create money adjustments automatically.

## Implemented API

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/api/v1/system/health` | Public | Lightweight HTTP/API readiness |
| GET | `/actuator/health` | Public | Operational health |
| GET | `/v3/api-docs` | Public in dev | OpenAPI JSON |
| GET | `/swagger-ui.html` | Public in dev | Swagger UI |
| POST | `/api/v1/auth/request-otp` | Public | Request an OTP (generic response) |
| POST | `/api/v1/auth/verify-otp` | Public | Verify OTP and issue access JWT + refresh cookie |
| POST | `/api/v1/auth/refresh` | Public with refresh cookie | Rotate refresh token and issue access JWT |
| POST | `/api/v1/auth/logout` | Authenticated | Revoke refresh-token family and clear cookie |
| GET | `/api/v1/auth/me` | Authenticated | Return safe authenticated profile |
| POST | `/api/v1/admin/employees` | ADMIN | Create a linked employee and OTP-authenticated user |
| GET | `/api/v1/admin/employees` | ADMIN | Paginated employee list with search, filters, and sorting |
| GET | `/api/v1/admin/employees/{employeeId}` | ADMIN | Get employee details |
| PATCH | `/api/v1/admin/employees/{employeeId}` | ADMIN | Partially update employee profile; email updates User atomically |
| PATCH | `/api/v1/admin/employees/{employeeId}/deactivate` | ADMIN | Deactivate employee and linked login |
| PATCH | `/api/v1/admin/employees/{employeeId}/reactivate` | ADMIN | Reactivate employee and linked login |
| GET | `/api/v1/employees/me` | EMPLOYEE | Return the authenticated employee's own profile |
| POST | `/api/v1/admin/departments` | ADMIN | Create a department |
| GET | `/api/v1/admin/departments` | ADMIN | List departments with pagination and filters |
| GET/PATCH | `/api/v1/admin/departments/{id}` | ADMIN | Read or update a department |
| PATCH | `/api/v1/admin/departments/{id}/deactivate` | ADMIN | Deactivate when no active employees remain assigned |
| PATCH | `/api/v1/admin/departments/{id}/reactivate` | ADMIN | Reactivate a department |
| POST | `/api/v1/admin/office-locations` | ADMIN | Create an office with GeoJSON coordinates and geofence limits |
| GET | `/api/v1/admin/office-locations` | ADMIN | List office locations with pagination and filters |
| GET/PATCH | `/api/v1/admin/office-locations/{id}` | ADMIN | Read or update an office location |
| PATCH | `/api/v1/admin/office-locations/{id}/deactivate` | ADMIN | Deactivate when no active assignments depend on it |
| PATCH | `/api/v1/admin/office-locations/{id}/reactivate` | ADMIN | Reactivate an office location |
| POST | `/api/v1/admin/shifts` | ADMIN | Create a shift |
| GET | `/api/v1/admin/shifts` | ADMIN | List shifts with pagination and filters |
| GET/PATCH | `/api/v1/admin/shifts/{id}` | ADMIN | Read or update a shift |
| PATCH | `/api/v1/admin/shifts/{id}/deactivate` | ADMIN | Deactivate when no active assignments depend on it |
| PATCH | `/api/v1/admin/shifts/{id}/reactivate` | ADMIN | Reactivate a shift |
| POST | `/api/v1/admin/employees/{employeeId}/work-assignments` | ADMIN | Create an effective-dated assignment |
| GET | `/api/v1/admin/employees/{employeeId}/work-assignments` | ADMIN | List assignment history |
| GET | `/api/v1/admin/work-assignments/{assignmentId}` | ADMIN | Get an assignment |
| PATCH | `/api/v1/admin/work-assignments/{assignmentId}/end` | ADMIN | Close an assignment period |
| POST | `/api/v1/attendance/check-in` | EMPLOYEE | Record a server-timestamped, geofenced check-in |
| POST | `/api/v1/attendance/check-out` | EMPLOYEE | Record a server-timestamped, snapshot-validated check-out |
| GET | `/api/v1/attendance/me?date=YYYY-MM-DD` | EMPLOYEE | Read attendance for one work date |
| GET | `/api/v1/attendance/me/history` | EMPLOYEE | Paginated personal attendance history |
| GET | `/api/v1/attendance/me/status` | EMPLOYEE | Read current open-session status |
| GET | `/api/v1/admin/attendance` | ADMIN | Filtered, paginated attendance listing |
| GET | `/api/v1/admin/attendance/{attendanceId}` | ADMIN | Full attendance detail and evidence |
| GET | `/api/v1/admin/employees/{employeeId}/attendance` | ADMIN | Employee attendance history |
| GET | `/api/v1/admin/attendance/summary` | ADMIN | Aggregate attendance metrics |
| PATCH | `/api/v1/admin/attendance/{attendanceId}/correction` | ADMIN | Audited session time correction |
| POST | `/api/v1/leaves` | EMPLOYEE | Apply for full-day leave and reserve paid balance |
| GET | `/api/v1/leaves/me` | EMPLOYEE | Filtered, paginated personal leave requests |
| GET | `/api/v1/leaves/me/balance` | EMPLOYEE | Personal paid-leave balances, optionally by year |
| PATCH | `/api/v1/leaves/{leaveId}/cancel` | EMPLOYEE | Cancel an owned pending request and release its reservation |
| GET | `/api/v1/admin/leaves` | ADMIN | Database-filtered and paginated leave request list |
| GET | `/api/v1/admin/leaves/{leaveId}` | ADMIN | Get leave request details |
| PATCH | `/api/v1/admin/leaves/{leaveId}/approve` | ADMIN | Approve a pending request and move pending days to used |
| PATCH | `/api/v1/admin/leaves/{leaveId}/reject` | ADMIN | Reject a pending request with remarks and release its reservation |
| GET | `/api/v1/admin/employees/{employeeId}/leave-balance` | ADMIN | Read an employee's paid-leave balances |
| PUT | `/api/v1/admin/employees/{employeeId}/leave-balance` | ADMIN | Create or update one manual leave allocation |
| POST | `/api/v1/admin/holidays` | ADMIN | Create and announce a paid company holiday |
| GET | `/api/v1/admin/holidays` | ADMIN | Filter and paginate all active/inactive holidays |
| GET | `/api/v1/admin/holidays/{holidayId}` | ADMIN | Get holiday details and audit metadata |
| PATCH | `/api/v1/admin/holidays/{holidayId}` | ADMIN | Update a holiday |
| PATCH | `/api/v1/admin/holidays/{holidayId}/deactivate` | ADMIN | Deactivate without deleting historical data |
| GET | `/api/v1/holidays` | EMPLOYEE | View active holidays by year/date range |
| GET | `/api/v1/holidays/upcoming` | EMPLOYEE | View future active holidays in ascending order |
| GET | `/api/v1/notifications` | EMPLOYEE | Paginated personal notification feed, newest first |
| GET | `/api/v1/notifications/unread-count` | EMPLOYEE | Count personal unread notifications |
| PATCH | `/api/v1/notifications/{notificationId}/read` | EMPLOYEE | Idempotently mark an owned notification read |
| PATCH | `/api/v1/notifications/read-all` | EMPLOYEE | Mark all owned notifications read |
| PUT | `/api/v1/admin/employees/{employeeId}/salary` | ADMIN | Create/update an effective-dated monthly fixed salary |
| GET | `/api/v1/admin/employees/{employeeId}/salary` | ADMIN | Get the open salary structure |
| GET | `/api/v1/admin/employees/{employeeId}/salary/history` | ADMIN | Get salary history |
| POST | `/api/v1/admin/payroll/generate` | ADMIN | Generate non-overwriting draft payroll for a month |
| GET | `/api/v1/admin/payroll` | ADMIN | Filter and paginate payroll records |
| GET/PATCH | `/api/v1/admin/payroll/{payrollId}` | ADMIN | Read or replace draft adjustments |
| PATCH | `/api/v1/admin/payroll/{payrollId}/review` | ADMIN | Move DRAFT to REVIEWED |
| PATCH | `/api/v1/admin/payroll/{payrollId}/finalize` | ADMIN | Move REVIEWED to FINALIZED |
| PATCH | `/api/v1/admin/payroll/{payrollId}/pay` | ADMIN | Move FINALIZED to PAID |
| GET | `/api/v1/payroll/me` | EMPLOYEE | List owned FINALIZED/PAID payroll only |
| GET | `/api/v1/payroll/me/{payrollId}` | EMPLOYEE | Read one owned visible payroll record |

Example employee request:

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phone": "+919876543210",
  "employmentType": "FULL_TIME",
  "joiningDate": "2026-01-15",
  "designation": "Engineer",
  "address": {
    "addressLine1": "1 Example Street",
    "city": "Jabalpur",
    "state": "Madhya Pradesh",
    "postalCode": "482001",
    "country": "India"
  }
}
```

Successful responses use the normal envelope and contain the employee profile, generated code, and employment status, but no authentication credentials. Employee list responses include `content`, `page`, `limit`, `totalElements`, and `totalPages`.

## Environment variables

Copy `.env.example` to a local `.env` for your own tooling. Spring Boot does not read `.env` automatically; export the variables in your shell or configure them in the IDE/run environment.

| Variable | Default | Notes |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Use `prod` in production |
| `MONGODB_URI` | `mongodb://localhost:27017` in dev | Atlas connection URI; required in prod |
| `MONGODB_DATABASE` | `hrms` | Database name |
| `COMPANY_TIMEZONE` | `Asia/Kolkata` | Valid IANA zone |
| `FRONTEND_URL` | `http://localhost:3000` | Sole allowed browser origin |
| `DEFAULT_GEOFENCE_RADIUS` | `100` | Metres |
| `MAX_GPS_ACCURACY` | `50` | Metres |
| `JWT_SECRET` | none | Phase 2; at least 32 random bytes |
| `JWT_ACCESS_EXPIRY` | `900000` | Milliseconds (15 minutes) |
| `JWT_REFRESH_EXPIRY` | `2592000000` | Milliseconds (30 days) |
| `OTP_HASH_SECRET` | none | Separate secret, at least 32 random characters |
| `OTP_EXPIRY` | `300000` | Milliseconds (5 minutes) |
| `OTP_RESEND_COOLDOWN` | `60000` | Milliseconds |
| `OTP_MAX_ATTEMPTS` | `5` | Verification attempts |
| `OTP_RATE_LIMIT_MAX_REQUESTS` | `10` | Per client IP and window |
| `OTP_RATE_LIMIT_WINDOW` | `300000` | Milliseconds |
| `INITIAL_ADMIN_EMAIL` | none | Phase 2 bootstrap identity |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server |
| `MAIL_PORT` | `587` | SMTP STARTTLS port |
| `MAIL_USERNAME` | none | Secret |
| `MAIL_PASSWORD` | none | Secret/app password |
| `MAIL_FROM` | none | Verified sender |
| `MAIL_FROM_NAME` | `HRMS` | Display name used in OTP mail |
| `REFRESH_COOKIE_NAME` | `hrms_refresh_token` | HTTP-only refresh cookie name |
| `COOKIE_SECURE` | `false` in dev, `true` in prod | Set true when using HTTPS |
| `COOKIE_SAME_SITE` | `Lax` | Cookie SameSite policy |
| `COOKIE_DOMAIN` | none | Optional cookie domain |
| `EMPLOYEE_CODE_PREFIX` | `EMP` | Prefix for atomic employee codes |
| `ATTENDANCE_EARLY_CHECKIN_MINUTES` | `120` | Earliest permitted check-in before scheduled shift start |
| `ATTENDANCE_LATE_CHECKIN_WINDOW_MINUTES` | `240` | Latest permitted check-in after scheduled shift start/end window |
| `ATTENDANCE_OVERNIGHT_CHECKOUT_GRACE_MINUTES` | `240` | Configured overnight review window |
| `ATTENDANCE_MAX_SESSION_DURATION_HOURS` | `24` | Maximum open session duration |

Never commit `.env` or real credentials.

## MongoDB Atlas setup

1. Create an Atlas project and an M10+ production cluster (or an appropriate development tier).
2. Create a least-privilege database user for the `hrms` database. Do not use an Atlas owner account in the application.
3. Add only the application/developer IPs to Network Access; avoid `0.0.0.0/0` in production.
4. Copy the Java/SRV connection string and percent-encode special characters in the username/password.
5. Set `MONGODB_URI` and `MONGODB_DATABASE=hrms` in the runtime environment.
6. Verify TLS remains enabled. Atlas connection strings enable TLS by default.
7. Apply required indexes as part of deployment before disabling automatic creation. Development currently creates annotated indexes automatically; production does not.

Transactions require a replica set. Atlas provides one; a standalone local `mongod` is insufficient for transaction integration testing.

## SMTP setup

For Gmail, enable two-factor authentication and create an app password. Set `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM`; do not use a personal password. Production should use a dedicated transactional email account/provider with a verified sending domain.

## Local development

Prerequisites: JDK 21, Maven 3.6.3+, and MongoDB 7+ (prefer a local replica set for transaction work).

PowerShell example:

```powershell
$env:MONGODB_URI = 'mongodb://localhost:27017'
$env:MONGODB_DATABASE = 'hrms'
$env:COMPANY_TIMEZONE = 'Asia/Kolkata'
$env:FRONTEND_URL = 'http://localhost:3000'
$env:JWT_SECRET = 'replace-with-at-least-32-random-characters'
$env:OTP_HASH_SECRET = 'replace-with-a-different-at-least-32-character-secret'
$env:MAIL_FROM = 'noreply@example.com'
$env:INITIAL_ADMIN_EMAIL = 'admin@example.com'
mvn spring-boot:run
```

Then open `http://localhost:8080/swagger-ui.html` or call `http://localhost:8080/api/v1/system/health`.

To manually test authentication, request an OTP for the configured admin email, read the code from the configured SMTP mailbox, then POST it to `/api/v1/auth/verify-otp`. Store the returned access token in an `Authorization: Bearer ...` header for `/api/v1/auth/me`. Preserve the `Set-Cookie` refresh cookie and send it to `/api/v1/auth/refresh`; the response replaces it. POST `/api/v1/auth/logout` with the access token and cookie to revoke the session.

With the admin access token, POST the employee JSON shown above to `/api/v1/admin/employees`, then use the returned `id` with the admin detail, patch, deactivate, and reactivate endpoints. Use query parameters such as `?page=0&limit=20&search=ada&employmentStatus=ACTIVE&sortBy=lastName&sortDirection=asc` for list filtering. After the employee receives an OTP, call `/api/v1/employees/me` with that employee's access token. Deactivation should make subsequent OTP requests ineligible; reactivation restores login eligibility.

To test attendance, create an active shift, active office, and employee work assignment first. Authenticate as the employee and POST `{ "latitude": 23.1815, "longitude": 79.9864, "accuracyMeters": 12.0 }` to `/api/v1/attendance/check-in`, then use the same endpoint response and a later request to `/api/v1/attendance/check-out`. The server supplies timestamps and resolves the employee, assignment, shift, and office; client employee IDs, office IDs, timestamps, and distances are ignored. Read `/api/v1/attendance/me?date=YYYY-MM-DD` and `/api/v1/attendance/me/status`. Admins can query `/api/v1/admin/attendance`, `/api/v1/admin/attendance/summary`, and submit a correction with a session ID and reason. Coordinates are validated against the stored office snapshot, so changing the office later does not alter historical checkout validation.

To test leave management, use an admin token to allocate a paid balance:

```http
PUT /api/v1/admin/employees/{employeeId}/leave-balance
Authorization: Bearer {adminToken}
Content-Type: application/json

{"leaveType":"CL","year":2030,"allocatedDays":12}
```

The response reports `allocatedDays`, `usedDays`, `pendingDays`, and the service-calculated `availableDays`. Ensure the employee has effective work assignments covering every calendar date in the requested range. Then use the employee token:

```http
POST /api/v1/leaves
Authorization: Bearer {employeeToken}
Content-Type: application/json

{"leaveType":"CL","startDate":"2030-01-04","endDate":"2030-01-07","reason":"Family event"}
```

For a Monday-to-Friday shift this example returns a pending request with `totalDays: 2.0`, and the CL balance changes from `12/0/0/12` to `12/0/2/10` (allocated/used/pending/available). Inspect it with `GET /api/v1/leaves/me/balance?year=2030` and list requests with `GET /api/v1/leaves/me?status=PENDING&page=0&limit=20`. Approve with `PATCH /api/v1/admin/leaves/{leaveId}/approve`; no body is required. Reject with `PATCH /api/v1/admin/leaves/{leaveId}/reject` and `{"remarks":"Coverage unavailable"}`. To verify cancellation, create another pending request and call `PATCH /api/v1/leaves/{leaveId}/cancel` as its owning employee. An LWP request follows the same apply/approval workflow without an allocation or balance change.

Successful responses use the standard envelope. A balance payload resembles:

```json
{
  "success": true,
  "message": "Leave balances retrieved",
  "data": [{
    "leaveType": "CL",
    "leaveTypeName": "Casual Leave",
    "year": 2030,
    "allocatedDays": 12,
    "usedDays": 0,
    "pendingDays": 2,
    "availableDays": 10
  }],
  "timestamp": "2030-01-01T00:00:00Z"
}
```

To test holidays, create one with the admin token:

```http
POST /api/v1/admin/holidays
Authorization: Bearer {adminToken}
Content-Type: application/json

{"name":"Republic Day","description":"National holiday","date":"2030-01-26"}
```

Use `GET /api/v1/admin/holidays?year=2030&active=true&page=0&limit=20&sortDirection=asc` for the admin calendar. Update it with `PATCH /api/v1/admin/holidays/{holidayId}` and a partial body such as `{"name":"Republic Day Observed","date":"2030-01-27"}`. Deactivate it with `PATCH /api/v1/admin/holidays/{holidayId}/deactivate`; the record remains queryable to admins. Employees can call `GET /api/v1/holidays?year=2030` and `GET /api/v1/holidays/upcoming`.

Create an active holiday on a weekday covered by an employee assignment, then submit paid leave or LWP across that date. The returned `totalDays` and paid balance reservation must exclude the holiday. Adding or changing a holiday later does not mutate an already approved request.

After creating a holiday or approving/rejecting leave, use the affected employee token:

```http
GET /api/v1/notifications?page=0&limit=20
GET /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{notificationId}/read
PATCH /api/v1/notifications/read-all
```

A notification response contains only the title, message, type, optional reference ID, read state, read timestamp, and creation timestamp; ownership is always derived from the authenticated employee. With SMTP configured, new holiday, approval, and rejection email is sent after commit. To verify failure isolation, temporarily use an unreachable SMTP host: the business API should still succeed and the server should log the mail error.

To test payroll, first create a monthly salary with an admin token:

```http
PUT /api/v1/admin/employees/{employeeId}/salary
Authorization: Bearer {adminToken}
Content-Type: application/json

{"monthlyFixedSalary":75000.00,"effectiveFrom":"2030-01-01"}
```

Ensure assignments cover the whole employment portion of the target month, holidays are maintained, approved leave is correct, and every attendance session is completed. Generate without overwriting existing records:

```http
POST /api/v1/admin/payroll/generate
Authorization: Bearer {adminToken}
Content-Type: application/json

{"month":1,"year":2030}
```

The summary separates generated, skipped (existing payroll or missing salary), and failed employees (for example, missing assignments or incomplete attendance). Review the date-level `calculationDetails`. Replace draft adjustments with `PATCH /api/v1/admin/payroll/{payrollId}` and a body such as:

```json
{
  "earnings": [
    {"type":"BONUS","description":"Performance bonus","amount":5000},
    {"type":"OVERTIME_PAY","description":"Approved overtime","amount":1200}
  ],
  "deductions": [
    {"type":"OTHER_DEDUCTION","description":"Approved recovery","amount":500}
  ]
}
```

Then call `/review`, `/finalize`, and `/pay` in order. The employee can see the record only after finalization at `GET /api/v1/payroll/me`. A PDF endpoint is intentionally deferred because no PDF dependency is present; this detail response is the structured payslip and contains every requested payslip value.

Run verification with:

```powershell
mvn clean verify
```

## Deployment

Build with JDK 21 using `mvn clean verify`, then run `java -jar target/hrms-backend-0.0.1-SNAPSHOT.jar` with `SPRING_PROFILES_ACTIVE=prod` and secrets supplied by the hosting platform's secret manager. Terminate TLS at a trusted ingress/load balancer, restrict CORS to the deployed frontend, use an Atlas IP allowlist/private endpoint, and expose only required health probes. Swagger is disabled by default in production and can be deliberately enabled with `OPENAPI_ENABLED=true`.

## Delivery roadmap

1. Platform foundation — implemented
2. OTP/JWT authentication and admin bootstrap — implemented
3. Employee management — implemented
4. Departments, office locations, shifts, and effective-dated assignments — implemented
5. Geofencing, attendance, correction, reconciliation groundwork — implemented
6. Leave types, balances, requests, approvals â€” implemented
7. Holidays, notifications, email delivery — implemented
8. Salary structures, payroll calculations, adjustments, and lifecycle — implemented
9. Future explicitly approved modules only

## Known limitations

Authentication is complete, but the current in-memory IP rate limiter is process-local; production deployments with multiple instances should place rate limiting behind a shared gateway or add a shared store before scaling horizontally. OTP SMTP delivery remains synchronous and fails OTP requests when delivery fails; business notification email is post-commit, best-effort, and has no automatic retry. MongoDB transaction integration tests are skipped when Docker is unavailable. Partial-day leave, automatic accrual, post-approval employee cancellation/correction, recurring or department-specific holidays, notification preferences, push delivery, payroll scheduling, and automatic disbursement remain out of scope. Historical approved leave is intentionally not recalculated when the holiday calendar changes. Attendance does not create absence, holiday, or on-leave records. Salary selection uses the structure effective on the employee's last eligible date in the payroll month; salary changes should therefore be effective on month boundaries because mid-month salary proration is not implemented. Payroll supports full-day attendance/leave classifications only, has no statutory tax/PF/ESI/TDS engine, no PDF renderer, and no post-finalization revision workflow. Cross-instance balance and payroll writes rely on MongoDB unique indexes, transactions, and optimistic locking; clients should retry explicit concurrency conflicts. Assignment overlap locking is process-local; multi-instance deployments should serialize assignment commands before scaling assignment writes horizontally. The active machine must use JDK 21; Java 17 cannot compile this project. Multi-company payroll and bank transfer integrations are not implemented.
