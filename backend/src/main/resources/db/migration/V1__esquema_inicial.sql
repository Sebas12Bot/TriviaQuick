-- =====================================================================
-- TriviaQuick - esquema inicial
-- Las puntuaciones solo se modifican desde el backend y cada cambio
-- queda registrado en score_transactions.
-- =====================================================================

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(160) NOT NULL,
    display_name   VARCHAR(80)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('TEACHER', 'STUDENT')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE trivias (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(140) NOT NULL,
    description  VARCHAR(500),
    owner_id     BIGINT       NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_trivias_owner ON trivias (owner_id);

CREATE TABLE questions (
    id                  BIGSERIAL PRIMARY KEY,
    trivia_id           BIGINT       NOT NULL REFERENCES trivias (id) ON DELETE CASCADE,
    position            INT          NOT NULL CHECK (position >= 0),
    text                VARCHAR(500) NOT NULL,
    time_limit_seconds  INT          NOT NULL CHECK (time_limit_seconds BETWEEN 5 AND 120),
    base_points         INT          NOT NULL CHECK (base_points BETWEEN 100 AND 5000),
    CONSTRAINT ux_questions_position UNIQUE (trivia_id, position)
);

CREATE TABLE answers (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT       NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position     INT          NOT NULL CHECK (position >= 0),
    text         VARCHAR(200) NOT NULL,
    correct      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ux_answers_position UNIQUE (question_id, position)
);

CREATE TABLE game_sessions (
    id                      BIGSERIAL PRIMARY KEY,
    trivia_id               BIGINT      NOT NULL REFERENCES trivias (id),
    host_id                 BIGINT      NOT NULL REFERENCES users (id),
    join_code               VARCHAR(8)  NOT NULL UNIQUE,
    status                  VARCHAR(20) NOT NULL CHECK (status IN ('LOBBY', 'QUESTION', 'EVENT', 'FINISHED')),
    current_question_index  INT         NOT NULL DEFAULT -1,
    question_started_at     TIMESTAMPTZ,
    question_deadline       TIMESTAMPTZ,
    events_enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    event_frequency         INT         NOT NULL DEFAULT 2 CHECK (event_frequency BETWEEN 1 AND 5),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at              TIMESTAMPTZ,
    finished_at             TIMESTAMPTZ,
    version                 BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_sessions_host ON game_sessions (host_id);

CREATE TABLE session_participants (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT      NOT NULL REFERENCES game_sessions (id) ON DELETE CASCADE,
    user_id          BIGINT      NOT NULL REFERENCES users (id),
    display_name     VARCHAR(80) NOT NULL,
    score            INT         NOT NULL DEFAULT 0 CHECK (score >= 0),
    correct_answers  INT         NOT NULL DEFAULT 0 CHECK (correct_answers >= 0),
    answered_count   INT         NOT NULL DEFAULT 0 CHECK (answered_count >= 0),
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ux_participant_session_user UNIQUE (session_id, user_id)
);
CREATE INDEX ix_participants_user ON session_participants (user_id);

CREATE TABLE submitted_answers (
    id                  BIGSERIAL PRIMARY KEY,
    participant_id      BIGINT      NOT NULL REFERENCES session_participants (id) ON DELETE CASCADE,
    question_id         BIGINT      NOT NULL REFERENCES questions (id),
    answer_id           BIGINT      NOT NULL REFERENCES answers (id),
    correct             BOOLEAN     NOT NULL,
    points_awarded      INT         NOT NULL CHECK (points_awarded >= 0),
    multiplier_applied  BOOLEAN     NOT NULL DEFAULT FALSE,
    response_time_ms    INT         NOT NULL CHECK (response_time_ms >= 0),
    submitted_at        TIMESTAMPTZ NOT NULL,
    -- Una sola respuesta por participante y pregunta: garantía final contra duplicados.
    CONSTRAINT ux_submitted_answer UNIQUE (participant_id, question_id)
);

CREATE TABLE game_events (
    id                    BIGSERIAL PRIMARY KEY,
    session_id            BIGINT      NOT NULL REFERENCES game_sessions (id) ON DELETE CASCADE,
    type                  VARCHAR(30) NOT NULL CHECK (type IN ('LIGHTNING_CHALLENGE', 'MULTIPLIER', 'DUEL', 'SHIELD')),
    status                VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RESOLVED')),
    after_question_index  INT         NOT NULL,
    challenge_key         VARCHAR(40),
    duel_first_id         BIGINT REFERENCES session_participants (id),
    duel_second_id        BIGINT REFERENCES session_participants (id),
    started_at            TIMESTAMPTZ NOT NULL,
    expires_at            TIMESTAMPTZ NOT NULL,
    resolved_at           TIMESTAMPTZ,
    outcome               VARCHAR(300),
    version               BIGINT      NOT NULL DEFAULT 0,
    -- Un evento como máximo entre dos preguntas: impide crear el mismo evento dos veces.
    CONSTRAINT ux_event_slot UNIQUE (session_id, after_question_index)
);

CREATE TABLE game_event_responses (
    id                BIGSERIAL PRIMARY KEY,
    event_id          BIGINT      NOT NULL REFERENCES game_events (id) ON DELETE CASCADE,
    participant_id    BIGINT      NOT NULL REFERENCES session_participants (id) ON DELETE CASCADE,
    option_index      INT         NOT NULL CHECK (option_index >= 0),
    correct           BOOLEAN     NOT NULL,
    response_time_ms  INT         NOT NULL CHECK (response_time_ms >= 0),
    responded_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_event_response UNIQUE (event_id, participant_id)
);

CREATE TABLE player_power_ups (
    id               BIGSERIAL PRIMARY KEY,
    participant_id   BIGINT      NOT NULL REFERENCES session_participants (id) ON DELETE CASCADE,
    type             VARCHAR(20) NOT NULL CHECK (type IN ('MULTIPLIER', 'SHIELD')),
    status           VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'ACTIVE', 'CONSUMED')),
    source_event_id  BIGINT      NOT NULL REFERENCES game_events (id) ON DELETE CASCADE,
    granted_at       TIMESTAMPTZ NOT NULL,
    activated_at     TIMESTAMPTZ,
    consumed_at      TIMESTAMPTZ,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ux_power_up_grant UNIQUE (source_event_id, participant_id)
);
CREATE INDEX ix_power_ups_participant ON player_power_ups (participant_id, type, status);

