CREATE DATABASE IF NOT EXISTS mediconnect_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'mediconnect'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON mediconnect_db.* TO 'mediconnect'@'localhost';
FLUSH PRIVILEGES;
