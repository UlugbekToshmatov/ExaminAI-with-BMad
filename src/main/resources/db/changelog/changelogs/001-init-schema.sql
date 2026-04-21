--liquibase formatted sql

--changeset dev:001-init-schema dbms:postgresql
CREATE TABLE user_account (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    email        VARCHAR(255),
    role         VARCHAR(20)  NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    date_created TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE course (
    id           BIGSERIAL    PRIMARY KEY,
    course_name  VARCHAR(255) NOT NULL,
    technology   VARCHAR(100),
    date_created TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE task (
    id               BIGSERIAL    PRIMARY KEY,
    task_name        VARCHAR(255) NOT NULL,
    task_description TEXT,
    course_id        BIGINT       NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    mentor_id        BIGINT       NOT NULL REFERENCES user_account(id),
    date_created     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE task_review (
    id              BIGSERIAL    PRIMARY KEY,
    task_id         BIGINT       NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    intern_id       BIGINT       NOT NULL REFERENCES user_account(id),
    mentor_id       BIGINT       REFERENCES user_account(id),
    status          VARCHAR(20)  NOT NULL,
    llm_result      VARCHAR(20),
    mentor_result   VARCHAR(20),
    mentor_remarks  TEXT,
    error_message   VARCHAR(500),
    date_created    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE task_review_issue (
    id             BIGSERIAL PRIMARY KEY,
    task_review_id BIGINT    NOT NULL REFERENCES task_review(id) ON DELETE CASCADE,
    line           INTEGER,
    code           TEXT,
    issue          TEXT      NOT NULL,
    improvement    TEXT
);
