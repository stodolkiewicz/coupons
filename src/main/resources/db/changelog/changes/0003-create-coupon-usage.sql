--liquibase formatted sql

--changeset dawid:0003-create-coupon-usage context:dev,prod
CREATE TABLE coupon_usage (
    id        UUID        NOT NULL,
    coupon_id UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    used_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_coupon_usage PRIMARY KEY (id),
    CONSTRAINT fk_coupon_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
    CONSTRAINT fk_coupon_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_coupon_usage_coupon_user UNIQUE (coupon_id, user_id)
);
--rollback DROP TABLE coupon_usage;
