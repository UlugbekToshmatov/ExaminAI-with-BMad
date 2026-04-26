--liquibase formatted sql

--changeset dev:006-stacks-and-course-stack dbms:postgresql
CREATE TABLE stack (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO stack (name) VALUES
    ('Java'),
    ('React'),
    ('TypeScript');

-- nullable during backfill; every course is assigned a row before NOT NULL
ALTER TABLE course
    ADD COLUMN stack_id BIGINT REFERENCES stack(id);
UPDATE course
SET stack_id = (SELECT id FROM stack WHERE name = 'Java')
WHERE stack_id IS NULL;
ALTER TABLE course
    ALTER COLUMN stack_id SET NOT NULL;

CREATE TABLE user_account_stack (
    user_account_id BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    stack_id        BIGINT NOT NULL REFERENCES stack(id) ON DELETE CASCADE,
    PRIMARY KEY (user_account_id, stack_id)
);

-- Seed: existing demo intern can access Java course tasks
INSERT INTO user_account_stack (user_account_id, stack_id)
SELECT u.id, s.id
FROM user_account u
CROSS JOIN stack s
WHERE u.username = 'intern'
  AND s.name = 'Java';
