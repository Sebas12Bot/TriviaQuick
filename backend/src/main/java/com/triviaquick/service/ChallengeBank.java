package com.triviaquick.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Banco de retos breves de lógica, memoria y conocimiento para el reto relámpago y el duelo.
 * La respuesta correcta nunca sale del servidor.
 */
@Component
public class ChallengeBank {

    public record Challenge(String key, String category, String prompt, List<String> options, int correctIndex) {

        public boolean isCorrect(int optionIndex) {
            return optionIndex == correctIndex;
        }
    }

    private static final List<Challenge> CHALLENGES = List.of(
            new Challenge("seq-1", "Lógica", "¿Qué número sigue? 2, 6, 12, 20, 30, …",
                    List.of("40", "42", "36", "44"), 1),
            new Challenge("seq-2", "Lógica", "¿Qué letra sigue? A, C, F, J, …",
                    List.of("M", "N", "O", "Ñ"), 2),
            new Challenge("odd-1", "Lógica", "¿Cuál no pertenece al grupo?",
                    List.of("Cubo", "Esfera", "Cuadrado", "Cilindro"), 2),
            new Challenge("math-1", "Cálculo mental", "¿Cuánto es 15 % de 240?",
                    List.of("32", "36", "38", "24"), 1),
            new Challenge("math-2", "Cálculo mental", "Si 3 lápices cuestan 4,50, ¿cuánto cuestan 7?",
                    List.of("10,50", "9,80", "11,00", "10,20"), 0),
            new Challenge("mem-1", "Memoria", "Memoriza: AZUL · 7 · RÍO · 3. ¿Qué palabra iba en tercer lugar?",
                    List.of("Azul", "Mar", "Río", "Siete"), 2),
            new Challenge("know-1", "Conocimiento", "¿Cuál es el planeta más grande del sistema solar?",
                    List.of("Saturno", "Júpiter", "Neptuno", "Tierra"), 1),
            new Challenge("know-2", "Conocimiento", "¿Cuántos lados tiene un hexágono?",
                    List.of("5", "6", "7", "8"), 1),
            new Challenge("logic-3", "Lógica", "Todos los gatos son felinos. Misu es un gato. Entonces Misu…",
                    List.of("puede ser un perro", "es un felino", "no es un felino", "no se sabe"), 1),
            new Challenge("know-3", "Conocimiento", "¿Qué gas absorben las plantas en la fotosíntesis?",
                    List.of("Oxígeno", "Nitrógeno", "Dióxido de carbono", "Hidrógeno"), 2));

    public Challenge pick(long seed) {
        return CHALLENGES.get((int) Math.floorMod(seed, CHALLENGES.size()));
    }

    /** Elige un reto de la categoría indicada (ANY = cualquiera). */
    public Challenge pick(com.triviaquick.model.ChallengeCategory category, long seed) {
        List<Challenge> pool = category == null || category == com.triviaquick.model.ChallengeCategory.ANY
                ? CHALLENGES
                : CHALLENGES.stream().filter(c -> CATEGORY.get(c.key().split("-")[0]) == category).toList();
        if (pool.isEmpty()) {
            pool = CHALLENGES;
        }
        return pool.get((int) Math.floorMod(seed, pool.size()));
    }

    private static final java.util.Map<String, com.triviaquick.model.ChallengeCategory> CATEGORY = java.util.Map.of(
            "seq", com.triviaquick.model.ChallengeCategory.LOGIC,
            "odd", com.triviaquick.model.ChallengeCategory.LOGIC,
            "logic", com.triviaquick.model.ChallengeCategory.LOGIC,
            "math", com.triviaquick.model.ChallengeCategory.MATH,
            "mem", com.triviaquick.model.ChallengeCategory.MEMORY,
            "know", com.triviaquick.model.ChallengeCategory.KNOWLEDGE);

    public Optional<Challenge> find(String key) {
        return CHALLENGES.stream().filter(c -> c.key().equals(key)).findFirst();
    }
}
