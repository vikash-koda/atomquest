# API Documentation

Base URL: `/api`

## Authentication

### `POST /auth/login`

Request:

```json
{ "email": "employee@demo.com", "password": "123456" }
```

Response:

```json
{ "token": "jwt", "id": 1, "name": "Vikas Employee", "email": "employee@demo.com", "role": "EMPLOYEE", "department": "Engineering" }
```

## Employee Goals

### `GET /goals`

Returns current employee goals.

### `POST /goals`

Creates a draft goal.

```json
{
  "title": "Improve delivery predictability",
  "description": "Track sprint commitments",
  "thrustArea": "Execution Excellence",
  "uomType": "PERCENT",
  "target": 100,
  "weightage": 20,
  "deadline": "2026-08-31"
}
```

### `PATCH /goals/{id}`

Updates a goal if the user is allowed and the goal is not locked.

### `POST /goals/submit`

Submits the employee goal sheet. Requires total weightage of 100%.

### `POST /goals/{id}/quarterly-updates`

Adds or updates quarterly achievement.

```json
{ "quarter": "Q2", "achievement": 72, "comment": "Progress is on track", "status": "ON_TRACK" }
```

Goal responses include the latest quarterly tracking fields when available:

```json
{
  "latestQuarter": "Q2",
  "latestProgressScore": 72,
  "latestProgressStatus": "ON_TRACK",
  "latestUpdateComment": "Progress is on track",
  "latestCompletionDate": "2026-08-31"
}
```

## Manager

### `GET /manager/team-goals`

Returns goals for employees reporting to the manager.

### `POST /manager/goals/{id}/review`

```json
{ "decision": "APPROVED", "managerNote": "Approved for tracking", "target": 100, "weightage": 30 }
```

Decision must be `APPROVED` or `REJECTED`.

### `GET /manager/team-users`

Returns the manager's direct reports for shared KPI assignment and team review.

### `POST /manager/shared-goals/push`

Pushes a shared departmental KPI to selected direct reports, or all direct reports when `employeeIds` is empty.

### `GET /manager/comments`

Returns structured manager check-in comments by employee and quarter.

## Admin / HR

### `GET /admin/users`

Lists users.

### `POST /admin/users`

Creates a user.

```json
{ "name": "New Employee", "email": "new@demo.com", "password": "123456", "role": "EMPLOYEE", "department": "Engineering" }
```

### `PATCH /admin/users/{id}`

Updates role, department, or reporting manager for org hierarchy maintenance.

```json
{ "role": "EMPLOYEE", "department": "Engineering", "managerId": 2 }
```

### `GET /admin/goals`

Lists all goals.

### `POST /admin/goals/{id}/unlock`

Unlocks a goal and returns it to draft.

### `GET /admin/shared-goals`

Lists shared goals, optionally filtered by `department`.

### `POST /admin/shared-goals`

Creates a shared goal definition.

```json
{
  "title": "Improve platform reliability score",
  "description": "Reduce avoidable incidents through better release checks",
  "thrustArea": "Operational Excellence",
  "uomType": "PERCENT",
  "target": 99.5,
  "ownerId": 3,
  "department": "Engineering",
  "deadline": "2026-08-31"
}
```

### `POST /admin/shared-goals/push`

Pushes a shared departmental KPI to selected employees, or all employees in the department when `employeeIds` is empty.

### `GET /admin/audit-logs?page=0&size=20`

Returns paginated audit logs.

## Analytics

### `GET /analytics/overview`

Returns total goals, completed goals, completion rate, status distribution, department progress, quarterly trend, and low-performing goal insights.
Also returns `checkInCompletion`, including employee achievement-capture completion and manager check-in completion by quarter.
