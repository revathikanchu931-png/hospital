CREATE TABLE users (
  id BINARY(16) NOT NULL PRIMARY KEY,
  name VARCHAR(140) NOT NULL,
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  phone VARCHAR(30),
  role VARCHAR(20) NOT NULL,
  verification_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_users_email UNIQUE (email),
  INDEX idx_user_role_active (role, active),
  INDEX idx_user_verification (verification_status)
) ENGINE=InnoDB;

CREATE TABLE patient_profiles (
  user_id BINARY(16) NOT NULL PRIMARY KEY,
  birth_date DATE,
  gender VARCHAR(30),
  address VARCHAR(500),
  CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE doctor_profiles (
  user_id BINARY(16) NOT NULL PRIMARY KEY,
  specialization VARCHAR(120) NOT NULL,
  qualification VARCHAR(200) NOT NULL,
  experience_years INT NOT NULL DEFAULT 0,
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  clinic_name VARCHAR(200),
  city VARCHAR(100),
  registration_number VARCHAR(100) NOT NULL,
  CONSTRAINT uk_doctor_registration UNIQUE (registration_number),
  CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE laboratory_profiles (
  user_id BINARY(16) NOT NULL PRIMARY KEY,
  contact_person VARCHAR(140) NOT NULL,
  address VARCHAR(500) NOT NULL,
  license_details VARCHAR(200) NOT NULL,
  home_collection BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT uk_lab_license UNIQUE (license_details),
  CONSTRAINT fk_lab_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE pharmacy_profiles (
  user_id BINARY(16) NOT NULL PRIMARY KEY,
  contact_person VARCHAR(140) NOT NULL,
  address VARCHAR(500) NOT NULL,
  license_details VARCHAR(200) NOT NULL,
  CONSTRAINT uk_pharmacy_license UNIQUE (license_details),
  CONSTRAINT fk_pharmacy_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE doctor_availability (
  id BINARY(16) NOT NULL PRIMARY KEY,
  doctor_id BINARY(16) NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uk_doctor_slot UNIQUE (doctor_id, slot_date, start_time),
  CONSTRAINT fk_slot_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  INDEX idx_doctor_slot_date (doctor_id, slot_date, available)
) ENGINE=InnoDB;

CREATE TABLE appointments (
  id BINARY(16) NOT NULL PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  doctor_id BINARY(16) NOT NULL,
  availability_id BINARY(16) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_appointment_slot UNIQUE (availability_id),
  CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
  CONSTRAINT fk_appointment_slot FOREIGN KEY (availability_id) REFERENCES doctor_availability(id),
  INDEX idx_appointments_patient (patient_id, status),
  INDEX idx_appointments_doctor (doctor_id, status)
) ENGINE=InnoDB;

CREATE TABLE hospitals (
  id BINARY(16) NOT NULL PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  city VARCHAR(100) NOT NULL,
  address VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
CREATE TABLE provider_verifications (
  id BINARY(16) NOT NULL PRIMARY KEY,
  provider_id BINARY(16) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reviewer_id BINARY(16),
  reviewed_at TIMESTAMP(6),
  notes VARCHAR(500),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_verification_provider FOREIGN KEY (provider_id) REFERENCES users(id),
  CONSTRAINT fk_verification_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  INDEX idx_verification_status (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE diagnostic_tests (
  id BINARY(16) NOT NULL PRIMARY KEY,
  laboratory_id BINARY(16) NOT NULL,
  name VARCHAR(160) NOT NULL,
  description TEXT,
  price_inr DECIMAL(10,2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_test_lab FOREIGN KEY (laboratory_id) REFERENCES users(id),
  INDEX idx_tests_lab_active (laboratory_id, active)
) ENGINE=InnoDB;
CREATE TABLE lab_availability (
  id BINARY(16) NOT NULL PRIMARY KEY,
  laboratory_id BINARY(16) NOT NULL,
  slot_date DATE NOT NULL,
  start_time TIME NOT NULL,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_lab_slot FOREIGN KEY (laboratory_id) REFERENCES users(id),
  UNIQUE KEY uk_lab_slot (laboratory_id, slot_date, start_time)
) ENGINE=InnoDB;
CREATE TABLE lab_bookings (
  id BINARY(16) NOT NULL PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  laboratory_id BINARY(16) NOT NULL,
  availability_id BINARY(16),
  status VARCHAR(24) NOT NULL DEFAULT 'BOOKED',
  home_collection BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_lab_booking_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_lab_booking_lab FOREIGN KEY (laboratory_id) REFERENCES users(id),
  CONSTRAINT fk_lab_booking_slot FOREIGN KEY (availability_id) REFERENCES lab_availability(id),
  INDEX idx_lab_booking_patient (patient_id, status)
) ENGINE=InnoDB;
CREATE TABLE lab_booking_items (
  booking_id BINARY(16) NOT NULL,
  test_id BINARY(16) NOT NULL,
  price_inr DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (booking_id, test_id),
  CONSTRAINT fk_lab_item_booking FOREIGN KEY (booking_id) REFERENCES lab_bookings(id) ON DELETE CASCADE,
  CONSTRAINT fk_lab_item_test FOREIGN KEY (test_id) REFERENCES diagnostic_tests(id)
) ENGINE=InnoDB;
CREATE TABLE lab_reports (
  id BINARY(16) NOT NULL PRIMARY KEY,
  booking_id BINARY(16) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  released_at TIMESTAMP(6),
  CONSTRAINT fk_report_booking FOREIGN KEY (booking_id) REFERENCES lab_bookings(id)
) ENGINE=InnoDB;

CREATE TABLE medicines (
  id BINARY(16) NOT NULL PRIMARY KEY,
  pharmacy_id BINARY(16) NOT NULL,
  name VARCHAR(180) NOT NULL,
  category VARCHAR(100) NOT NULL,
  description TEXT,
  price_inr DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  prescription_required BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_medicine_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES users(id),
  INDEX idx_medicine_catalog (active, name)
) ENGINE=InnoDB;
CREATE TABLE cart_items (
  id BINARY(16) NOT NULL PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  medicine_id BINARY(16) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT uk_cart_item UNIQUE (patient_id, medicine_id),
  CONSTRAINT fk_cart_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id)
) ENGINE=InnoDB;
CREATE TABLE prescriptions (
  id BINARY(16) NOT NULL PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  review_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  reviewed_by BINARY(16),
  reviewed_at TIMESTAMP(6),
  CONSTRAINT fk_prescription_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_prescription_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
) ENGINE=InnoDB;
CREATE TABLE medicine_orders (
  id BINARY(16) NOT NULL PRIMARY KEY,
  patient_id BINARY(16) NOT NULL,
  pharmacy_id BINARY(16) NOT NULL,
  prescription_id BINARY(16),
  delivery_address VARCHAR(500) NOT NULL,
  total_inr DECIMAL(10,2) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'PLACED',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_order_patient FOREIGN KEY (patient_id) REFERENCES users(id),
  CONSTRAINT fk_order_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES users(id),
  CONSTRAINT fk_order_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id),
  INDEX idx_order_patient (patient_id, created_at)
) ENGINE=InnoDB;
CREATE TABLE medicine_order_items (
  id BINARY(16) NOT NULL PRIMARY KEY,
  order_id BINARY(16) NOT NULL,
  medicine_id BINARY(16) NOT NULL,
  medicine_name VARCHAR(180) NOT NULL,
  quantity INT NOT NULL,
  unit_price_inr DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES medicine_orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_item_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id)
) ENGINE=InnoDB;
CREATE TABLE audit_logs (
  id BINARY(16) NOT NULL PRIMARY KEY,
  actor_id BINARY(16),
  action VARCHAR(120) NOT NULL,
  target_type VARCHAR(80),
  target_id VARCHAR(80),
  details JSON,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
  INDEX idx_audit_actor_time (actor_id, created_at)
) ENGINE=InnoDB;
