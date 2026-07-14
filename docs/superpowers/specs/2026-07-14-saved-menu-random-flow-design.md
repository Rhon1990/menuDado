# Flujo aleatorio de menús guardados

## Objetivo

Separar visual y funcionalmente la generación con IA de la selección aleatoria local, y hacer evidente que el usuario puede pedir otra opción sin consumir usos de IA.

## Problema confirmado

- `state.isRolling` representa la selección aleatoria entre menús guardados, pero actualmente también cambia la pose del dado mostrado dentro de la acción de IA.
- El botón `Elegir entre mis menús` no explica que la selección es aleatoria ni ofrece feedback propio mientras busca.
- El detalle resultante no ofrece una acción visible para elegir otro menú; el usuario debe cerrar el modal y deducir que puede pulsar nuevamente el botón.

## Diseño aprobado

### Separación de flujos

- El dado del botón `Lanzar con IA` solo se anima y cambia de pose durante una generación con IA.
- La selección aleatoria local mantiene `isRolling` para bloquear dobles pulsaciones y respetar la duración actual, pero no modifica el dado IA.
- La selección local no realiza llamadas a Firebase AI Logic ni consume cuota de IA.

### Acción de menús guardados

- El texto principal será `Elegir un menú al azar`.
- El texto secundario será `Entre tus menús guardados`.
- Mientras `isRolling` esté activo, el botón mostrará un indicador de progreso compacto y el texto `Buscando entre tus menús…`.
- La acción seguirá usando los filtros de comida y público elegidos en la parte superior.
- El control conservará el estilo secundario de MenuDado para no competir con la llamada principal de IA.

### Resultado y repetición

- El resultado seguirá abriendo el detalle normal del menú guardado.
- Si el detalle se abrió desde la selección aleatoria, mostrará una acción secundaria `Elegir otro menú` antes de `Cerrar`.
- `Elegir otro menú` cerrará el detalle actual y ejecutará otra selección con los mismos filtros.
- La acción no aparecerá cuando el detalle se abra desde `Tu último menú`, una tarjeta, favoritos o una lista por público.
- La selección existente seguirá evitando repeticiones durante el día mientras queden candidatos nuevos; cuando se agoten, reiniciará el ciclo según el comportamiento actual.

## Estado y componentes

- `MenuDadoViewModel` conserva el contrato actual de selección (`rollDice`, `isRolling`, `result` y recuperación vacía).
- `MenuDadoScreen` mantendrá un origen explícito y guardable para distinguir un detalle abierto por selección aleatoria de un detalle abierto por navegación normal.
- `TodayMenuSection` mostrará el estado propio del botón local mediante `state.isRolling`.
- `MenuDetailDialog` recibirá una acción opcional para elegir otro menú; su ausencia conservará el comportamiento actual.
- Los textos nuevos se añadirán en español, inglés y francés.

## Flujo de datos

1. El usuario elige comida y público.
2. Pulsa `Elegir un menú al azar`.
3. El botón local entra en estado `Buscando entre tus menús…`; el dado IA permanece inmóvil.
4. `MenuDadoViewModel.rollDice()` selecciona un candidato local respetando filtros y memoria diaria.
5. `MenuDadoScreen` marca el detalle como originado por selección aleatoria y abre el menú resultante.
6. El usuario puede cerrar o pulsar `Elegir otro menú` para repetir el paso 2 con los mismos filtros.

## Recuperación y analítica

- Si faltan filtros, se mantienen los mensajes actuales.
- Si no existen coincidencias, se mantiene el diálogo actual para generar con IA, ampliar el tipo de comida o cambiar filtros.
- La primera selección conserva `choose_saved_menu` y `dice_rolled`.
- La repetición desde el detalle se marcará con un CTA cerrado específico, sin nombres, ingredientes ni identificadores del menú.

## Validación

- Prueba de regresión que confirme que `isRolling` no avanza la pose ni anima el dado IA.
- Pruebas del contrato textual y del estado ocupado del botón de selección local.
- Pruebas de visibilidad de `Elegir otro menú` únicamente cuando el detalle proviene del flujo aleatorio.
- Prueba ViewModel existente y/o ampliada para confirmar filtros, no repetición, duración y recuperación vacía.
- Suite `:app:testDebugUnitTest`, compilación `:app:assembleDebug` y `git diff --check`.
- QA manual: selección inicial, indicador local, dado IA inmóvil, detalle resultante, elección de otra opción, cierre, filtros sin resultados y ausencia de errores en Logcat.

## Versión de la entrega

- La versión visible cambia de `1.1.0` a `1.2.0` porque la entrega agrupa una mejora funcional y de experiencia de usuario, no solo una corrección interna.
- `versionCode` aumenta de `10` a `11` para mantener el orden requerido por Android y Google Play.
- El cambio debe mantenerse sincronizado en Gradle, el contrato de `Acerca de la app` y `docs/project-context.md`.
- La validación del APK debe confirmar `versionName=1.2.0` y `versionCode=11` desde los metadatos generados del build.

## Fuera de alcance

- Cambiar el algoritmo aleatorio o la memoria diaria.
- Añadir una segunda ilustración de dado o una tarjeta completa adicional.
- Modificar cuotas, prompts, modelo o configuración de Firebase AI Logic.
- Rediseñar el detalle general de menús.
