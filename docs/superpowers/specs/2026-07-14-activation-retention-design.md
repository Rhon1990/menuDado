# MenuDado Activation and Retention Design

## Objetivo

Mejorar la activacion inicial y la recurrencia de MenuDado reduciendo las decisiones antes del primer resultado util, facilitando el guardado y eliminando los callejones sin salida del dado.

La primera entrega se considera exitosa si permite medir y mejorar este embudo:

`onboarding -> generar o elegir -> obtener resultado -> guardar -> volver y abrir el menu reciente`

## Contexto confirmado

- En Firebase, durante los 28 dias analizados, hubo 98 usuarios: 66 nuevos y 32 recurrentes.
- La retencion observada fue aproximadamente 12,3 % en dia 1 y 2,9 % en dia 7.
- Solo 8 usuarios registraron `first_menu_created`, frente a 66 `first_open` en la misma ventana.
- La IA es la accion funcional con mayor adopcion: 44 usuarios iniciaron generaciones.
- El dado produjo 51 resultados vacios sobre 205 tiradas, aproximadamente un 24,9 %.
- La aplicacion tiene 100 % de usuarios sin fallos registrados, por lo que el principal riesgo observado es de activacion y valor recurrente, no de estabilidad.
- El Home actual ya tiene modo IA por defecto, selectores de comida y publico, ingredientes opcionales, dado, formulario manual, detalle de idea generada, favoritos y carruseles.
- Room ya devuelve menus por `createdAt DESC`; no se necesita migracion para mostrar el menu mas reciente.
- La analitica ya centraliza eventos en `MenuDadoAnalytics` y prohibe contenido personal o libre del menu.

Las proporciones de Firebase son indicios de producto, no conversiones estrictas de cohorte, porque el informe actual no separa todos los eventos entre usuarios nuevos y recurrentes.

## Alcance aprobado

### Incluido

- Onboarding de una sola pagina orientado a ejecutar la primera accion.
- Reorganizacion visual de Home con IA como accion principal.
- Accesos secundarios a elegir entre menus guardados y escribir un menu.
- Seccion de continuidad con el menu guardado mas reciente.
- Guardado destacado en el detalle de una idea generada.
- Recuperacion explicita cuando el dado no encuentra coincidencias.
- Analitica necesaria para medir el nuevo embudo.
- Textos equivalentes en espanol, ingles y frances.
- Pruebas unitarias, compilacion y QA manual guiado.

### Excluido

- Notificaciones, permisos o trabajos programados.
- Planificador semanal.
- Referidos, campañas o cambios en la ficha de Google Play.
- Nuevas preferencias como `rapido`, `economico` o `familiar` en el prompt IA.
- Cambios de modelo, cuota, App Check o configuracion de Firebase AI Logic.
- Migraciones Room, nuevos documentos Firestore o cambios de autenticacion.
- Publicidad nueva o cambios de monetizacion.

El mockup de la opcion A define la jerarquia visual. Los chips ilustrativos del primer mockup no forman parte de esta entrega; se reutilizan los selectores reales de desayuno, almuerzo, cena y publico para evitar ampliar el contrato IA.

## Diseno UX

### 1. Primera apertura

El onboarding pasa de cinco explicaciones a una sola pagina con:

- Promesa: resolver que comer en menos de un minuto.
- Explicacion breve: generar una idea con IA o elegir entre menus guardados.
- CTA principal `Crear mi primer menu`.
- Accion secundaria `Explorar por mi cuenta`.
- Mensajes breves de confianza: no exige registro y los datos permanecen bajo control del usuario.

`Crear mi primer menu` completa el onboarding y deja Home en modo IA, que ya es el estado por defecto. `Explorar por mi cuenta` conserva la semantica actual de omitir.

No se incrementa `CURRENT_ONBOARDING_VERSION`: los usuarios que ya completaron la version actual no deben volver a recibir un modal introductorio. Las instalaciones nuevas veran el contenido simplificado.

### 2. Home orientado a una accion principal

`Que comer hoy` se convierte en el primer bloque de valor del Home:

- Titulo directo y una frase corta.
- Selectores existentes de comida y publico visibles antes de actuar.
- Ingredientes base se mantienen opcionales y visualmente secundarios.
- CTA principal `Generar mi menu` usa el flujo IA existente.
- Accion secundaria `Elegir entre mis menus` usa los mismos filtros y el selector aleatorio existente.
- Accion terciaria `Escribir mi menu` revela el formulario manual existente.

