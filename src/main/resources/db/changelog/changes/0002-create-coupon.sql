--liquibase formatted sql

--changeset dawid:0002-create-coupon context:dev,prod
CREATE TABLE coupon (
    id           UUID        NOT NULL,
    code         VARCHAR(64) NOT NULL,
    country_code VARCHAR(2)  NOT NULL,
    max_uses     INT         NOT NULL,
    current_uses INT         NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_coupon PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code UNIQUE (code),
    CONSTRAINT ck_coupon_max_uses CHECK (max_uses > 0),
    CONSTRAINT ck_coupon_current_uses CHECK (current_uses >= 0 AND current_uses <= max_uses)
);
--rollback DROP TABLE coupon;
