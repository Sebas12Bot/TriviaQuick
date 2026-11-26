-- =====================================================================
-- TriviaQuick V3: bonus para la respuesta correcta más rápida y video en preguntas.
-- Solo añade columnas y amplía una restricción; no elimina datos.
-- =====================================================================

ALTER TABLE trivias
    ADD COLUMN fastest_bonus_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fastest_bonus_points  INT     NOT NULL DEFAULT 100 CHECK (fastest_bonus_points BETWEEN 1 AND 2000);

-- Clave del video en el almacenamiento (Cloudinary o disco local).
ALTER TABLE questions
    ADD COLUMN video_key VARCHAR(80);

ALTER TABLE score_transactions DROP CONSTRAINT IF EXISTS score_transactions_reason_check;
ALTER TABLE score_transactions ADD CONSTRAINT score_transactions_reason_check
    CHECK (reason IN ('CORRECT_ANSWER', 'LIGHTNING_BONUS', 'DUEL_WIN', 'DUEL_LOSS', 'SHIELD_BLOCK', 'STREAK_BONUS', 'FASTEST_ANSWER'));

-- Búsqueda de la respuesta correcta más rápida de una pregunta al cerrarla.
CREATE INDEX ix_submitted_question_fastest ON submitted_answers (question_id, correct, response_time_ms);