`HomeMenuMode` se conserva para no romper el contrato actual. IA sigue siendo el modo inicial; el modo manual solo controla la visibilidad del formulario de escritura. La seleccion desde menus guardados se expone como accion directa y no como una interpretacion ambigua del modo manual.

No se duplica el bloque historico `DiceSection`. El Home debe tener una unica entrada para elegir que comer, reutilizando `ContextualDiceButton`, `CompactMenuSelectors`, `MenuTextFields` y `SaveMenuButton` donde resulten adecuados.

### 3. Continuidad para usuarios que vuelven

Si existe al menos un menu, Home muestra `Continua donde lo dejaste` con el menu de mayor `createdAt`:

- Nombre, tipo de comida y estado visual disponible.
- Al tocarlo se abre `MenuDetailDialog`.
- La apertura reutiliza `trackMenuCardOpened` y añade un CTA cerrado que identifica el origen `recent_menu`.

Si no existen menus, la seccion no aparece. No se crea estado persistente nuevo: la lista actual es la fuente de verdad.

### 4. Resultado generado

`GeneratedMenuDetailDialog` conserva nombre, descripcion, calorias y analisis, pero refuerza la jerarquia:

- CTA principal de ancho completo `Guardar en mis menus`.
- Accion secundaria `Probar otra idea` solo debe iniciar una nueva generacion tras un toque explicito y debe respetar throttle, cuota diaria, limites de invitado y manejo de errores existentes.
- Cerrar o retroceder mantiene la semantica segura de descartar el borrador sin guardar.

Tras guardar:

- Se reutiliza `saveGeneratedMenuIdea()` y, por tanto, `saveMenu()`.
- Se cierra el detalle.
- Se muestra una confirmacion breve no bloqueante.
- El menu aparece como reciente sin una escritura adicional.

### 5. Recuperacion del dado sin coincidencias

El mensaje generico se reemplaza por un estado explicito de recuperacion cuando no existen candidatos para comida y publico seleccionados.

Acciones:

- `Crear una idea con IA`: conserva comida y publico y llama al flujo IA existente mediante un toque explicito.
- `Probar otro tipo de comida`: se ofrece solo si existen menus de otra comida para el mismo publico. Ejecuta una seleccion aleatoria ignorando temporalmente el tipo de comida, conserva el publico y no modifica los selectores visibles.
- `Cambiar filtros`: cierra la recuperacion y devuelve el foco a los selectores.

Si solo se agotaron las opciones del dia pero existen candidatos, se conserva el reseteo diario actual y no se muestra el estado vacio.

## Arquitectura y estado

### UI

`MenuDadoScreen.kt` mantiene la composicion principal. Los cambios se limitan a componentes pequenos y privados:

- Onboarding simplificado.
- Hero/acciones de `TodayMenuSection`.
- Tarjeta de menu reciente.
- Dialogo o sheet de recuperacion sin resultados.
- Jerarquia de acciones del detalle generado.

No se crea una segunda pantalla Home ni una nueva ruta de navegacion.

### ViewModel

`MenuDadoUiState` incorpora un estado explicito y minimo para la recuperacion del dado. Debe permitir distinguir:

- No hay menus para la combinacion actual.
- Existen menus de otra comida para el mismo publico.
- La recuperacion esta cerrada.

Las acciones del ViewModel deben:

- Abrir recuperacion solo cuando `DiceSelector` confirma ausencia real de candidatos.
- Conservar siempre `formAudience`/`diceAudienceFilter` al ampliar la comida; la seleccion ampliada ignora el tipo solo para esa tirada y no muta el formulario.
- Limpiar el estado de recuperacion al cambiar filtros, obtener resultado o abandonar el flujo.
- Reutilizar `generateMenuIdea()`, `rollDice()` y `saveMenu()` en vez de duplicar limites o persistencia.

La seleccion del menu reciente debe ser una funcion pura basada en `createdAt`, para que el comportamiento no dependa accidentalmente del orden de una lista falsa en tests.

### Datos y backend

- Room y Firestore no cambian.
- No hay migraciones de esquema.
- No se persisten estados de coaching, recuperacion o menu reciente.
- No cambia el prompt de IA ni el formato de `GeneratedMenu`.

## Analitica

Se mantienen eventos y parametros sin contenido personal.

### Embudo

- `onboarding_shown`
- `onboarding_completed` con `action=start` o `skip`
- `ai_menu_gen_started`
- `ai_menu_gen_finished`
- `menu_saved`
- `first_menu_created`
- `menu_card_opened`

### Acciones nuevas o aclaradas