CREATE TABLE score_transactions (
    id                          BIGSERIAL PRIMARY KEY,
    session_id                  BIGINT       NOT NULL REFERENCES game_sessions (id) ON DELETE CASCADE,
    participant_id              BIGINT       NOT NULL REFERENCES session_participants (id) ON DELETE CASCADE,
    delta                       INT          NOT NULL,
    balance_after               INT          NOT NULL CHECK (balance_after >= 0),
    reason                      VARCHAR(30)  NOT NULL CHECK (reason IN ('CORRECT_ANSWER', 'LIGHTNING_BONUS', 'DUEL_WIN', 'DUEL_LOSS', 'SHIELD_BLOCK')),
    question_id                 BIGINT REFERENCES questions (id),
    event_id                    BIGINT REFERENCES game_events (id) ON DELETE CASCADE,
    counterpart_participant_id  BIGINT REFERENCES session_participants (id),
    description                 VARCHAR(200) NOT NULL,
    -- Clave de idempotencia: un mismo hecho de juego no puede generar dos movimientos.
    idempotency_key             VARCHAR(120) NOT NULL UNIQUE,
    created_at                  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX ix_score_tx_participant ON score_transactions (participant_id, created_at);

CREATE TABLE game_results (
    id               BIGSERIAL PRIMARY KEY,
    session_id       BIGINT      NOT NULL REFERENCES game_sessions (id) ON DELETE CASCADE,
    participant_id   BIGINT      NOT NULL REFERENCES session_participants (id) ON DELETE CASCADE,
    position         INT         NOT NULL CHECK (position >= 1),
    final_score      INT         NOT NULL CHECK (final_score >= 0),
    correct_answers  INT         NOT NULL CHECK (correct_answers >= 0),
    answered_count   INT         NOT NULL CHECK (answered_count >= 0),
    created_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_result_participant UNIQUE (session_id, participant_id)
);
CREATE INDEX ix_results_session_position ON game_results (session_id, position);
