DROP PROCEDURE IF EXISTS GetDailyAppointmentReportByDoctor;
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByMonth;
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByYear;

CREATE PROCEDURE GetDailyAppointmentReportByDoctor(IN report_date DATE)
SELECT
    d.name AS doctor_name,
    a.appointment_time,
    a.status,
    p.name AS patient_name,
    p.phone AS patient_phone
FROM appointment a
JOIN doctor d ON a.doctor_id = d.id
JOIN patient p ON a.patient_id = p.id
WHERE DATE(a.appointment_time) = report_date
ORDER BY d.name, a.appointment_time;

CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(IN input_month INT, IN input_year INT)
SELECT doctor_id, COUNT(patient_id) AS patients_seen
FROM appointment
WHERE MONTH(appointment_time) = input_month
  AND YEAR(appointment_time) = input_year
GROUP BY doctor_id
ORDER BY patients_seen DESC
LIMIT 1;

CREATE PROCEDURE GetDoctorWithMostPatientsByYear(IN input_year INT)
SELECT doctor_id, COUNT(patient_id) AS patients_seen
FROM appointment
WHERE YEAR(appointment_time) = input_year
GROUP BY doctor_id
ORDER BY patients_seen DESC
LIMIT 1;
