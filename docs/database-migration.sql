-- Legacy migration for the original OAuth database layout.
-- Back up both databases and stop the services before running this script.
-- Replace super_star_oauth if the legacy database used another name.

CREATE DATABASE IF NOT EXISTS super_star_user
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS super_star_user.user_account (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(100)  NOT NULL,
    nickname    VARCHAR(64)   DEFAULT NULL,
    avatar_url  VARCHAR(512)  DEFAULT NULL,
    email       VARCHAR(128)  DEFAULT NULL,
    phone       VARCHAR(32)   DEFAULT NULL,
    authorities VARCHAR(255)  NOT NULL DEFAULT 'ROLE_USER',
    status      TINYINT       NOT NULL DEFAULT 1,
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS super_star_user.user_identity (
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
        FOREIGN KEY (user_id) REFERENCES super_star_user.user_account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO super_star_user.user_account
    (id, username, password, nickname, avatar_url, email, phone, authorities, status, create_time, update_time)
SELECT id, username, password, nickname, avatar_url, email, phone, authorities, status, create_time, update_time
FROM super_star_oauth.sys_user;

INSERT IGNORE INTO super_star_user.user_identity
    (id, user_id, provider, openid, unionid, access_token, refresh_token, expires_at,
     scope, nickname, avatar_url, raw_profile, create_time, update_time)
SELECT id, user_id, provider, openid, unionid, access_token, refresh_token, expires_at,
       scope, nickname, avatar_url, raw_profile, create_time, update_time
FROM super_star_oauth.sys_user_third_auth;

SET @token_settings_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'super_star_oauth'
      AND table_name = 'oauth2_registered_client'
      AND column_name = 'token_settings'
);

SET @add_token_settings_sql = IF(
    @token_settings_exists = 0,
    'ALTER TABLE super_star_oauth.oauth2_registered_client ADD COLUMN token_settings varchar(2000) NOT NULL AFTER client_settings',
    'SELECT 1'
);

PREPARE add_token_settings_statement FROM @add_token_settings_sql;
EXECUTE add_token_settings_statement;
DEALLOCATE PREPARE add_token_settings_statement;

-- The application upgrades the legacy {noop}demo-secret to BCrypt on startup.
-- Verify the copied data before archiving the legacy sys_user tables.
