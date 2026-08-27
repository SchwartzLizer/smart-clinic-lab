USE cms;

-- Training data only. Production passwords must be hashed before storage.
INSERT IGNORE INTO doctor (id, email, name, password, phone, specialty) VALUES
(1, 'dr.adams@example.com', 'Dr. Emily Adams', 'pass12345', '555-101-2020', 'Cardiologist'),
(2, 'dr.johnson@example.com', 'Dr. Mark Johnson', 'secure4567', '555-202-3030', 'Neurologist'),
(3, 'dr.lee@example.com', 'Dr. Sarah Lee', 'leePass987', '555-303-4040', 'Orthopedist'),
(4, 'dr.wilson@example.com', 'Dr. Tom Wilson', 'w!ls0nPwd', '555-404-5050', 'Pediatrician'),
(5, 'dr.brown@example.com', 'Dr. Alice Brown', 'brownie123', '555-505-6060', 'Dermatologist'),
(6, 'dr.taylor@example.com', 'Dr. Taylor Grant', 'taylor321', '555-606-7070', 'Cardiologist'),
(7, 'dr.white@example.com', 'Dr. Sam White', 'whiteSecure1', '555-707-8080', 'Neurologist'),
(8, 'dr.clark@example.com', 'Dr. Emma Clark', 'clarkPass456', '555-808-9090', 'Orthopedist'),
(9, 'dr.davis@example.com', 'Dr. Olivia Davis', 'davis789', '555-909-0101', 'Pediatrician'),
(10, 'dr.miller@example.com', 'Dr. Henry Miller', 'millertime!', '555-010-1111', 'Dermatologist');

INSERT IGNORE INTO doctor_available_times (doctor_id, available_times) VALUES
(1, '09:00-10:00'), (1, '10:00-11:00'), (1, '14:00-15:00'),
(2, '10:00-11:00'), (2, '11:00-12:00'), (2, '15:00-16:00'),
(3, '09:00-10:00'), (3, '11:00-12:00'), (3, '16:00-17:00'),
(4, '09:00-10:00'), (4, '15:00-16:00'), (4, '16:00-17:00'),
(5, '09:00-10:00'), (5, '10:00-11:00'), (5, '14:00-15:00'),
(6, '09:00-10:00'), (6, '11:00-12:00'), (6, '14:00-15:00'),
(7, '10:00-11:00'), (7, '15:00-16:00'),
(8, '11:00-12:00'), (8, '14:00-15:00'),
(9, '09:00-10:00'), (9, '13:00-14:00'),
(10, '10:00-11:00'), (10, '16:00-17:00');

INSERT IGNORE INTO patient (id, address, email, name, password, phone) VALUES
(1, '101 Oak St, Cityville', 'jane.doe@example.com', 'Jane Doe', 'passJane1', '888-111-1111'),
(2, '202 Maple Rd, Townsville', 'john.smith@example.com', 'John Smith', 'smithSecure', '888-222-2222'),
(3, '303 Pine Ave, Villageton', 'emily.rose@example.com', 'Emily Rose', 'emilyPass99', '888-333-3333'),
(4, '404 Birch Ln, Metropolis', 'michael.j@example.com', 'Michael Jordan', 'airmj23', '888-444-4444'),
(5, '505 Cedar Blvd, Springfield', 'olivia.m@example.com', 'Olivia Moon', 'moonshine12', '888-555-5555'),
(6, '606 Spruce Ct, Gotham', 'liam.k@example.com', 'Liam King', 'king321', '888-666-6666'),
(7, '707 Aspen Dr, Riverdale', 'sophia.l@example.com', 'Sophia Lane', 'sophieLane', '888-777-7777'),
(8, '808 Elm St, Newtown', 'noah.b@example.com', 'Noah Brooks', 'noahBest!', '888-888-8888'),
(9, '909 Willow Way, Star City', 'ava.d@example.com', 'Ava Daniels', 'avaSecure8', '888-999-9999'),
(10, '111 Chestnut Pl, Midvale', 'william.h@example.com', 'William Harris', 'willH2025', '888-000-0000');

INSERT IGNORE INTO appointment (id, appointment_time, status, doctor_id, patient_id) VALUES
(1, '2025-04-01 09:00:00', 1, 1, 1),
(2, '2025-04-02 10:00:00', 1, 1, 2),
(3, '2025-04-03 11:00:00', 1, 1, 3),
(4, '2025-04-04 14:00:00', 1, 1, 4),
(5, '2025-04-05 10:00:00', 1, 1, 5),
(6, '2025-04-10 10:00:00', 1, 1, 6),
(7, '2025-04-11 09:00:00', 1, 1, 7),
(8, '2025-04-15 09:00:00', 1, 1, 8),
(9, '2025-04-01 10:00:00', 1, 2, 1),
(10, '2025-04-02 11:00:00', 1, 2, 2),
(11, '2025-04-03 14:00:00', 1, 2, 3),
(12, '2025-04-04 15:00:00', 1, 2, 4),
(13, '2025-04-05 11:00:00', 1, 2, 5),
(14, '2025-04-15 10:00:00', 1, 2, 6),
(15, '2025-04-01 11:00:00', 1, 3, 1),
(16, '2025-04-02 14:00:00', 1, 3, 2),
(17, '2025-04-03 16:00:00', 1, 3, 3),
(18, '2025-04-15 11:00:00', 1, 3, 4),
(19, '2025-04-04 16:00:00', 1, 4, 5),
(20, '2025-04-05 15:00:00', 1, 4, 6),
(21, '2025-04-15 15:00:00', 1, 4, 7),
(22, '2025-04-06 14:00:00', 1, 5, 8),
(23, '2025-04-07 09:00:00', 1, 5, 9),
(24, '2025-04-08 10:00:00', 1, 6, 10),
(25, '2025-05-01 09:00:00', 0, 1, 1),
(26, '2025-05-02 10:00:00', 0, 2, 2),
(27, '2025-05-03 11:00:00', 0, 3, 3),
(28, '2025-05-04 15:00:00', 0, 4, 4),
(29, '2025-05-05 14:00:00', 0, 5, 5),
(30, '2025-05-06 11:00:00', 0, 6, 6);

INSERT IGNORE INTO admin (id, username, password)
VALUES (1, 'admin', 'admin@1234');

-- Assignment verification commands:
-- SHOW TABLES;
-- SELECT * FROM patient LIMIT 5;
