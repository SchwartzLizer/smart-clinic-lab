-- Repair the original demo seed hash so the documented local password works.
-- Public local demo password: password. These are not production credentials.
SET @demo_password = '$2a$10$5zoDZvoG2nXzqQDv3DWpDeM//YSnm0b5fHf.CHDCJCEhxiOTItkwa';

UPDATE admin
SET password = @demo_password
WHERE id = 1 AND username = 'admin';

UPDATE doctor
SET password = @demo_password
WHERE id BETWEEN 1 AND 10
  AND email IN (
    'dr.adams@example.com',
    'dr.johnson@example.com',
    'dr.lee@example.com',
    'dr.wilson@example.com',
    'dr.brown@example.com',
    'dr.taylor@example.com',
    'dr.white@example.com',
    'dr.clark@example.com',
    'dr.davis@example.com',
    'dr.miller@example.com'
  );

UPDATE patient
SET password = @demo_password
WHERE id BETWEEN 1 AND 10
  AND email IN (
    'jane.doe@example.com',
    'john.smith@example.com',
    'emily.rose@example.com',
    'michael.j@example.com',
    'olivia.m@example.com',
    'liam.k@example.com',
    'sophia.l@example.com',
    'noah.b@example.com',
    'ava.d@example.com',
    'william.h@example.com'
  );

SET @demo_password = NULL;
