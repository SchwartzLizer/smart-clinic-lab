# MySQL initialization mapping

Original Coursera SQL remains assignment evidence. Runtime initialization uses Flyway from the application classpath:

| Course artifact | Flyway migration | Runtime purpose |
| --- | --- | --- |
| `01-schema.sql` | `V1__create_smart_clinic_schema.sql` | Tables, constraints, and availability collection |
| `03-stored-procedures.sql` | `V2__create_stored_procedures.sql` | Reporting procedures retained for the course domain |
| safe rows from `02-seed-data.sql` | `V3__seed_demo_accounts.sql` | Public local demo accounts and appointments with BCrypt hashes |

The old scripts are not executed by Docker Compose. Hibernate validates the Flyway schema with `ddl-auto=validate`. Demo password for local-only accounts is `password`; provider deployments must use separate secrets and a clean database.
