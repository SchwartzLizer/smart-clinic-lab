CREATE TABLE admin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE doctor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    specialty VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE patient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    address VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE doctor_available_times (
    doctor_id BIGINT NOT NULL,
    available_times VARCHAR(50) NOT NULL,
    PRIMARY KEY (doctor_id, available_times),
    CONSTRAINT fk_available_time_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE CASCADE
);

CREATE TABLE appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_time DATETIME(6) NOT NULL,
    status INT NOT NULL DEFAULT 0,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_doctor_appointment_time UNIQUE (doctor_id, appointment_time),
    CONSTRAINT fk_appointment_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_patient
        FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE RESTRICT
);
