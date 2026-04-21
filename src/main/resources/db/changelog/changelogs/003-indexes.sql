--liquibase formatted sql

--changeset dev:003-indexes dbms:postgresql
CREATE INDEX idx_task_review_intern_id ON task_review(intern_id);
CREATE INDEX idx_task_review_status    ON task_review(status);
CREATE INDEX idx_task_review_mentor_id ON task_review(mentor_id);
