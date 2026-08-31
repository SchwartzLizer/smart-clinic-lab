# API examples

These examples use a bearer header. The token is never placed in a path or query string.

```bash
BASE_URL=http://localhost:8080

# Login and keep the returned token in a shell variable.
TOKEN=$(curl --silent --fail "$BASE_URL/api/auth/patients/login" \
  -H 'Content-Type: application/json' \
  --data '{"email":"jane.doe@example.com","password":"password"}' | jq -r .token)

# Public doctor directory.
curl --fail "$BASE_URL/api/doctors?specialty=Cardiologist&page=0&size=10"

# Current patient profile.
curl --fail "$BASE_URL/api/patients/me" \
  -H "Authorization: Bearer $TOKEN"

# Book a future slot; replace doctorId and timestamp with a returned slot.
curl --fail "$BASE_URL/api/appointments" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"doctorId":1,"appointmentTime":"2030-01-02T09:00:00"}'
```

For admin/doctor workflows, call the corresponding login route and replace `TOKEN`. Error responses use `application/problem+json`.
