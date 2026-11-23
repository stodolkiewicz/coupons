--liquibase formatted sql

--changeset dawid:0001-create-users context:dev,prod,test
CREATE TABLE users (
    id         UUID         NOT NULL,
    version    BIGINT       NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);
--rollback DROP TABLE users;
