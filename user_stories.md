# Smart Clinic Management System User Stories

## Admin stories

### 1. Admin login
**User story:** As an Admin, I want to log in with my username and password so that I can securely manage the clinic.

**Acceptance criteria:**
1. Valid credentials return a signed token and open the Admin Dashboard.
2. Invalid credentials return an unauthorized response.
3. Protected admin actions reject missing or invalid tokens.

**Priority:** High  
**Story points:** 3

### 2. Add a doctor
**User story:** As an Admin, I want to add a doctor with contact, specialty, and availability information so that patients can book that doctor.

**Acceptance criteria:**
1. Required doctor fields are validated.
2. A unique email creates a doctor record.
3. A duplicate email returns a conflict response.

**Priority:** High  
**Story points:** 5

### 3. View and search doctors
**User story:** As an Admin, I want to view and search all doctors so that I can quickly find a clinic provider.

**Acceptance criteria:**
1. The dashboard lists every doctor.
2. Search matches partial doctor names without case sensitivity.
3. Specialty and AM/PM filters narrow the results.

**Priority:** High  
**Story points:** 5

### 4. Update a doctor
**User story:** As an Admin, I want to update a doctor's profile and availability so that clinic information remains accurate.

**Acceptance criteria:**
1. An existing doctor can be updated.
2. An unknown doctor ID returns not found.
3. Validation rules also apply to updates.

**Priority:** Medium  
**Story points:** 3

### 5. Delete a doctor
**User story:** As an Admin, I want to delete a doctor so that former providers no longer appear in the clinic.

**Acceptance criteria:**
1. Only an authenticated Admin can delete a doctor.
2. Related appointments are removed before the doctor.
3. An unknown doctor ID returns not found.

**Priority:** Medium  
**Story points:** 3

## Doctor stories

### 6. Doctor login
**User story:** As a Doctor, I want to log in with my email and password so that I can access my appointment dashboard.

**Acceptance criteria:**
1. Valid credentials return a signed token and doctor ID.
2. Invalid credentials return an unauthorized response.
3. The token grants access only to doctor functions.

**Priority:** High  
**Story points:** 3

### 7. View daily appointments
**User story:** As a Doctor, I want to view my appointments for a selected date so that I can plan my workday.

**Acceptance criteria:**
1. Results include only the authenticated doctor's appointments.
2. Results are limited to the selected date.
3. Each row includes patient and appointment details.

**Priority:** High  
**Story points:** 5

### 8. Search appointments by patient
**User story:** As a Doctor, I want to search daily appointments by patient name so that I can find a patient quickly.

**Acceptance criteria:**
1. Search supports partial patient names.
2. Search is case-insensitive.
3. Date and doctor restrictions remain applied.

**Priority:** Medium  
**Story points:** 3

### 9. Add a prescription
**User story:** As a Doctor, I want to add a prescription to an appointment so that the patient's treatment is recorded.

**Acceptance criteria:**
1. Prescription fields are validated.
2. Only an authenticated Doctor can save a prescription.
3. A successful prescription marks the appointment completed.

**Priority:** High  
**Story points:** 5

### 10. View a prescription
**User story:** As a Doctor, I want to retrieve a prescription by appointment ID so that I can review previous treatment.

**Acceptance criteria:**
1. The endpoint requires a valid doctor token.
2. Matching prescription details are returned.
3. An empty result is returned when no prescription exists.

**Priority:** Medium  
**Story points:** 2

## Patient stories

### 11. Patient registration
**User story:** As a Patient, I want to create an account so that I can use Smart Clinic services.

**Acceptance criteria:**
1. Name, email, password, phone, and address are validated.
2. Duplicate email or phone is rejected.
3. Valid details create a patient record.

**Priority:** High  
**Story points:** 5

### 12. Patient login
**User story:** As a Patient, I want to log in with my email and password so that I can manage my appointments.

**Acceptance criteria:**
1. Valid credentials return a signed token and patient ID.
2. Invalid credentials return an unauthorized response.
3. The token grants access only to permitted patient data.

**Priority:** High  
**Story points:** 3

### 13. Find a doctor
**User story:** As a Patient, I want to search doctors by name, specialty, and time so that I can choose a suitable provider.

**Acceptance criteria:**
1. Doctors can be viewed without logging in.
2. Name and specialty matching is case-insensitive.
3. AM/PM filtering uses each doctor's available time slots.

**Priority:** High  
**Story points:** 5

### 14. Book or update an appointment
**User story:** As a Patient, I want to book or update an appointment so that I can receive care at an available time.

**Acceptance criteria:**
1. The requested doctor and time slot are validated.
2. A valid available slot creates or updates the appointment.
3. A booked or invalid slot returns a conflict response.

**Priority:** High  
**Story points:** 8

### 15. View and cancel appointments
**User story:** As a Patient, I want to view, filter, and cancel my appointments so that I can manage my clinic visits.

**Acceptance criteria:**
1. Only appointments owned by the authenticated patient are returned.
2. Appointments can be filtered by doctor and status.
3. A patient can cancel only an appointment they own.

**Priority:** High  
**Story points:** 5
