-- =====================================================================
-- TriviaQuick V2: editor de cuestionarios, puntuación configurable y perfil.
-- Solo añade columnas y tablas; no elimina datos. Las trivias existentes quedan
-- PUBLICADAS y reciben los tramos de bonificación por defecto.
-- =====================================================================

-- ---------------------------------------------------------------- usuarios / perfil
ALTER TABLE users
    ADD COLUMN avatar_key               VARCHAR(80),
    ADD COLUMN pending_email            VARCHAR(160),
    ADD COLUMN email_token_hash         VARCHAR(64),
    ADD COLUMN email_token_expires_at   TIMESTAMPTZ,
    ADD COLUMN credentials_changed_at   TIMESTAMPTZ,
    ADD COLUMN deleted_at               TIMESTAMPTZ,
    ADD COLUMN version                  BIGINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX ux_users_email_token ON users (email_token_hash) WHERE email_token_hash IS NOT NULL;

CREATE TABLE user_preferences (
    user_id          BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    sound_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    master_volume    INT     NOT NULL DEFAULT 70 CHECK (master_volume BETWEEN 0 AND 100),
    effects_volume   INT     NOT NULL DEFAULT 80 CHECK (effects_volume BETWEEN 0 AND 100),
    countdown_sounds BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------- cuestionarios
ALTER TABLE trivias
    ADD COLUMN status                 VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    ADD COLUMN category               VARCHAR(60),
    ADD COLUMN play_mode              VARCHAR(20)  NOT NULL DEFAULT 'GROUP' CHECK (play_mode IN ('INDIVIDUAL', 'GROUP')),
    ADD COLUMN cover_image_key        VARCHAR(80),
    ADD COLUMN activity_minutes       INT CHECK (activity_minutes BETWEEN 1 AND 240),
    ADD COLUMN updated_at             TIMESTAMPTZ,
    ADD COLUMN version                BIGINT       NOT NULL DEFAULT 0,
    -- puntuación
    ADD COLUMN default_points         INT          NOT NULL DEFAULT 1000 CHECK (default_points BETWEEN 10 AND 5000),
    ADD COLUMN speed_bonus_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN streak_enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN streak_breaks_on_miss  BOOLEAN      NOT NULL DEFAULT TRUE,
    -- eventos
    ADD COLUMN event_mode             VARCHAR(20)  NOT NULL DEFAULT 'AUTO' CHECK (event_mode IN ('AUTO', 'MANUAL', 'OFF')),
    ADD COLUMN auto_frequency         INT          NOT NULL DEFAULT 2 CHECK (auto_frequency BETWEEN 1 AND 5),
    ADD COLUMN auto_event_types       VARCHAR(120) NOT NULL DEFAULT 'LIGHTNING_CHALLENGE,MULTIPLIER,SHIELD,DUEL',
    -- configuración por defecto de cada habilidad
    ADD COLUMN lightning_seconds      INT          NOT NULL DEFAULT 20  CHECK (lightning_seconds BETWEEN 5 AND 60),
    ADD COLUMN lightning_reward       INT          NOT NULL DEFAULT 300 CHECK (lightning_reward BETWEEN 10 AND 2000),
    ADD COLUMN lightning_category     VARCHAR(20)  NOT NULL DEFAULT 'ANY' CHECK (lightning_category IN ('ANY', 'LOGIC', 'MATH', 'MEMORY', 'KNOWLEDGE')),
    ADD COLUMN multiplier_factor      INT          NOT NULL DEFAULT 2   CHECK (multiplier_factor BETWEEN 2 AND 3),
    ADD COLUMN multiplier_target      VARCHAR(20)  NOT NULL DEFAULT 'BOTTOM_HALF' CHECK (multiplier_target IN ('BOTTOM_HALF', 'ALL')),
    ADD COLUMN shield_target          VARCHAR(20)  NOT NULL DEFAULT 'CORRECT_PREVIOUS' CHECK (shield_target IN ('CORRECT_PREVIOUS', 'ALL')),
    ADD COLUMN duel_seconds           INT          NOT NULL DEFAULT 20  CHECK (duel_seconds BETWEEN 5 AND 60),
    ADD COLUMN duel_stake             INT          NOT NULL DEFAULT 250 CHECK (duel_stake BETWEEN 10 AND 2000),
    ADD COLUMN power_up_seconds       INT          NOT NULL DEFAULT 15  CHECK (power_up_seconds BETWEEN 5 AND 60);
UPDATE trivias SET updated_at = created_at WHERE updated_at IS NULL;

-- Tramos de bonificación por velocidad: hasta X % del tiempo consumido → +N puntos.
CREATE TABLE speed_bonus_tiers (
    id                   BIGSERIAL PRIMARY KEY,
    trivia_id            BIGINT NOT NULL REFERENCES trivias (id) ON DELETE CASCADE,
    max_elapsed_percent  INT    NOT NULL CHECK (max_elapsed_percent BETWEEN 1 AND 100),
    bonus_points         INT    NOT NULL CHECK (bonus_points BETWEEN 0 AND 2000),
    CONSTRAINT ux_speed_tier UNIQUE (trivia_id, max_elapsed_percent)
);

-- Tramos de racha: al alcanzar N aciertos consecutivos → +M puntos.
CREATE TABLE streak_bonus_tiers (
    id             BIGSERIAL PRIMARY KEY,
    trivia_id      BIGINT NOT NULL REFERENCES trivias (id) ON DELETE CASCADE,
    streak_length  INT    NOT NULL CHECK (streak_length BETWEEN 2 AND 50),
    bonus_points   INT    NOT NULL CHECK (bonus_points BETWEEN 1 AND 2000),
    CONSTRAINT ux_streak_tier UNIQUE (trivia_id, streak_length)
);

INSERT INTO speed_bonus_tiers (trivia_id, max_elapsed_percent, bonus_points)
SELECT t.id, v.p, v.b FROM trivias t CROSS JOIN (VALUES (25, 100), (60, 50), (100, 10)) AS v(p, b);
INSERT INTO streak_bonus_tiers (trivia_id, streak_length, bonus_points)
SELECT t.id, v.n, v.b FROM trivias t CROSS JOIN (VALUES (3, 30), (5, 60), (10, 100)) AS v(n, b);

-- Línea de tiempo manual: un evento como máximo entre dos preguntas.
CREATE TABLE trivia_timeline_events (
    id                  BIGSERIAL PRIMARY KEY,
    trivia_id           BIGINT      NOT NULL REFERENCES trivias (id) ON DELETE CASCADE,
    after_question      INT         NOT NULL CHECK (after_question >= 0),
    type                VARCHAR(30) NOT NULL CHECK (type IN ('LIGHTNING_CHALLENGE', 'MULTIPLIER', 'DUEL', 'SHIELD')),
    duration_seconds    INT CHECK (duration_seconds BETWEEN 5 AND 60),
    reward_points       INT CHECK (reward_points BETWEEN 10 AND 2000),
    multiplier_factor   INT CHECK (multiplier_factor BETWEEN 2 AND 3),
    stake               INT CHECK (stake BETWEEN 10 AND 2000),
    challenge_category  VARCHAR(20) CHECK (challenge_category IN ('ANY', 'LOGIC', 'MATH', 'MEMORY', 'KNOWLEDGE')),
    target              VARCHAR(20) CHECK (target IN ('BOTTOM_HALF', 'ALL', 'CORRECT_PREVIOUS')),
    CONSTRAINT ux_timeline_slot UNIQUE (trivia_id, after_question)
);

-- ---------------------------------------------------------------- preguntas
ALTER TABLE questions
    ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE' CHECK (question_type IN ('MULTIPLE_CHOICE', 'TRUE_FALSE')),
    ADD COLUMN image_key     VARCHAR(80);
ALTER TABLE questions DROP CONSTRAINT IF EXISTS questions_base_points_check;
ALTER TABLE questions ADD CONSTRAINT questions_base_points_check CHECK (base_points BETWEEN 10 AND 5000);

-- ---------------------------------------------------------------- partida
ALTER TABLE session_participants
    ADD COLUMN current_streak      INT NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    ADD COLUMN best_streak         INT NOT NULL DEFAULT 0 CHECK (best_streak >= 0),
    ADD COLUMN last_correct_index  INT NOT NULL DEFAULT -1;

ALTER TABLE submitted_answers
    ADD COLUMN base_points        INT NOT NULL DEFAULT 0 CHECK (base_points >= 0),
    ADD COLUMN speed_bonus        INT NOT NULL DEFAULT 0 CHECK (speed_bonus >= 0),
    ADD COLUMN multiplier_factor  INT NOT NULL DEFAULT 1 CHECK (multiplier_factor BETWEEN 1 AND 3);

ALTER TABLE game_events
    ADD COLUMN reward_points      INT,
    ADD COLUMN multiplier_factor  INT,
    ADD COLUMN stake              INT,
    ADD COLUMN target             VARCHAR(20);

ALTER TABLE player_power_ups
    ADD COLUMN multiplier_factor  INT NOT NULL DEFAULT 2 CHECK (multiplier_factor BETWEEN 1 AND 3);

ALTER TABLE score_transactions DROP CONSTRAINT IF EXISTS score_transactions_reason_check;
ALTER TABLE score_transactions ADD CONSTRAINT score_transactions_reason_check
    CHECK (reason IN ('CORRECT_ANSWER', 'LIGHTNING_BONUS', 'DUEL_WIN', 'DUEL_LOSS', 'SHIELD_BLOCK', 'STREAK_BONUS'));