- `cta_tapped` usa valores cerrados para:
  - `create_first_menu`
  - `explore_without_onboarding`
  - `generate_menu`
  - `choose_saved_menu`
  - `write_menu`
  - `open_recent_menu`
  - `save_generated_menu`
  - `try_another_generated_menu`
- Evento dedicado `dice_empty_recovery` con parametro cerrado `action`:
  - `shown`
  - `generate_ai`
  - `broaden_meal_type`
  - `change_filters`

No se envian nombres, ingredientes, notas, UID, correo, IDs locales, alergias, condiciones ni texto libre.

Para poder responder en Firebase cual es el boton mas usado, el despliegue incluye registrar en GA4 las dimensiones personalizadas de alcance evento `cta`, `screen` y `action`. El registro no recupera datos historicos; solo aplica hacia adelante.

## Manejo de errores

- IA conserva timeout, throttle, cuota diaria, limites de invitado y mensajes actuales.
- `Probar otra idea` queda deshabilitado mientras existe una solicitud activa.
- Si IA no esta disponible desde la recuperacion, el aviso existente se muestra sin perder filtros.
- La ampliacion del tipo de comida nunca cambia el publico seleccionado.
- Un resultado vacio no borra formularios, menus ni perfil.
- Volver o cerrar un detalle generado no guarda implicitamente.

## Accesibilidad y localizacion

- Todos los textos visibles usan recursos Android en `values`, `values-en` y `values-fr`.
- CTAs mantienen un minimo tactil de 48 dp.
- No se usa solo el color para explicar salud, seleccion o error.
- Las acciones principales tienen `contentDescription` o texto visible suficiente.
- Los textos soportan escalado de fuente y evitan una sola linea cuando el contenido puede crecer.

## Validacion QA

### Pruebas unitarias

- Onboarding contiene una sola propuesta de valor y conserva acciones `start`/`skip`.
- El modo inicial sigue siendo IA.
- El menu reciente es el de mayor `createdAt`, aunque la lista no venga ordenada.
- Un resultado real cierra cualquier recuperacion previa.
- La ausencia de candidatos abre recuperacion y registra `shown`.
- La ampliacion de comida conserva el publico.
- `Crear una idea con IA` reutiliza las validaciones y cuotas actuales.
- Guardar una idea generada persiste analisis y calorias sin repetir IA.
- Los nuevos eventos solo aceptan valores cerrados y no contienen datos personales.

### Verificacion automatizada

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:lintDebug`

### QA manual

- Instalacion limpia: onboarding de una pagina y CTA hacia Home IA.
- Usuario existente con onboarding completado: no vuelve a mostrarse.
- Home sin menus: IA principal, formulario manual accesible y sin tarjeta reciente.
- Home con menus: tarjeta reciente correcta y detalle accesible.
- IA correcta: resultado, guardado, confirmacion y menu reciente.
- IA en throttle, sin cuota o con fallo: aviso existente y filtros conservados.
- Dado con coincidencia: comportamiento aleatorio y memoria diaria intactos.
- Dado sin coincidencia, pero con otra comida del mismo publico: ofrece ampliar comida.
- Dado sin ningun menu del publico: ofrece IA y cambio de filtros, sin mezclar publicos.
- Espanol, ingles y frances con fuente grande y pantalla estrecha.

## Riesgos y mitigaciones

- `MenuDadoScreen.kt` tiene mas de 5.000 lineas. Se evitaran refactors amplios y se extraeran solo funciones privadas pequenas cuando reduzcan complejidad del cambio.
- Una accion `Probar otra idea` puede consumir cuota. Debe indicar el conteo disponible cuando corresponda y nunca ejecutarse automaticamente.
- Mostrar onboarding actualizado a usuarios antiguos seria molesto. Se mantiene la version actual para limitarlo a nuevas instalaciones.
- Ampliar filtros entre publicos podria producir recomendaciones inadecuadas. Solo se amplia el tipo de comida dentro del mismo publico.
- Los parametros de Firebase no son visibles historicamente sin dimensiones personalizadas. El alta en GA4 es una tarea de despliegue y su efecto es prospectivo.

## Archivos previstos

- `app/src/main/java/com/menudado/ui/MenuDadoScreen.kt`
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- `app/src/main/java/com/menudado/analytics/MenuDadoAnalytics.kt`
- `app/src/main/java/com/menudado/analytics/FirebaseMenuDadoAnalytics.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/test/java/com/menudado/ui/MenuCardUiStateTest.kt`
- `app/src/test/java/com/menudado/ui/MenuDadoViewModelTest.kt`
- `docs/project-context.md`

La lista puede reducirse durante la implementacion si una pieza existente permite cumplir el contrato con menos cambios.
