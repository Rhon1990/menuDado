# Mensajería cercana y entrada analítica de la colmena

## Objetivo

Hacer que el aviso mostrado cuando la generación no encuentra una idea se sienta
más humano, cercano y coherente con MenuDado. A la vez, registrar de forma
explícita cada entrada al flujo de respaldo para poder medir cuántas búsquedas
comienzan y cómo terminan.

El cambio no expondrá al usuario la existencia de Firestore, la colmena o el
fallback. Tampoco enviará a Analytics restricciones, ingredientes, edad, texto
libre del perfil ni ningún otro dato personal.

## Enfoques considerados

### Reutilizar el evento final con estado `started`

Evita ampliar la interfaz de Analytics, pero mezcla en un mismo parámetro estados
de ciclo de vida y resultados terminales. Las consultas y embudos serían menos
claros. Se descarta.

### Inferir la entrada desde otros eventos de generación

No añade eventos, pero no permite distinguir una generación resuelta en vivo de
una que realmente entró al respaldo. No satisface la necesidad de observabilidad.
Se descarta.

### Añadir un evento explícito de entrada

Se añadirá `ai_menu_hive_fallback_started` al comenzar la búsqueda y se mantendrá
`ai_menu_hive_fallback` para el resultado. Es el enfoque seleccionado porque
produce un embudo claro, conserva compatibilidad con los datos actuales y
requiere un cambio pequeño.

## Mensajería

### Español

Estado de espera:

- Título: `Esta vez no encontramos tu idea`
- Mensaje contextual de alta demanda, usando como ejemplo la captura aprobada:
  `Queremos proponerte algo que encaje de verdad contigo, pero ahora mismo la IA
  necesita un pequeño respiro para Almuerzo · Persona adulta (18+ años) con tu
  perfil actual.`
- Destacado: `Démonos un momento`
- Apoyo: `Cuando termine la pausa, este aviso te lo dirá. Tus preferencias
  seguirán aquí para que puedas volver a intentarlo con tranquilidad.`

Estado disponible:

- Título: `¿Probamos otra vez?`
- Destacado: `Ya estamos listos`
- Apoyo: `Cierra este aviso y vuelve a tocar Ayúdame a elegir. Mantendremos tus
  preferencias para buscar una idea que encaje contigo.`

El sufijo `con tu perfil actual` seguirá apareciendo únicamente cuando existan
restricciones. Tipo, público y edad continuarán usando las etiquetas localizadas
actuales.

El mensaje interpolará las etiquetas localizadas del tipo, público y edad
seleccionados. Omitirá `con tu perfil actual` cuando no existan restricciones.

### Inglés

Estado de espera:

- Título: `We didn’t find your idea this time`
- Mensaje contextual de alta demanda: `We want to suggest something that truly
  fits you, but AI needs a short break before preparing an idea for Lunch ·
  Adult (18+ years) with your current profile.`
- Destacado: `Let’s give it a moment`
- Apoyo: `When the pause is over, this notice will let you know. Your preferences
  will still be here so you can try again with peace of mind.`

Estado disponible:

- Título: `Shall we try again?`
- Destacado: `We’re ready`
- Apoyo: `Close this notice and tap Help me choose again. We’ll keep your
  preferences to look for an idea that fits you.`

### Francés

Estado de espera:

- Título: `Cette fois, nous n’avons pas trouvé votre idée`
- Mensaje contextual de alta demanda: `Nous voulons vous proposer quelque chose
  qui vous corresponde vraiment, mais l’IA a besoin d’une courte pause avant de
  préparer une idée pour Déjeuner · Adulte (18 ans et plus) avec votre profil
  actuel.`
- Destacado: `Donnons-lui un instant`
- Apoyo: `Lorsque la pause sera terminée, cet avis vous l’indiquera. Vos
  préférences resteront disponibles afin que vous puissiez réessayer
  sereinement.`

Estado disponible:

- Título: `On réessaie ?`
- Destacado: `Nous sommes prêts`
- Apoyo: `Fermez cet avis et touchez de nouveau Aidez-moi à choisir. Nous
  conserverons vos préférences pour chercher une idée qui vous corresponde.`

Inglés y francés interpolarán igualmente sus etiquetas localizadas y omitirán el
sufijo del perfil cuando no existan restricciones.

Los avisos de límite diario, timeout, conectividad e indisponibilidad conservarán
su causa y acción de recuperación actuales. Este alcance cambia el tono general
del modal y el mensaje de alta demanda mostrado en el caso aprobado.

## Analítica

Se ampliará `MenuDadoAnalytics` con una operación específica de entrada:

`trackAiMenuHiveFallbackStarted(mealType, triggerFailureType)`

`FirebaseMenuDadoAnalytics` emitirá:

- evento: `ai_menu_hive_fallback_started`;
- parámetro `meal_type`;
- parámetro `failure_type`.

La llamada se realizará una vez al entrar en `searchHiveFallback`, antes de leer
la rotación local o consultar servidor/caché. Se ejecutará tanto después de un
fallo real del proveedor como cuando el límite técnico envíe directamente al
respaldo.

El evento terminal `ai_menu_hive_fallback` se mantiene sin cambios y seguirá
registrando:

- `hit`: candidato compatible del servidor;
- `cache_hit`: candidato compatible de caché;
- `miss`: búsqueda completada sin candidato utilizable;
- `error`: fallo de servidor y caché.

El evento de entrada se protegerá con `runCatching`, como el resto de la
analítica, para que un problema de telemetría nunca bloquee la generación.

## Flujo

1. La generación en vivo falla o el límite técnico deriva directamente al
   respaldo.
2. El `ViewModel` entra en `searchHiveFallback`.
3. Se registra `ai_menu_hive_fallback_started`.
4. Se consulta la colmena y se valida el candidato contra el perfil vigente.
5. Se registra el evento terminal existente con el resultado.
6. Si no hay candidato, se muestra el aviso con el nuevo tono; si lo hay, se abre
   el detalle normalmente.

## Pruebas

### Mensajería

- alta demanda en español usa el nuevo texto y conserva contexto, edad y sufijo
  condicional del perfil;
- inglés y francés contienen el mismo significado;
- el modal de espera y el estado listo usan los nuevos recursos en los tres
  idiomas;
- ningún texto menciona colmena, Firestore, fallback o contenido compartido.

### Analítica

- un fallo del proveedor registra primero `ai_menu_hive_fallback_started` y
  después el resultado terminal;
- un acceso directo por límite técnico también registra la entrada;
- cada búsqueda registra exactamente una entrada;
- `hit`, `cache_hit`, `miss` y `error` continúan disponibles;
- los eventos no contienen perfil, ingredientes ni texto libre.

### Regresión

- ejecutar los tests unitarios enfocados de mensajes, Analytics y `ViewModel`;
- ejecutar la suite unitaria completa de `app`;
- compilar Kotlin debug;
- revisar `git diff --check`;
- actualizar `docs/project-context.md` con el nuevo copy y el evento de entrada.

## Criterios de aceptación

1. El modal aprobado usa un tono cercano en español, inglés y francés.
2. El mensaje sigue siendo honesto: no promete que exista una receta de respaldo.
3. Cada entrada a la colmena emite exactamente un evento de inicio.
4. El evento final actual mantiene sus resultados y compatibilidad.
5. No se registran ni muestran detalles internos o datos sensibles.
6. La telemetría no puede alterar ni bloquear el flujo funcional.
7. Las pruebas y la compilación seleccionadas finalizan correctamente.
