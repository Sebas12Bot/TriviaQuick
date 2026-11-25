---
name: TriviaQuick
description: Trivias en vivo con una ceremonia final de retransmisión deportiva nocturna y una mascota cobra, Coby, que acompaña el acceso.
colors:
  blue-electric: "#287bc1"
  blue-electric-hi: "#3b8fd6"
  blue-electric-deep: "#1f68a6"
  blue-secondary: "#164e83"
  blue-sky: "#8bc9ef"
  blue-sky-light: "#c4e4f8"
  blue-sky-mid: "#5eaae0"
  blue-sky-soft: "#e6f3fc"
  blue-deep: "#0e2c57"
  blue-night-hi: "#0b2550"
  blue-night: "#071a3b"
  blue-ink: "#06142d"
  coral: "#ff806f"
  coral-hi: "#ff9586"
  coral-ink: "#b8402f"
  coral-soft-hi: "#ffe4dc"
  coral-soft: "#fff1ec"
  coral-line: "#ffd2c8"
  stage-shadow: "#000310"
  stage-floor: "#020c22"
  stage-field: "#03132e"
  stage-panel: "#041531"
  stage-raised: "#04193a"
  stage-tile: "#0c1f3b"
  stage-tile-hi: "#09224a"
  stage-hover: "#142b4e"
  stage-step: "#18325b"
  stage-step-hi: "#193c71"
  stage-soft-hi: "#c1d4e4"
  stage-soft: "#aec0d0"
  stage-faint: "#8294a7"
  shadow-tint: "#07162c"
  white: "#fcfeff"
  black: "#000000"
  gray-50: "#f4f7fa"
  gray-100: "#e9edf2"
  gray-200: "#d8dfe6"
  gray-400: "#96a0a9"
  gray-500: "#727b86"
  gray-600: "#515c69"
  gray-800: "#212c3a"
  gold: "#f1bf4e"
  gold-deep: "#c38323"
  silver: "#cad2db"
  silver-deep: "#89939e"
  bronze: "#d38b59"
  bronze-deep: "#9d5b38"
  success: "#15b477"
  success-ink: "#1d6b48"
  success-soft: "#e7f6ef"
  success-line: "#3ba776"
  success-on-dark: "#bfe9d3"
  danger: "#db4241"
  danger-ink: "#a32a1c"
  danger-soft: "#fdecea"
  danger-line: "#f2b8b0"
  danger-on-dark: "#f6c7c0"
  warning: "#edb345"
  warning-ink: "#6b4a00"
  warning-soft: "#fff8ec"
  warning-line: "#f5dfb2"
typography:
  display:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "clamp(2.6rem, 2rem + 3vw, 4.6rem)"
    fontWeight: 800
    lineHeight: 1
    fontVariation: "'wdth' 125"
  headline:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "clamp(1.7rem, 1.2rem + 2vw, 2.4rem)"
    fontWeight: 780
    lineHeight: 1.1
    letterSpacing: "-0.015em"
    fontVariation: "'wdth' 112"
  title:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "1.18rem"
    fontWeight: 720
    lineHeight: 1.1
    letterSpacing: "-0.015em"
    fontVariation: "'wdth' 115"
  score:
    fontFamily: "'Archivo Variable', 'Archivo', system-ui, sans-serif"
    fontSize: "1.9rem"
    fontWeight: 760
    lineHeight: 1
    fontFeature: "'tnum' 1"
    fontVariation: "'wdth' 120"
  body:
    fontFamily: "'Onest Variable', 'Onest', system-ui, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  row-name:
    fontFamily: "'Onest Variable', 'Onest', system-ui, sans-serif"
    fontSize: "1.05rem"
    fontWeight: 600
    lineHeight: 1.25
  label:
    fontFamily: "'Onest Variable', 'Onest', system-ui, sans-serif"
    fontSize: "0.76rem"
    fontWeight: 700
    lineHeight: 1.2
rounded:
  sm: "8px"
  icon: "10px"
  md: "12px"
  lg: "18px"
  step: "10px 10px 0 0"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  2xl: "24px"
  gutter: "clamp(16px, 4vw, 40px)"
