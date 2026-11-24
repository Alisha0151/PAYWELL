Schema is owned by Flyway migrations in `backend/src/main/resources/db/migration` (V1 core schema, V2 OTP + payment requests).
There is no separate init SQL: the backend creates and migrates the schema on startup.
