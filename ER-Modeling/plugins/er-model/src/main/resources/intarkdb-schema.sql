-- ER Model plugin minimal schema (INTARKDB variant).
-- Matches the openGauss schema so plugin features work identically across drivers.

CREATE TABLE IF NOT EXISTS er_model_saved_diagram (
    diagram_id       BIGSERIAL PRIMARY KEY,
    diagram_name     VARCHAR(128)    NOT NULL,
    dialect          VARCHAR(32)     NOT NULL,
    dbml_content     TEXT            NOT NULL,
    description      TEXT,
    created_by       VARCHAR(64),
    updated_by       VARCHAR(64),
    created_at       TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS er_model_execution_audit (
    execution_id     BIGSERIAL PRIMARY KEY,
    diagram_id       BIGINT,
    cluster_id       VARCHAR(128),
    node_id          VARCHAR(128),
    target_database  VARCHAR(128),
    target_schema    VARCHAR(128),
    executed_sql     TEXT            NOT NULL,
    status           VARCHAR(32)     DEFAULT 'SUCCESS',
    error_message    TEXT,
    executed_at      TIMESTAMPTZ     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_er_model_execution_diagram
        FOREIGN KEY (diagram_id)
        REFERENCES er_model_saved_diagram (diagram_id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_er_model_execution_diagram
    ON er_model_execution_audit (diagram_id);

