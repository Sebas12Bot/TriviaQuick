# TriviaQuick

Plataforma de trivias en vivo. El docente lanza una partida; los estudiantes se unen con un código, responden preguntas y minijuegos, y la partida termina con una ceremonia de resultados: podio animado y clasificación oficial calculada por el servidor.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, WebSocket/STOMP), Flyway, springdoc-openapi |
| Base de datos | PostgreSQL (embebido en desarrollo y pruebas, mediante zonky) |
| Frontend | React 19 + TypeScript, Vite, React Router, @stomp/stompjs, lucide-react |

## Ejecutar en local

Requisitos: Java 21 y Node 20 o superior. No hace falta instalar PostgreSQL.

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

- Frontend: http://localhost:5173 (Vite reenvía `/api` y `/ws` al backend).
- Swagger UI: http://localhost:8080/swagger-ui.html
- Datos de demostración: define `DEMO_PASSWORD` antes de arrancar el backend para crear un docente (`docente@demo.triviaquick.local`), cuatro estudiantes (`lucia`, `mateo`, `valentina` y `samuel` `@demo.triviaquick.local`) y una trivia de 9 preguntas. No se crea ninguna partida ni puntuación ficticia.

Variables de entorno: [backend/.env.example](backend/.env.example) y [frontend/.env.example](frontend/.env.example).

## Arquitectura (MVC + servicios)

```
backend/src/main/java/com/triviaquick/
├── model/        Entidades JPA y reglas de dominio (Modelo)
├── repository/   Spring Data JPA (consultas parametrizadas y bloqueos)
├── service/      Reglas de negocio: GameSessionService, ScoreService, RankingService,
│                 GameResultService, GameEventService, SessionAccessService…
├── controller/   REST: reciben DTO validados y delegan (Controlador)
├── dto/ mapper/  Contratos de la API y mapeo explícito (nunca se exponen entidades)
├── security/     JWT, filtros, CSRF, CORS, autorización STOMP, límite de peticiones
├── config/       DataSource, WebSocket, OpenAPI, reloj, datos demo
└── exception/    Errores uniformes sin trazas internas
frontend/src/     Vista: pages/, components/{results,game,common}/, hooks/, services/, types/
```

### Entidades

`User`, `Trivia`, `Question`, `Answer` (opción), `GameSession`, `SessionParticipant`, `SubmittedAnswer` (respuesta de un participante, única por pregunta), `GameEvent`, `GameEventResponse`, `PlayerPowerUp`, `ScoreTransaction` y `GameResult`.

## Reglas de juego

- **Puntos por respuesta**: entre el 50 % (al agotarse el tiempo) y el 100 % (respuesta inmediata) de los puntos base. Los calcula solo el servidor.
- **Reto relámpago**: +300 si se acierta el reto dentro del plazo (una respuesta por jugador).
- **Multiplicador**: se otorga a la mitad inferior de la clasificación. Una vez activado, duplica la siguiente respuesta y se consume con ella, aunque falle.
- **Escudo**: se otorga a quien acertó la pregunta anterior. Una vez activado, bloquea la siguiente transferencia en su contra y se consume.
- **Duelo**: enfrenta a los dos jugadores consecutivos con menos diferencia de puntos. Gana quien acierte; si aciertan ambos, el más rápido. El ganador se lleva hasta 250 puntos del perdedor, nunca más de los que este tiene. *Interpretación del enunciado*: «transferir al perdedor» se implementó como que el perdedor transfiere al ganador, coherente con el escudo y con la regla de no perder más puntos de los que se tienen.
- **Clasificación**: puntuación descendente; a igualdad de puntos gana quien tenga más aciertos; si persiste el empate, ambos comparten posición (1, 2, 2, 4). Al cerrar la partida se guarda en `GameResult` y ya no cambia.

## Concurrencia e integridad

- **Bloqueo pesimista** de las filas de participante (`SELECT … FOR UPDATE`) en cada cambio de puntos. Las transferencias bloquean siempre por id ascendente para evitar interbloqueos. Se eligió este enfoque porque las puntuaciones son filas muy disputadas con transacciones cortas, y el bloqueo optimista obligaría a reintentos en ráfaga.
- **Sesión**: las respuestas toman un bloqueo compartido (`FOR SHARE`) y avanzar o cerrar la partida toma uno exclusivo. Así, ninguna respuesta entra después del cierre ni a mitad de una transición.
- **Idempotencia**: restricciones únicas (respuesta por participante y pregunta, respuesta por evento, potenciador por evento, `idempotency_key` en `score_transactions`) como última garantía. `@Version` en sesión, participante, evento y potenciador.
- **Invariante**: puntuación = suma de `ScoreTransaction.delta` ≥ 0 (con `CHECK` en la base de datos).

