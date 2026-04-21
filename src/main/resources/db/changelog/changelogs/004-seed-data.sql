--liquibase formatted sql

--changeset dev:004-seed-data dbms:postgresql
-- Seed users (passwords BCrypt strength 12)
INSERT INTO user_account (username, password, email, role, active, date_created)
VALUES
  ('admin',  '$2a$12$oo33.H/qBIcMNLS2vWv/MuZwY0LoEQreHslyw8XSoNDh3NGTBYb1W',  'admin@examinai.local',  'ADMIN',  true, now()),
  ('mentor', '$2a$12$mj7I6R4FZqKlnuEe4wlOqucUV9KYLLDNa7fCEBCkAITgbGTvqPDQK', 'mentor@examinai.local', 'MENTOR', true, now()),
  ('intern', '$2a$12$TsTLqwyjIpMct94zfB1wD.HuudUW/Ky7qnHW7AhpGGn4DYgbAxzCS', 'intern@examinai.local', 'INTERN', true, now());

-- Seed course
INSERT INTO course (course_name, technology, date_created)
VALUES ('Spring Boot Fundamentals', 'Java', now());

-- Seed tasks (FK refs to course and mentor via subquery)
INSERT INTO task (task_name, task_description, course_id, mentor_id, date_created)
VALUES
  ('Build a REST API',
   'Implement a RESTful API using Spring Boot, JPA, and PostgreSQL. Apply CRUD operations on a domain entity with proper HTTP status codes.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now()),
  ('Implement Spring Security',
   'Add form-based authentication to an existing Spring Boot application. Implement role-based access control for at least two user roles.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now()),
  ('Write Unit Tests',
   'Write comprehensive unit tests for the service layer using JUnit 5 and Mockito. Achieve at least 80% line coverage on the tested classes.',
   (SELECT id FROM course WHERE course_name = 'Spring Boot Fundamentals'),
   (SELECT id FROM user_account WHERE username = 'mentor'),
   now());