components:
  button-primary:
    backgroundColor: "{colors.blue-electric}"
    textColor: "{colors.white}"
    rounded: "{rounded.pill}"
    padding: "10px 18px"
    height: "44px"
  button-coral:
    backgroundColor: "{colors.coral-soft}"
    textColor: "{colors.coral-ink}"
    rounded: "{rounded.pill}"
    padding: "10px 18px"
    height: "44px"
  button-ghost-light:
    backgroundColor: "{colors.white}"
    textColor: "{colors.blue-deep}"
    rounded: "{rounded.pill}"
    padding: "10px 18px"
    height: "44px"
  button-ghost-stage:
    backgroundColor: "{colors.blue-night}"
    textColor: "{colors.blue-sky}"
    rounded: "{rounded.pill}"
    padding: "8px 14px"
    height: "40px"
  icon-button:
    backgroundColor: "{colors.white}"
    textColor: "{colors.gray-600}"
    rounded: "{rounded.icon}"
    size: "36px"
  input:
    backgroundColor: "{colors.white}"
    textColor: "{colors.blue-ink}"
    rounded: "{rounded.md}"
    padding: "10px 14px"
    height: "46px"
  chip:
    backgroundColor: "{colors.white}"
    textColor: "{colors.gray-600}"
    rounded: "{rounded.pill}"
    padding: "6px 14px"
    height: "36px"
  chip-selected:
    backgroundColor: "{colors.blue-electric}"
    textColor: "{colors.white}"
  status-draft:
    backgroundColor: "{colors.coral-soft}"
    textColor: "{colors.coral-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "2px 10px"
  status-published:
    backgroundColor: "{colors.blue-sky-soft}"
    textColor: "{colors.blue-secondary}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "2px 10px"
  stat-tile:
    backgroundColor: "{colors.white}"
    textColor: "{colors.blue-secondary}"
    typography: "{typography.score}"
    rounded: "{rounded.md}"
    padding: "14px 16px"
  user-avatar:
    backgroundColor: "{colors.blue-electric}"
    textColor: "{colors.white}"
    rounded: "{rounded.pill}"
  banner:
    backgroundColor: "{colors.blue-sky-soft}"
    textColor: "{colors.blue-secondary}"
    rounded: "{rounded.md}"
    padding: "10px 14px"
  panel-light:
    backgroundColor: "{colors.white}"
    textColor: "{colors.blue-ink}"
    rounded: "{rounded.lg}"
    padding: "22px"
  panel-stage:
    backgroundColor: "{colors.stage-panel}"
    textColor: "{colors.white}"
    rounded: "{rounded.lg}"
  answer-option:
    backgroundColor: "{colors.stage-tile}"
    textColor: "{colors.white}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
    height: "64px"
  badge-self:
    backgroundColor: "{colors.blue-sky}"
    textColor: "{colors.blue-ink}"
    rounded: "{rounded.pill}"
    padding: "2px 8px"
---

# Design System: TriviaQuick

## Overview

**Creative North Star: "La retransmisión nocturna"**

TriviaQuick vive en dos superficies del mismo mundo. El juego en vivo y la ceremonia final ocurren en un escenario nocturno: un campo azul tinta con una luz cenital azul eléctrico, paneles azul noche separados por filetes celestes finos y cifras en Archivo expandido, como el grafismo de un marcador deportivo. Las pantallas de operación (acceso, panel del docente, editor de cuestionarios y perfil) usan una superficie clara de grises fríos, el azul principal como acción y el coral como acento complementario: indicadores, estados de borrador, menús y la pestaña activa.

El escenario se asoma en la operación solo en el lateral azul noche del acceso, donde vive Coby, una cobra 3D celeste con vientre melocotón y capucha coral, sobre un halo coral discreto. Coby se anima solo en acceso y registro; en el resto aparece como imagen fija (estados vacíos, verificación de correo). La emoción se concentra en el cierre y se dosifica: el oro, la plata y el bronce existen solo para los puestos 1 a 3, la luz se usa como foco y la animación cuenta una secuencia, no decora.

