# Smart Clinic Final Assignment Answers

Repository: https://github.com/SchwartzLizer/java-database-capstone

## Public links

1. User stories: https://github.com/SchwartzLizer/java-database-capstone/issues
2. Schema design: https://github.com/SchwartzLizer/java-database-capstone/blob/main/schema-design.md
3. Doctor.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/models/Doctor.java
4. Appointment.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/models/Appointment.java
5. DoctorController.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/controllers/DoctorController.java
6. AppointmentService.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/services/AppointmentService.java
7. PrescriptionController.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/controllers/PrescriptionController.java
8. PatientRepository.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/repo/PatientRepository.java
9. TokenService.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/services/TokenService.java
10. DoctorService.java: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/src/main/java/com/project/back_end/services/DoctorService.java
11. Dockerfile: https://github.com/SchwartzLizer/java-database-capstone/blob/main/app/Dockerfile
12. Maven workflow: https://github.com/SchwartzLizer/java-database-capstone/blob/main/.github/workflows/compile-backend.yml

## Screenshot questions

- Q13: capture the Admin Portal login modal on the landing page.
- Q14: capture the Doctor Portal login modal on the landing page.
- Q15: capture the Patient Portal login screen.
- Q16: capture Admin Dashboard with the Add Doctor modal open.
- Q17: capture Patient Dashboard after finding a doctor by name.
- Q18: capture Doctor Dashboard showing the appointment table.

## Verified MySQL outputs

### Q19 — SHOW TABLES

```text
admin
appointment
doctor
doctor_available_times
patient
```

### Q20 — exactly five Patient records

```text
1 | Jane Doe       | jane.doe@example.com   | 888-111-1111
2 | John Smith     | john.smith@example.com | 888-222-2222
3 | Emily Rose     | emily.rose@example.com | 888-333-3333
4 | Michael Jordan | michael.j@example.com  | 888-444-4444
5 | Olivia Moon    | olivia.m@example.com   | 888-555-5555
```

### Q21 — GetDailyAppointmentReportByDoctor('2025-04-15')

```text
+------------------+----------------------------+--------+----------------+---------------+
| doctor_name      | appointment_time           | status | patient_name   | patient_phone |
+------------------+----------------------------+--------+----------------+---------------+
| Dr. Emily Adams  | 2025-04-15 09:00:00.000000 |      1 | Noah Brooks    | 888-888-8888  |
| Dr. Mark Johnson | 2025-04-15 10:00:00.000000 |      1 | Liam King      | 888-666-6666  |
| Dr. Sarah Lee    | 2025-04-15 11:00:00.000000 |      1 | Michael Jordan | 888-444-4444  |
| Dr. Tom Wilson   | 2025-04-15 15:00:00.000000 |      1 | Sophia Lane    | 888-777-7777  |
+------------------+----------------------------+--------+----------------+---------------+
```

### Q22 — GetDoctorWithMostPatientsByMonth(4, 2025)

```text
doctor_id | patients_seen
1         | 8
```

### Q23 — GetDoctorWithMostPatientsByYear(2025)

```text
doctor_id | patients_seen
1         | 9
```

## Runtime curl questions

Run these after starting the Spring Boot app:

```bash
curl http://localhost:8080/doctor
curl http://localhost:8080/patient/{patientId}/patient/{token}
curl http://localhost:8080/doctor/filter/all/AM/Cardiologist
```

Q24-Q26 require live runtime output; do not submit the command text alone.
