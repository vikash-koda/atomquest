# Deployment Guide

## Neon PostgreSQL

1. Create a Neon project.
2. Create a database named `atomquest`.
3. Copy host, database, username, and password.
4. Build a JDBC URL:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

## Render Backend

1. Create a new Web Service from this repository.
2. Root directory: `backend`
3. Build command: `mvn clean package -DskipTests`
4. Start command: `java -jar target/goal-tracking-portal-1.0.0.jar`
5. Add environment variables:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`
   - `CORS_ALLOWED_ORIGINS`

## Vercel Frontend

1. Import the repository into Vercel.
2. Root directory: `frontend`
3. Build command: `npm run build`
4. Output directory: `dist`
5. Add `VITE_API_URL=https://<render-service>.onrender.com/api`

## Production Notes

- Use a long random `JWT_SECRET`.
- Set exact Vercel origin in `CORS_ALLOWED_ORIGINS`.
- Keep Spring `ddl-auto=update` for hackathon speed; switch to Flyway for a longer-lived production project.