La celebración es elegante, no de feria: nada de morado, violeta, magenta ni púrpura, nada de confeti de colores de feria (el confeti del ganador solo mezcla metales, azules y blanco), nada de tarjetas iguales en rejilla.

**Key Characteristics:**
- Escenario azul tinta con luz radial azul eléctrico desde arriba; superficie clara de grises fríos para operar.
- Azul como voz principal, coral como único acento cálido de marca, metales solo en el podio.
- Todas las cifras (puntos, puestos, códigos, estadísticas, temporizador) en Archivo expandido (110–125 % de anchura).
- Filas separadas por filetes finos en lugar de tarjetas, tanto en el marcador como en las listas del docente.
- Mascota Coby: animada en acceso y registro, fija en el resto, con silueta como respaldo sin WebGL.
- Movimiento con una sola curva (`cubic-bezier(0.16, 1, 0.3, 1)`) y estado final garantizado sin animación.

## Colors

Una familia de azules (celeste, principal, secundario, profundo, noche, tinta), un coral complementario con su versión de texto, grises fríos y tres metales que solo tocan el podio.

### Primary
- **Azul principal** (blue-electric): la acción y la selección en operación. Botón primario, chip seleccionado, interruptor activo, borde de foco de campos, nodo seleccionado de la línea de tiempo, insignia de pregunta, fondo del avatar de usuario, controles nativos (`accent-color`).
- **Azul principal claro** (blue-electric-hi): anillo de foco del teclado en toda la aplicación.
- **Azul secundario** (blue-secondary): cifras de las estadísticas del panel, texto de la insignia «Publicado», de los avisos informativos y de la nota del inspector; extremo oscuro del degradado de las portadas de cuestionario.

### Secondary
- **Coral** (coral): acento cálido y no textual. Subrayado de la pestaña activa, punto de los indicadores (en vivo, pista del acceso), halo bajo Coby, insignia y fondo de icono de evento en el editor, línea de destino al arrastrar, anillo del nodo de inicio, borde de la opción activada, sonido activado en el escenario.
- **Coral texto** (coral-ink): el coral cuando lleva letra. Insignia «Borrador», botón coral, enlace secundario, aviso de cambios sin guardar, correo pendiente, hover de menús.
- **Coral velado** (coral-soft) y **filete coral** (coral-line): fondos de hover de menús y filas recientes, fondo del botón coral y de la insignia de borrador; el filete coral es el anillo del avatar y de la foto de perfil, el borde del botón coral y el color de selección de texto.

### Tertiary
- **Oro** (gold / gold-deep): primer puesto, y el remate de la barra central del logotipo.
- **Plata** (silver / silver-deep): segundo puesto.
- **Bronce** (bronze / bronze-deep): tercer puesto.

### Neutral
- **Tinta azul** (blue-ink): texto sobre superficie clara, texto sobre coral sólido y final del degradado del escenario.
- **Azul noche** (blue-night): inicio del degradado del escenario, lateral del acceso, nodos de inicio y fin de la línea de tiempo, fondo de botones fantasma oscuros.
- **Azul profundo** (blue-deep): texto del botón fantasma claro y códigos de partida.
- **Celeste** (blue-sky): la luz fría del escenario (arco de cuenta regresiva, haz, insignia «Tú», borde de opción elegida) y el texto secundario sobre azul noche en operación.
- **Celeste velado** (blue-sky-soft): fondo de avisos informativos, de la insignia «Publicado» y del rol seleccionado.
- **Azul noche claro** (blue-night-hi): inicio del degradado del lateral del acceso y de su franja móvil.
- **Familia del escenario**: niveles de azul casi negro ordenados de abajo arriba. stage-shadow da el tinte de las sombras largas, stage-floor el suelo, stage-field el recuadro del podio, stage-panel la clasificación, stage-raised la base de las gradas, stage-tile y stage-tile-hi las losas de opciones y comodines, stage-hover su estado al pasar el cursor, y stage-step y stage-step-hi el cuerpo de las gradas. Para el texto sobre el escenario están stage-soft y stage-soft-hi (niebla) y stage-faint (niebla tenue); stage-ink es el blanco, y stage-line es el filete celeste al 14 %. La luz del escenario ya no tiene token propio: es el mismo azul principal, aplicado como tinte al 20–35 %.
- **Celeste claro** (blue-sky-light) y **celeste medio** (blue-sky-mid): variantes de la luz fría del escenario, como el contraluz del avatar ganador.
- **Blanco** (white): texto principal sobre el escenario y superficie elevada clara.
- **Grises fríos** (gray-50 a gray-800): gray-50 es el fondo de operación, gray-200 la línea y el borde de campo, gray-600 el texto secundario, gray-100 el fondo de pills, del selector segmentado y de las teclas de respuesta.

