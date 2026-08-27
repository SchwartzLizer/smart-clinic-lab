# Smart Clinic Database Design

## MySQL Database Design

MySQL stores the clinic's structured operational data. Patient and doctor records are retained when they have appointment history; the application should deactivate those accounts instead of deleting referenced rows. Appointment scheduling must also check for overlapping time ranges in the service layer.

### Table: patients

- `id`: BIGINT, Primary Key, Auto Increment
- `name`: VARCHAR(100), Not Null
- `email`: VARCHAR(255), Not Null, Unique
- `password`: VARCHAR(255), Not Null
- `phone`: VARCHAR(10), Not Null, Unique
- `address`: VARCHAR(255), Not Null

### Table: doctors

- `id`: BIGINT, Primary Key, Auto Increment
- `name`: VARCHAR(100), Not Null
- `specialty`: VARCHAR(50), Not Null
- `email`: VARCHAR(255), Not Null, Unique
- `password`: VARCHAR(255), Not Null
- `phone`: VARCHAR(10), Not Null, Unique

Doctor availability is stored separately because `availableTimes` is an `@ElementCollection` in the Java model.

### Table: doctor_available_times

- `doctor_id`: BIGINT, Foreign Key -> `doctors(id)`, Not Null
- `available_times`: VARCHAR(50), Not Null
- Primary Key: (`doctor_id`, `available_times`)
- On doctor deletion: Cascade, because availability slots have no meaning without their doctor

### Table: appointments

- `id`: BIGINT, Primary Key, Auto Increment
- `doctor_id`: BIGINT, Foreign Key -> `doctors(id)`, Not Null
- `patient_id`: BIGINT, Foreign Key -> `patients(id)`, Not Null
- `appointment_time`: DATETIME, Not Null
- `status`: INT, Not Null, Default 0 (`0` = Scheduled, `1` = Completed)
- Unique constraint: (`doctor_id`, `appointment_time`) to prevent two appointments starting for one doctor at the same time
- On doctor or patient deletion: Restrict, preserving appointment history

### Table: admin

- `id`: BIGINT, Primary Key, Auto Increment
- `username`: VARCHAR(100), Not Null, Unique
- `password`: VARCHAR(255), Not Null

Email and phone formats are validated by the Java model. Password columns store password hashes, never plain-text passwords.

## MongoDB Collection Design

MongoDB stores prescriptions because doctor notes and medication metadata can evolve without changing the relational appointment schema. Documents keep relational IDs rather than embedding full patient or doctor records, avoiding stale copies of personal data.

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc1234567890abcdef123')",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500 mg",
  "doctorNotes": "Take one tablet every six hours with food."
}
```

`appointmentId` links the document to the MySQL appointment. The application validates that the appointment exists before saving a prescription. `doctorNotes` remains optional so the document can support prescriptions that need no additional instructions.
