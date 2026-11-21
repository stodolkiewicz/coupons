--liquibase formatted sql

--changeset dawid:0004-insert-users context:dev
INSERT INTO users (id, version, first_name, last_name)
VALUES ('3f6c1a52-8b0e-4d7a-9c21-5e4b7f0a1d01', 0, 'Jan', 'Kowalski');
--rollback DELETE FROM users WHERE id = '3f6c1a52-8b0e-4d7a-9c21-5e4b7f0a1d01';