### Estados
- **Éxito**, **error** y **aviso**: solo veredicto de respuesta, errores de formulario, acciones destructivas, avisos de validación y temporizador urgente. Nunca como decoración ni como acento de marca.
- Cada estado es una familia: base (borde, punto, veredicto), `-ink` (texto sobre claro), `-soft` (fondo de aviso), `-line` (borde de aviso) y, para éxito y error, `-on-dark` (texto sobre el escenario).

### Transparencias
Toda transparencia se escribe como `color-mix(in srgb, var(--token) N%, transparent)` sobre un token existente. Así ningún color nace fuera de la paleta.

### Named Rules
**The Metales-con-Dueño Rule.** El oro, la plata y el bronce marcan exclusivamente los puestos 1, 2 y 3 (y el remate dorado del logotipo, que representa ese podio). Ningún estado, botón, pill, estadística o realce genérico los usa.

**The Azul-Habla-Coral-Señala Rule.** El azul lleva la acción principal y la selección; el coral señala lo que está vivo, pendiente o activo y nunca sustituye al botón primario. El coral base no lleva texto sobre blanco: la letra coral es siempre coral-ink.

**The Sin-Violeta Rule.** Nada de morado, violeta, magenta ni púrpura: ni en gradientes, sombras de color, confeti ni en la mascota. La marca vive en los azules y en el coral; ningún tono intermedio los une.

## Typography

**Display Font:** Archivo Variable, eje de anchura (con Archivo, system-ui)
**Body Font:** Onest Variable (con Onest, system-ui)

**Character:** Archivo ensanchado al 110–125 % da el grafismo de marcador televisivo a cifras y titulares; Onest, más redonda y neutra, lleva nombres, explicaciones y controles con lectura cómoda en proyector y móvil.

### Hierarchy
- **Display** (800, clamp(2.6rem → 4.6rem), 1, anchura 125 %): numerales grandes del escenario. Número de grada, cuenta regresiva 3-2-1, código de partida del docente.
- **Headline** (760–780, clamp(1.7rem → 2.4rem), 1.1, anchura 112 %): títulos de página en operación (panel, perfil), título del acceso (1.9rem) y, en el escenario, el nombre del ganador.
- **Title** (720–760, 1.15–1.35rem, anchura 112–118 %): cabeceras de panel, del inspector, de secciones del editor y título del cuestionario en la barra del editor.
- **Score** (720–760, 1.1–2.9rem, anchura 115–122 %, dígitos tabulares): puntos en podio, filas, HUD, temporizador, estadísticas del panel docente (1.9rem) y puntuación de partidas recientes.
- **Body** (400, 1rem, 1.5): texto general; `text-wrap: pretty` en párrafos y `balance` en titulares.
- **Row name** (600–650, 1–1.05rem, 1.25): nombres en filas, títulos de cuestionario en listas, opciones de respuesta.
- **Label** (650–700, 0.72–0.78rem): insignias de estado y pills. Solo «Tú» y «Empate» del escenario van en mayúsculas con 0.06em.

### Named Rules
**The Cifra-Ancha Rule.** Toda cifra que se compara o se cuenta (puntos, puesto, código, segundos, estadísticas) va en Archivo expandido; el texto corrido nunca.

**The Ganador-Manda Rule.** En la ceremonia, el nombre y la puntuación del primer puesto son lo más grande de la pantalla; el título de la trivia queda en una franja estrecha (como máximo 1.6rem).

## Layout

