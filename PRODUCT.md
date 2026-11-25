# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Elegido por el usuario: Java 21 + Spring Boot (MVC + capa de servicios) + PostgreSQL en el backend; React + TypeScript (Vite) en el frontend; tiempo real con Spring WebSocket/STOMP. En desarrollo y pruebas se usa PostgreSQL embebido; en producción, un PostgreSQL externo configurado por variables de entorno.

## Users

- **Docentes**: crean trivias, lanzan partidas en vivo en el aula (normalmente proyectadas) y controlan su ritmo.
- **Estudiantes / participantes**: entran con un código desde el móvil o el portátil, responden preguntas y minijuegos y consultan su posición.
- Nivel educativo abierto (de secundaria a universidad); no se ha fijado un nivel concreto.

## Product Purpose

TriviaQuick es una plataforma de trivias y evaluaciones interactivas. Una partida en vivo termina con una clasificación oficial que calcula el servidor y una ceremonia de resultados con podio. El objetivo es que el cierre de la partida sea memorable sin que nadie pueda manipular las puntuaciones.

## Positioning

Las puntuaciones y la clasificación son siempre autoritativas del servidor. Los minijuegos (reto relámpago, multiplicador, duelo y escudo) modifican los puntos solo mediante transacciones auditadas, de modo que la ceremonia final muestra un resultado verificable.

## Operating Context

- Aula con proyector (docente) y dispositivos personales (estudiantes).
- La partida la avanza el docente pregunta a pregunta; cada participante ve su progreso en tiempo real.

## Capabilities and Constraints

- Roles: docente y estudiante. El registro público crea estudiantes; una cuenta de docente requiere un código de alta configurado por variable de entorno.
- La interfaz está en español.
- Restricción de marca: no se usan morados, violetas ni púrpuras.
- Pendiente de decidir: el editor visual de trivias (por ahora se crean mediante la API o con los datos de ejemplo).

## Brand Commitments

- El nombre es **TriviaQuick**.
- La paleta es binding: azul eléctrico, azul profundo, azul celeste, azul oscuro de contraste, blanco y grises neutros; oro, plata y bronce exclusivamente para los tres primeros puestos. Nada de morado, violeta ni púrpura.
- El tono es moderno, tecnológico, dinámico y profesional; la celebración es elegante, no infantil.

## Evidence on Hand

No hay testimonios, clientes ni métricas reales. Las cuentas y la trivia de demostración están marcadas como datos de ejemplo y no deben presentarse como reales.

## Product Principles

1. El servidor decide: el cliente representa los resultados, nunca los calcula ni los envía.
2. Cada punto deja rastro: toda modificación queda en el historial de transacciones.
3. Ceremonia con contención: emoción en el cierre, claridad durante el juego.
4. Justicia visible: la regla de desempate es explícita y quien empata comparte posición.

## Accessibility & Inclusion

WCAG 2.1 AA como objetivo; se respeta `prefers-reduced-motion`; la clasificación se puede usar con lector de pantalla y teclado.
