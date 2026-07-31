# Rotación diaria de colmena sin repeticiones

## Objetivo

Evitar que una cuenta o invitado reciba durante el mismo día una receta de colmena que ya se le mostró. Cuando se agoten las opciones compatibles, MenuDado debe esperar a que Gemini pueda volver a intentarse o pedir que se vuelva al día siguiente, en lugar de reiniciar inmediatamente el conjunto pequeño de recetas.

## Comportamiento actual

La colmena prioriza candidatos compatibles no vistos, pero cuando todos han sido mostrados inicia inmediatamente un ciclo nuevo. Con dos o tres candidatos esto produce repeticiones el mismo día y puede hacer creer que Gemini genera siempre las mismas recetas.

## Comportamiento aprobado

- La rotación seguirá siendo independiente por invitado y por cuenta.
- Cada receta compatible se mostrará como máximo una vez durante el mismo día de uso.
- La búsqueda devolverá ausencia de resultado cuando todos los candidatos compatibles ya hayan sido vistos ese día. No reiniciará el ciclo inmediatamente.
- Al cambiar el día, el historial diario se reiniciará y las recetas anteriores podrán volver a mostrarse.
- No se almacenarán recetas, perfiles ni texto libre en el historial; únicamente la clave de día y hasta 24 `semanticHash`.
- La validación de público, edad, embarazo, veganismo, alergias y alimentos evitados seguirá ejecutándose antes de mostrar una receta.
- Los documentos equivalentes que la deduplicación semántica agrupa como una sola receta cuentan como una única opción diaria.

## Relación con la recuperación de Gemini

La app no realizará llamadas automáticas para comprobar si Gemini está disponible. Continuará usando el `retryAtMillis` derivado de la respuesta del proveedor:

- Mientras el plazo siga activo, una generación autorizada consulta directamente opciones de colmena no vistas.
- Al vencer el plazo, el siguiente toque del usuario vuelve a llamar a Gemini, aunque aún sea el mismo día.
- Una respuesta correcta confirma la recuperación, limpia la pausa y muestra la idea como `Idea generada con IA`.
- Un nuevo fallo actualiza la pausa y vuelve a consultar la colmena. Si ya no quedan opciones diarias, se muestra el aviso de agotamiento.
- Si Firebase no proporciona un plazo concreto, se mantiene el respaldo actual de reintentar tras el siguiente reinicio diario del proveedor.

## Mensaje al agotar opciones

No se mencionarán Gemini, Firestore, fallback ni colmena. Para no prometer que hay que esperar hasta mañana cuando Firebase haya indicado una recuperación más corta, el aviso dependerá del siguiente reintento permitido.

Cuando `retryAtMillis` vence antes del siguiente reinicio diario:

| Idioma | Texto |
| --- | --- |
| Español | `Ya viste todas las ideas diferentes disponibles por ahora. Vuelve a intentarlo en un momento.` |
| Inglés | `You've seen all the different ideas available for now. Try again in a little while.` |
| Francés | `Vous avez vu toutes les idées différentes disponibles pour le moment. Réessayez dans quelques instants.` |

Cuando no existe una recuperación más corta o se alcanzó el límite técnico diario:

| Idioma | Texto |
| --- | --- |
| Español | `Ya viste todas las ideas disponibles por hoy. Vuelve mañana para encontrar una nueva.` |
| Inglés | `You've seen all the ideas available for today. Come back tomorrow to find a new one.` |
| Francés | `Vous avez vu toutes les idées disponibles aujourd’hui. Revenez demain pour en découvrir une nouvelle.` |

Este aviso solo corresponde a una búsqueda correcta cuyo conjunto compatible está totalmente visto. Los errores de red, una colmena deshabilitada, una respuesta remota inválida o la ausencia inicial de candidatos conservan sus mensajes existentes.

## Diseño técnico

### Historial diario

`HiveRotationSnapshot` incorporará la clave del día asociada a los hashes. `HiveRotationStore` recibirá la clave del día al leer y registrar:

- Si la clave guardada coincide, devuelve los hashes del día.
- Si no coincide, devuelve una rotación vacía y el primer registro sustituye el historial anterior.
- La persistencia continúa fuera de Auto Backup y limitada a 24 hashes.

La clave de día utilizará el mismo calendario diario que los contadores técnicos de IA para que el reinicio sea coherente con el resto de límites.

### Selección de candidato

`AiMenuHiveRepository` mantendrá el filtrado seguro, la canonicalización y el colapso semántico actuales. Después seleccionará únicamente entre candidatos cuyo `semanticHash` no aparezca en el historial diario. Si no queda ninguno, devolverá un resultado distinguible de una colmena vacía o de un error técnico; no establecerá `startsNewRotationCycle` ni elegirá una receta repetida.

### Presentación

`MenuDadoViewModel` pasará la clave diaria al historial y, al recibir el resultado de agotamiento, elegirá el mensaje localizado según el próximo reintento permitido. Solo registrará una receta como vista después de confirmar que el detalle se mostró con el perfil y público todavía vigentes.

Los eventos existentes conservarán datos cerrados y no incluirán receta, perfil, UID ni texto libre. El agotamiento contará como `miss`; no se añade un evento nuevo.

## Cuotas y recompensas

Este cambio no modifica cuándo se autoriza o consume un uso gratuito o recompensado. Su alcance es impedir la repetición visible y proporcionar una explicación clara cuando el conjunto diario ya se agotó.

## Validación

- Dos o tres candidatos compatibles se muestran una sola vez cada uno durante el día.
- Una cuarta solicitud devuelve agotamiento sin repetir una receta.
- Un único candidato no se repite el mismo día.
- El cambio de día permite volver a recorrer el conjunto.
- Invitado y cuentas mantienen historiales independientes.
- Un fallo de lectura o escritura local conserva el comportamiento fail-safe existente.
- Al vencer `retryAtMillis`, el siguiente toque intenta Gemini antes de consultar la colmena.
- Los mensajes de agotamiento temporal y diario existen en español, inglés y francés.
- Los candidatos incompatibles nunca se muestran ni se registran.

## Riesgos

- Una colmena pequeña puede agotarse pronto, lo cual es intencional y se comunica al usuario.
- El historial está limitado a 24 hashes; este límite conserva el almacenamiento acotado existente. Para los conjuntos pequeños que motivan el cambio garantiza la no repetición diaria.
- Si el almacenamiento local falla, la rotación continuará de forma fail-safe y podría repetir una receta; la generación no debe bloquearse por un fallo de preferencias locales.
