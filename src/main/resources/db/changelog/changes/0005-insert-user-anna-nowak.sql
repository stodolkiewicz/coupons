--liquibase formatted sql

--changeset dawid:0005-insert-user-anna-nowak context:dev
INSERT INTO users (id, version, first_name, last_name)
VALUES ('b35b8ef7-2bf9-4de9-b5fe-592d6e87e5bc', 0, 'Anna', 'Nowak');
--rollback DELETE FROM users WHERE id = 'b35b8ef7-2bf9-4de9-b5fe-592d6e87e5bc';
