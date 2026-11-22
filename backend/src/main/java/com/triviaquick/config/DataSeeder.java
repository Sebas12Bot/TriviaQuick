package com.triviaquick.config;

import com.triviaquick.model.Answer;
import com.triviaquick.model.Question;
import com.triviaquick.model.Role;
import com.triviaquick.model.SpeedBonusTier;
import com.triviaquick.model.StreakBonusTier;
import com.triviaquick.model.Trivia;
import com.triviaquick.model.User;
import com.triviaquick.repository.TriviaRepository;
import com.triviaquick.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Datos de demostración (desactivables con SEED_DEMO_DATA=false). Crea un docente, cuatro estudiantes
 * y una trivia, todos marcados como ejemplo. No crea partidas ni puntuaciones: los resultados
 * solo existen si se juega una partida real.
 */
@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_DOMAIN = "@demo.triviaquick.local";

    private final AppProperties props;
    private final UserRepository users;
    private final TriviaRepository trivias;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DataSeeder(AppProperties props, UserRepository users, TriviaRepository trivias, PasswordEncoder encoder,
                      Clock clock) {
        this.props = props;
        this.users = users;
        this.trivias = trivias;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seed().demoData() || users.existsByEmailIgnoreCase("docente" + DEMO_DOMAIN)) {
            return;
        }
        String password = props.seed().demoPassword();
        if (password == null || password.length() < 8) {
            log.warn("Datos de demostración omitidos: define DEMO_PASSWORD (mínimo 8 caracteres) para crearlos.");
            return;
        }
        String hash = encoder.encode(password);
        User teacher = users.save(new User("docente" + DEMO_DOMAIN, "Docente Demo", hash, Role.TEACHER, clock.instant()));
        for (String[] s : List.of(new String[]{"lucia", "Lucía Ramos"}, new String[]{"mateo", "Mateo Díaz"},
                new String[]{"valentina", "Valentina Ortiz"}, new String[]{"samuel", "Samuel Peña"})) {
            users.save(new User(s[0] + DEMO_DOMAIN, s[1] + " (demo)", hash, Role.STUDENT, clock.instant()));
        }

        Trivia trivia = new Trivia("Ciencia y mundo (demo)",
                "Trivia de ejemplo para probar una partida completa con minijuegos.", teacher, clock.instant());
        addQuestion(trivia, "¿Cuál es el órgano más grande del cuerpo humano?", 20, "Hígado", "Piel", "Pulmones", "Intestino", 1);
        addQuestion(trivia, "¿Qué planeta es conocido como el planeta rojo?", 15, "Venus", "Júpiter", "Marte", "Mercurio", 2);
        addQuestion(trivia, "¿Cuál es el símbolo químico del oro?", 15, "Au", "Ag", "Go", "Or", 0);
        addQuestion(trivia, "¿En qué continente está el desierto del Sahara?", 15, "Asia", "África", "Oceanía", "América", 1);
        addQuestion(trivia, "¿Cuántos huesos tiene el cuerpo humano adulto?", 20, "186", "206", "226", "196", 1);
        addQuestion(trivia, "¿Qué científico propuso la teoría de la relatividad?", 15, "Newton", "Curie", "Einstein", "Tesla", 2);
        addQuestion(trivia, "¿Cuál es el océano más grande del planeta?", 15, "Atlántico", "Índico", "Ártico", "Pacífico", 3);
        addQuestion(trivia, "¿A qué temperatura hierve el agua a nivel del mar?", 15, "90 °C", "100 °C", "110 °C", "120 °C", 1);
        addQuestion(trivia, "¿Qué parte de la célula contiene el ADN?", 20, "Núcleo", "Ribosoma", "Membrana", "Citoplasma", 0);
        // Tramos de bonificación por defecto (los mismos que aplica la migración V2 a las trivias existentes).
        trivia.getSpeedTiers().add(new SpeedBonusTier(trivia, 25, 100));
        trivia.getSpeedTiers().add(new SpeedBonusTier(trivia, 60, 50));
        trivia.getSpeedTiers().add(new SpeedBonusTier(trivia, 100, 10));
        trivia.getStreakTiers().add(new StreakBonusTier(trivia, 3, 30));
        trivia.getStreakTiers().add(new StreakBonusTier(trivia, 5, 60));
        trivia.getStreakTiers().add(new StreakBonusTier(trivia, 10, 100));
        trivias.save(trivia);
        log.info("Datos de demostración creados (cuentas *{}).", DEMO_DOMAIN);
    }

    private static void addQuestion(Trivia trivia, String text, int seconds, String a, String b, String c, String d, int correct) {
        Question q = new Question(text, seconds, 1000);
        String[] options = {a, b, c, d};
        for (int i = 0; i < options.length; i++) {
            q.addAnswer(new Answer(options[i], i == correct));
        }
        trivia.addQuestion(q);
    }
}
