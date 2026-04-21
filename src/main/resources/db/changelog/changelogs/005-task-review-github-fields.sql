--liquibase formatted sql

--changeset dev:005-task-review-github-fields dbms:postgresql
ALTER TABLE task_review ADD COLUMN github_repo_owner VARCHAR(255);
ALTER TABLE task_review ADD COLUMN github_repo_name VARCHAR(255);
ALTER TABLE task_review ADD COLUMN github_pr_number INTEGER;
