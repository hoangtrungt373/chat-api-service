CREATE SEQUENCE IF NOT EXISTS product.USER_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MAXVALUE
    NO CYCLE;

CREATE TABLE IF NOT EXISTS product.USER (
    USER_ID                 INTEGER                         NOT NULL     PRIMARY KEY,
    USER_UUID               VARCHAR(36)                     NOT NULL,
    EMAIL                   VARCHAR(255)                    NOT NULL,
    USERNAME                VARCHAR(255)                    NOT NULL,
    PASSWORD                VARCHAR(255)                    NOT NULL,
    FIRST_NAME              VARCHAR(255),
    LAST_NAME               VARCHAR(255),
    PROFILE_PICTURE         VARCHAR(500),
    PROVIDER                VARCHAR(50)                     NOT NULL,
    PROVIDER_ID             VARCHAR(255),
    EMAIL_VERIFIED          BOOLEAN                         NOT NULL,
    STATUS                  VARCHAR(50)                     NOT NULL,
    ENABLED                 BOOLEAN                         NOT NULL,
    USR_CREATION            VARCHAR(128)                    NOT NULL,
    DTE_CREATION            TIMESTAMP WITH TIME ZONE        NOT NULL,
    USR_LAST_MODIFICATION   VARCHAR(128)                    NOT NULL,
    DTE_LAST_MODIFICATION   TIMESTAMP WITH TIME ZONE        NOT NULL,
    VERSION                 INTEGER                         NOT NULL,

    CONSTRAINT CKC_USER_PROVIDER CHECK (PROVIDER IN ('LOCAL', 'GOOGLE', 'FACEBOOK')),

    CONSTRAINT CKC_USER_STATUS CHECK (STATUS IN ('ONLINE', 'OFFLINE', 'AWAY', 'BUSY'))
);

ALTER SEQUENCE product.USER_SEQ OWNED BY product.USER.USER_ID;

CREATE INDEX IF NOT EXISTS IDX_USER_USER_UUID ON product.USER(USER_UUID);
CREATE INDEX IF NOT EXISTS IDX_USER_EMAIL ON product.USER(EMAIL);
CREATE INDEX IF NOT EXISTS IDX_USER_USERNAME ON product.USER(USERNAME);
CREATE INDEX IF NOT EXISTS IDX_USER_STATUS ON product.USER(STATUS) WHERE ENABLED = TRUE;