Contenedores centrados con márgenes laterales fluidos (`clamp(16px, 4vw, 40px)`): 1320px para la ceremonia, 1180px para el panel docente, 1120px para el panel del participante y la consola, 1080px para el perfil, 760px para la vista del participante en juego. El ritmo usa pasos de 4px; el hueco por defecto entre bloques es 20px, entre controles 8–12px.

Operación: el panel docente apila cabecera, cuatro estadísticas en fila (dos en móvil) y una rejilla 1.7fr + lateral mínimo 280px (lista de cuestionarios a la izquierda, partidas recientes y en vivo a la derecha). El editor es una línea de tiempo pegajosa a la izquierda (mínimo 300px, 0.9fr) y un inspector a la derecha (1.4fr); la línea de tiempo enlaza nodos con un conector vertical de 2px donde aparecen los botones de insertar. El perfil usa navegación lateral pegajosa de 220px y secciones en panel. El acceso divide la pantalla en lateral azul noche con Coby (1fr) y formulario centrado de 420px (1.1fr).

Escenario: la ceremonia es una rejilla de dos columnas (podio 1.6fr, clasificación mínimo 360px) con franja de resumen arriba; el podio es una fila 1 : 1.35 : 1 (1 : 1.6 : 1 en móvil) en orden 2-1-3.

Puntos de corte observados: 960px (ceremonia, editor y panel docente a una columna), 900px (consola), 860px (panel del participante), 820px (el acceso oculta el lateral y muestra una franja azul noche con Coby de 120px sobre el formulario; el perfil pasa la navegación a fila), 760px (estadísticas a dos columnas), 620px (acciones de cuestionario bajo el título), 560px (opciones, rejillas de dos campos y nombre de cuenta), 520px (clasificación compacta).

## Elevation & Depth

Profundidad híbrida. En la superficie clara, sombras ambientales suaves teñidas de azul para paneles y la pestaña activa, y una sombra media para menús desplegables. En el escenario, la profundidad la da la luz: gradientes radiales, el haz cenital, aristas metálicas en las gradas y una sombra larga bajo los paneles flotantes. El lateral del acceso combina una radial azul principal bajo la mascota, una radial coral tenue arriba a la derecha y un halo elíptico coral bajo Coby.

### Shadow Vocabulary
Todas las sombras se tiñen mezclando un token con transparente: shadow-tint en la superficie clara, stage-shadow o blue-ink en el escenario.
- **Ambiente bajo** (`0 1px 2px` shadow-tint al 8 %, `0 2px 6px` al 6 %): paneles claros, inspector, pestaña activa, mando del interruptor.
- **Ambiente medio** (`0 4px 10px` y `0 12px 28px`, shadow-tint al 10 %): menú de cuenta y menú de eventos de la línea de tiempo.
- **Panel del escenario** (`0 18px 40px`, stage-shadow al 45 %): clasificación lateral y tarjeta de minijuego.
- **Brillo de acción** (`0 6px 16px`, azul principal al 28 %; 35 % en el botón del escenario): botón primario.
- **Anillo del avatar** (`0 0 0 2px <superficie>, 0 0 0 4px coral-line`): avatar de usuario; la foto de perfil usa un anillo coral de 4px.
- **Arista de grada** (`inset 0 3px 0 <metal>`, filo blanco al 12 %, resplandor del metal hacia arriba y `0 10px 30px` en stage-shadow al 60 %): borde superior iluminado de cada grada del podio.

### Named Rules
**The Un-Solo-Haz Rule.** Hay una única fuente de luz protagonista por escena: en la ceremonia, el haz que termina sobre el ganador; en el acceso, la luz bajo Coby. No se añaden focos ni brillos competidores.

## Shapes

Esquinas suaves y consistentes: 8px para controles pequeños, teclas de respuesta y elementos de menú; 9–10px para botones de icono, insignias de la línea de tiempo y enlaces de navegación lateral; 12px para campos, opciones, estadísticas, avisos, portadas de cuestionario y nodos de la línea de tiempo; 18px para paneles, inspector, línea de tiempo y escenarios. Botones, chips, pills, insignias de estado, interruptores, HUD y el selector segmentado son cápsulas completas (999px). Las gradas solo redondean las esquinas superiores (10px). Los avatares son círculos. Las filas de listas (marcador, cuestionarios, sesiones) son rectángulos sin redondeo separados por filetes de 1px; solo la fila propia del marcador y la fila reciente en hover se redondean.

