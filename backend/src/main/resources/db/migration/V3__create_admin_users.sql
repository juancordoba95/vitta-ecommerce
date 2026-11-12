-- Admin panel users. No seed data: the first admin is created at startup from
-- ADMIN_EMAIL / ADMIN_PASSWORD (see AdminBootstrap).
CREATE TABLE admin_users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_admin_users PRIMARY KEY (id),
    CONSTRAINT uk_admin_users_email UNIQUE (email)
);
