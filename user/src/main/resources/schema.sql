CREATE TABLE IF NOT EXISTS user_account (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(100)  NOT NULL,
    nickname    VARCHAR(64)   DEFAULT NULL,
    avatar_url  VARCHAR(512)  DEFAULT NULL,
    email       VARCHAR(128)  DEFAULT NULL,
    phone       VARCHAR(32)   DEFAULT NULL,
    authorities VARCHAR(255)  NOT NULL DEFAULT 'ROLE_USER',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'User account';

CREATE TABLE IF NOT EXISTS user_identity (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    provider      VARCHAR(32)   NOT NULL,
    openid        VARCHAR(128)  NOT NULL,
    unionid       VARCHAR(128)  DEFAULT NULL,
    access_token  VARCHAR(2048) DEFAULT NULL,
    refresh_token VARCHAR(2048) DEFAULT NULL,
    expires_at    DATETIME      DEFAULT NULL,
    scope         VARCHAR(255)  DEFAULT NULL,
    nickname      VARCHAR(64)   DEFAULT NULL,
    avatar_url    VARCHAR(512)  DEFAULT NULL,
    raw_profile   LONGTEXT      DEFAULT NULL,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_identity_provider_openid (provider, openid),
    KEY idx_user_identity_user_id (user_id),
    CONSTRAINT fk_user_identity_user
        FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Third-party identity';