## Components

### Buttons
Cápsulas firmes, de 44px de alto mínimo (36px en la variante pequeña), en peso 650.
- **Shape:** cápsula completa (999px).
- **Primary:** azul principal con texto blanco, 10px × 18px, brillo de acción debajo. Es el mismo azul en operación y en el escenario.
- **Hover / Focus:** en hover oscurece a azul principal profundo (blue-electric-deep); al pulsar baja 1px. El foco es un contorno de 3px en azul principal claro con 2px de separación. Deshabilitado al 55 %.
- **Coral (secundario):** fondo coral velado, filete coral y texto coral-ink; en hover el fondo pasa a coral-soft-hi y el borde a coral. Para acciones complementarias, nunca la principal.
- **Ghost (claro):** blanco con borde gris-200 y texto azul profundo; en hover borde y texto azul principal.
- **Ghost (escenario):** azul noche translúcido, borde celeste al 35 %, texto celeste.
- **Quiet:** transparente con texto gris; en hover fondo gray-100.
- **Danger:** blanco con borde danger-line y texto danger-ink; en hover fondo danger-soft.
- **Botón de icono:** cuadrado de 36px, radio 10px, borde de línea; en hover icono y borde azul principal (rojo en la variante destructiva).

### Chips
- **Chip de opción:** cápsula blanca de 36px con borde de línea y texto gris; seleccionado, relleno azul principal con texto blanco.
- **Insignias de estado:** cápsula con punto de 6px del color del texto. Borrador en coral velado con coral-ink; publicado en celeste velado con azul secundario.
- **Pills de sesión:** cápsula gray-100; en curso, tinte azul; terminada, celeste velado.
- **Insignias del escenario:** «Tú» en cápsula celeste con tinta azul; «Empate» con borde de filete.

### Cards / Containers
- **Corner Style:** 18px en paneles; 12px en estadísticas.
- **Background:** blanco sobre gray-50 (operación); panel del escenario o gradiente azul en el escenario.
- **Shadow Strategy:** ambiente bajo en claro; sombra de panel del escenario en oscuro.
- **Border:** 1px gris-200 en claro; filete celeste al 14–20 % en el escenario.
- **Internal Padding:** 22px en paneles, 20px en el inspector, 14px × 16px en estadísticas.
- **Estadística del panel:** etiqueta gris pequeña bajo una cifra en Archivo 120 % de 1.9rem en azul secundario; la variante de acento usa filete coral, un degradado coral velado a blanco y cifra coral-ink.

### Inputs / Fields
- **Style:** 46px de alto, 10px × 14px, radio 12px, borde gris-200 sobre blanco; hover borde gris-400. Las áreas de texto siguen la misma piel.
- **Focus:** borde azul principal con halo de 4px al 18 %.
- **Error / Disabled:** borde rojo de estado y mensaje de 0.82rem; deshabilitado con fondo gray-100.
- **Contraseña:** botón de ojo de 40px dentro del campo; en hover o activo, coral velado con coral-ink.
- **Interruptor:** pista de 42 × 24px gris-200, mando blanco de 18px; activo en azul principal.
- **Código de partida:** campo de 56px con Archivo a 125 % de anchura, 1.6rem, mayúsculas y 0.18em de espaciado.

### Navigation
- **Operación:** barra blanca con filete inferior gray-200, logotipo a la izquierda y menú de cuenta a la derecha: avatar con anillo coral, nombre y rol; desplegable blanco de 230px con ambiente medio y elementos que en hover pasan a coral velado con coral-ink. En móvil se oculta el nombre.
- **Selector segmentado:** cápsula gray-100; la pestaña activa es blanca con ambiente bajo y un subrayado coral interior de 2px.
- **Perfil:** navegación lateral de enlaces de 10px de radio; hover coral velado.
- **Escenario:** enlace de vuelta en niebla y marca invertida; botón de sonido circular de 40px que pasa a coral al activarse.
- **Logotipo:** tres barras de podio en azul principal con remate dorado y la palabra en Archivo con «Quick» en azul principal (celeste en versión invertida).

