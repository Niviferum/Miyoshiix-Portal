-- Schéma initial : la liste blanche des personnes autorisées.
-- Les modules ajouteront leurs tables dans V2__, V3__, etc.
-- Ne jamais modifier une migration déjà appliquée : Flyway en vérifie l'empreinte.

CREATE TABLE app_user (
    -- Snowflake Discord, en texte : au-delà de 2^53 un parse JSON le dégraderait.
    discord_id    VARCHAR(32)  PRIMARY KEY,
    display_name  VARCHAR(100) NOT NULL,
    avatar_hash   VARCHAR(64),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,

    CONSTRAINT app_user_role_valid CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT app_user_discord_id_numeric CHECK (discord_id ~ '^[0-9]{17,20}$')
);

-- Couvre le filtre de connexion : discord_id + enabled.
CREATE INDEX idx_app_user_enabled ON app_user (enabled) WHERE enabled = TRUE;

COMMENT ON TABLE app_user IS
    'Liste blanche : seuls ces identifiants Discord peuvent ouvrir une session.';
