# ATOMQUEST Architecture

## Folder Structure

```text
backend/
  src/main/java/com/atomquest/portal/
    config/       Security, CORS, seed data
    controller/   REST API controllers
    dto/          Request and response records
    entity/       JPA entities and enums
    exception/    Global API errors
    repository/   Spring Data repositories
    security/     JWT and UserDetails integration
    service/      Business rules and orchestration
frontend/
  src/
    api/          Axios client
    components/   UI, layout, charts
    lib/          Utilities
    pages/        Route-level pages
    store/        Zustand auth store
    types/        Domain types
docs/
  API.md
  ARCHITECTURE.md
```

## Entity Relationships

- `User` can have a manager, which is another `User`.
- `Goal` belongs to an employee `User`.
- `Goal` may reference a `SharedGoal`.
- `QuarterlyUpdate` belongs to a `Goal` and is unique by goal plus quarter.
- `ManagerComment` connects manager, employee, and quarter.
- `AuditLog` optionally references the acting `User`.
- `SharedGoal` has an owner `User` and department.

## JWT Flow

1. Client posts email/password to `POST /api/auth/login`.
2. Spring Security authenticates with BCrypt password matching.
3. Backend signs a JWT with user id and role claims.
4. Frontend stores token in Zustand persisted storage.
5. Axios adds `Authorization: Bearer <token>` to protected requests.
6. `JwtAuthenticationFilter` validates the token and loads the user for authorization.

## API Architecture

- `/api/auth`: public authentication.
- `/api/goals`: employee goal CRUD, submission, quarterly check-ins.
- `/api/manager`: team goal review and approval workflow.
- `/api/admin`: user management, shared goals, unlocks, audit logs.
- `/api/analytics`: aggregate dashboard data.

## Implementation Roadmap

1. Build entities, repositories, DTOs, JWT security, and seed data.
2. Implement goal rules and approval workflow in services.
3. Add audit logging around critical actions.
4. Create role-based React shell and login.
5. Build goal, check-in, analytics, admin, and audit pages.
6. Verify backend compile and frontend production build.
7. Deploy backend to Render, frontend to Vercel, database to Neon.