### Coby (mascota)
Cobra procedural en three.js: cuerpo celeste con anillos, vientre melocotón, interior de capucha y mejillas coral, pupilas azul noche, boca azul secundario, iluminación con contraluz coral y relleno celeste. Poses: reposo, saludo, mirar el formulario, taparse los ojos (contraseña oculta), espiar (contraseña visible), error, celebración y registro. Se anima solo en acceso y registro; en estados vacíos y verificación se usa una imagen fija renderizada una vez; sin WebGL se muestra una silueta plana.

### Línea de tiempo del editor
Columna de nodos enlazados por un conector vertical. Inicio y fin en azul noche con texto blanco y subtítulo celeste; preguntas en blanco con insignia numerada azul principal; eventos en coral velado con insignia coral y texto tinta. Seleccionado: borde azul principal con halo al 20 %. Al pasar o enfocar un hueco aparecen botones cápsula para insertar pregunta (azul) o evento (coral-ink); al arrastrar, el conector se ensancha a 4px en coral.

### Podio (componente firma)
Tres gradas en orden 2-1-3 con alturas proporcionales al puesto, cuerpo en gradiente azul del escenario, arista superior del metal del puesto, medalla y numeral contorneado en la base. Secuencia: cuenta 3-2-1, suben las gradas, aparecen segundo y tercero, el haz se fija sobre el ganador mientras su marcador cuenta hacia arriba y entra la clasificación fila a fila (45ms). El estado final queda visible sin depender de las animaciones; con movimiento reducido todo aparece sin transición.

### Clasificación (marcador)
Panel del escenario con filas en rejilla (puesto, avatar, nombre, aciertos, puntos) separadas por filetes celestes al 16 %. Los puestos 1–3 llevan ficha y tinte del metal; los empates comparten puesto con «=»; la fila propia se rellena de azul al 24 % con contorno interior.

### Opciones de respuesta
Losas de 64px con tecla de letra en Archivo. Elegida: borde celeste y tinte azul al 35 %. Veredicto: borde y tinte verde o rojo de estado. El resto se atenúa tras responder.

## Do's and Don'ts

### Do:
- **Do** usar el azul principal para la acción primaria y la selección, y el coral para indicadores, estados pendientes, acentos activos y acciones secundarias.
- **Do** escribir todo texto coral en coral-ink; el coral base solo como relleno, borde, punto o subrayado.
- **Do** expresar todo color como token, y toda transparencia como `color-mix(in srgb, var(--token) N%, transparent)`.
- **Do** poner toda cifra comparable en Archivo expandido (110–125 %).
- **Do** reservar oro, plata y bronce a los puestos 1–3 y al remate del logotipo.
- **Do** separar filas de listas y marcadores con filetes de 1px en lugar de tarjetas o sombras.
- **Do** animar a Coby solo en acceso y registro, y usar su imagen fija en el resto.
- **Do** usar la curva `cubic-bezier(0.16, 1, 0.3, 1)` para todo movimiento y garantizar un estado final legible sin animación y con `prefers-reduced-motion`.
- **Do** mantener el foco visible: contorno de 3px en azul principal claro con 2px de separación.

### Don't:
- **Don't** usar morado, violeta, magenta ni púrpura en ningún elemento, gradiente o sombra.
- **Don't** usar oro, plata o bronce para estados, pills, botones, estadísticas o realces que no sean un puesto del podio.
- **Don't** convertir el coral en el color del botón primario ni usar coral base para texto sobre blanco.
- **Don't** repartir la ceremonia en una rejilla de tarjetas iguales ni usar confeti de colores de feria.
- **Don't** añadir una segunda fuente de luz que compita con el haz del ganador o con la luz de Coby.
- **Don't** usar los colores de éxito, error o aviso como acento decorativo.
- **Don't** animar la mascota fuera del acceso ni dejar que desplace el formulario en móvil.
