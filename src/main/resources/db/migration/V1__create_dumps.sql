CREATE TABLE dumps (
    id         VARCHAR(32) PRIMARY KEY DEFAULT replace(gen_random_uuid()::text, '-', ''),
    problems   TEXT[]      NOT NULL,
    raw        TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    legacy     BOOLEAN     NOT NULL DEFAULT false
);