## Seguridad

- JWT en una cookie `httpOnly` y `SameSite` (nunca en `localStorage`). Para clientes de API también se acepta `Authorization: Bearer`.
- Defensa CSRF: las escrituras autenticadas con cookie exigen la cabecera `X-Requested-With: TriviaQuick`, y CORS solo admite los orígenes configurados.
- Roles comprobados en la cadena de filtros y con `@PreAuthorize`. Solo el anfitrión controla su sesión y solo sus miembros pueden verla. La identidad sale siempre del token.
- WebSocket: el handshake valida el origen y la cookie; las suscripciones solo se permiten a sesiones propias; cualquier `SEND` del cliente se rechaza.
- Límite de respuestas por usuario (HTTP 429). Errores sin trazas. Secretos en variables de entorno. Registro de docentes protegido por un código.

## Pruebas

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm test && npm run typecheck
```

- **Backend**: 47 pruebas (unitarias de clasificación, puntuación, duelo y autorización STOMP; de integración con PostgreSQL real para seguridad, flujo completo con los 4 minijuegos y concurrencia).
- **Frontend**: 22 pruebas (podio con 1, 2 y 3+ participantes, empates, clasificación, marcador animado, carga, vacío, error, partida pendiente, secuencia de animación y movimiento reducido).

## Versión 2: identidad, mascota, audio, editor y perfil

- **Identidad azul + coral**: azul principal `#287BC1`, azul noche `#071A3B`, coral `#FF806F` para acentos (para texto, `--coral-ink`). Oro, plata y bronce solo en el podio. Sin morados.
- **Coby, la cobra mascota**: modelo 3D por código en three.js (`components/mascot/`). Se carga en un fragmento aparte después del formulario y se pausa fuera de pantalla; respeta `prefers-reduced-motion` y muestra una silueta si no hay WebGL. Está animada en el login y el registro: saluda, mira el formulario, se tapa los ojos con la capucha durante la contraseña, espía si se muestra, muestra confusión ante un error y celebra el registro. En el resto de la app aparece como imagen estática (`CobraStill`).
- **Audio** (`services/audio/audioEngine.ts` + `context/AudioContext.tsx`): efectos sintetizados con Web Audio para la cuenta regresiva, cada habilidad, la recompensa y el podio. **El sonido de respuesta es uno solo**, se reproduce únicamente en el dispositivo de quien responde y nunca pasa por el WebSocket. Las preferencias se guardan por usuario y existe un silencio rápido durante la partida.
- **Editor de cuestionarios** (`/teacher/quizzes/:id`): borradores y publicación; preguntas de selección múltiple y de verdadero/falso con imagen; línea de tiempo con reordenación (arrastrar o botones); minijuegos en modo manual, automático o desactivado, con la configuración de cada habilidad.
- **Puntuación configurable**: puntos base por pregunta, más un bonus por velocidad según tramos (el tiempo lo mide el servidor), multiplicado por el factor del multiplicador; además, bonus por racha según tramos (fallar o no responder la rompe, si se configura). Cada bonus genera su `ScoreTransaction`.
- **Perfil** (`/profile`): nombre; cambio de correo con verificación por enlace (SMTP configurable); foto validada por su contenido y re-codificada; cambio de contraseña, que cierra las demás sesiones; preferencias de sonido; eliminación de cuenta por **anonimización**, que conserva el historial ajeno.
- **Migración `V2__editor_puntuacion_perfil.sql`**: solo añade columnas y tablas. Las trivias existentes quedan publicadas y reciben los tramos por defecto.

## Pendiente

- Modalidad **individual**: se guarda en el cuestionario, pero las partidas siguen siendo en vivo, guiadas por el docente. El **tiempo general de la actividad** es orientativo y no se aplica.
- Preguntas con audio o vídeo: la infraestructura actual solo admite imágenes.
- El límite de peticiones y el broker STOMP son en memoria: con varias réplicas habría que usar un almacén compartido (Redis) y un broker externo.
