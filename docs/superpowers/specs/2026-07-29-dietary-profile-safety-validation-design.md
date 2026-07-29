# Validación de seguridad del perfil alimentario

## Objetivo

Impedir que MenuDado muestre, guarde o comparta una idea generada que contradiga
el público o el perfil alimentario seleccionado. La solución debe cubrir la
respuesta en vivo de Firebase AI Logic y los candidatos de `sharedAiMenus`, sin
añadir otra llamada a IA ni cambiar Firebase, el modelo o el esquema persistido.

El cumplimiento significa que la app falla de forma conservadora cuando detecta
una incompatibilidad o no puede confirmar los campos mínimos de seguridad. No
significa certificación médica: las condiciones de salud escritas libremente
siguen siendo orientación para la generación y deben conservar el aviso de
consulta profesional.

## Enfoques considerados

### Solo reforzar el prompt

Es el cambio más pequeño, pero mantiene a Gemini como única barrera. Una respuesta
que ignore las instrucciones seguiría mostrándose. Se descarta como solución
principal.

### Segunda llamada de IA para revisar la primera

Permite una revisión semántica adicional, pero duplica coste y latencia y sigue
dependiendo de una respuesta probabilística. También contradice el objetivo del
proyecto de resolver generación y evaluación en una sola llamada. Se descarta.

### Defensa local en profundidad

Se mantiene el prompt como primera barrera y se incorpora una validación local
única antes de exponer el resultado. Es determinista, reutilizable, no consume
tokens adicionales y puede aplicarse otra vez al guardar. Este es el enfoque
seleccionado.

## Arquitectura

### Contrato de validación

`DietaryProfileCompatibility.kt` evolucionará desde un booleano basado solo en
ingredientes a un contrato que evalúe:

- `GeneratedMenu`
- `MenuAudience`
- `DietaryProfile`

El resultado distinguirá entre compatible e incompatible y devolverá categorías
técnicas cerradas, nunca el texto del perfil:

- incompatibilidad vegana;
- alérgeno detectado;
- alimento explícitamente evitado;
- riesgo para bebé;
- riesgo para peques;
- riesgo durante el embarazo.

Las categorías sirven para pruebas y control de flujo. No se enviarán a Analytics
con ingredientes, alérgenos concretos ni condiciones de salud.

### Reglas deterministas

Las coincidencias se normalizarán sin tildes y con límites de palabra. Las listas
incluirán español, inglés y francés porque son los idiomas soportados por la app.

- Veganismo: carne, aves, pescado, marisco, huevo, lácteos, miel, gelatina y
  derivados comunes.
- Alérgenos: ampliar sinónimos y derivados de gluten, lácteos, huevo, frutos
  secos, cacahuete, soja, pescado, marisco y sésamo.
- Evitaciones: solo se tratarán como ingredientes concretos las entradas
  separadas por coma, punto y coma o salto de línea. Las palabras que representen
  condiciones clínicas no se buscarán literalmente dentro de la receta.
- Bebé: rechazar miel, alcohol, sal o azúcar añadidas, edulcorantes, frutos secos
  enteros, palomitas, uvas o tomates enteros, piezas duras o redondas, trozos
  grandes de carne o queso, salchichas, huesos y preparaciones crudas o poco
  cocinadas. Al abarcar `6-24 meses`, se aplicará el criterio conservador seguro
  para la parte más joven del rango.
- Peques: rechazar alcohol y riesgos evidentes de atragantamiento para la parte
  más joven del rango `2-12 años`.
- Embarazo: rechazar alcohol, leche o queso no pasteurizado, carne, pescado o
  huevo crudo/poco cocinado y especies de pescado de alto mercurio ya recogidas
  en el prompt.

Para evitar falsos positivos, términos ambiguos como `crema`, `harina` o `pasta`
no bastarán por sí solos para declarar lácteos o gluten cuando el texto indique
explícitamente una alternativa compatible, como `crema vegetal`, `harina de
garbanzo` o `pasta sin gluten`.

### Flujo de generación en vivo

1. El `ViewModel` valida los ingredientes base antes de consumir cuota, como
   ahora.
2. Firebase AI Logic genera y parsea una única respuesta.
3. Antes de copiarla al estado visible, el `ViewModel` ejecuta el validador con
   el público y la instantánea del perfil usados en esa petición.
4. Si cumple, se muestra normalmente.
5. Si no cumple, la receta nunca llega al modal y se intenta el respaldo
   compatible existente.
6. Si tampoco existe respaldo seguro, se muestra un aviso localizado que indica
   que no se encontró una idea compatible con el perfil. No se enumeran datos
   sensibles ni detalles internos.

Una respuesta rechazada no desencadena otra llamada a Gemini y no avanza la
rotación culinaria ni el historial de ideas.

### Respaldo compartido

`AiMenuHiveRepository` usará exactamente el mismo contrato con público y perfil.
La clave de elegibilidad seguirá siendo una preselección; el contenido real de
cada candidato será la autoridad final antes de devolverlo.

### Defensa al guardar

`saveGeneratedMenuIdea()` validará otra vez el contenido visible contra el público
y el perfil actuales. Así se cubre un cambio o sincronización de perfil mientras
el modal está abierto. Una idea incompatible no se guarda en Room, no se sube al
documento privado y no contribuye a `sharedAiMenus`.

Los menús escritos manualmente seguirán siendo responsabilidad del usuario y no
se bloquearán, conforme al contrato existente del proyecto.

## Condiciones de salud

Diabetes, hipertensión u otras condiciones libres no pueden certificarse a
partir de texto y calorías estimadas por IA. La solución:

- mantiene esas indicaciones en el prompt;
- evita confundir el nombre de la condición con un alimento prohibido;
- conserva el aviso sanitario de que MenuDado es informativo;
- cambia el texto del perfil para pedir alimentos concretos a evitar cuando el
  usuario necesite una exclusión estricta;
- no presenta la etiqueta `Saludable` como garantía médica.

No se incorporan umbrales clínicos improvisados de azúcar, sodio, carbohidratos o
calorías. Una futura certificación de condiciones requeriría ingredientes y
nutrientes estructurados, una fuente nutricional fiable y criterios revisados por
profesionales sanitarios.

## Mensajería

Se añadirá un mensaje localizado equivalente a:

`La idea no cumplía tu perfil alimentario y no la mostramos. No encontramos una alternativa segura ahora; inténtalo más tarde.`

El mensaje no incluirá alergias, embarazo, condiciones, ingredientes del usuario,
Firestore ni el origen de la receta.

El campo libre del perfil se describirá como `Alimentos a evitar o indicaciones`
y usará ejemplos concretos como `sin picante, sin champiñones`. La ayuda indicará
que las condiciones de salud orientan a la IA pero necesitan revisión profesional.

## Pruebas

### Dominio

- acepta una receta compatible;
- rechaza cada familia vegana y cada alérgeno soportado;
- admite sustitutos explícitos compatibles;
- rechaza miel, sal añadida y riesgos de atragantamiento para bebé;
- rechaza riesgos infantiles para peques;
- rechaza alcohol, crudos, no pasteurizados y pescado de alto mercurio durante
  el embarazo;
- respeta alimentos explícitos a evitar;
- no interpreta `diabético` o `hipertenso` como nombres de ingredientes.

### Flujo

- una respuesta en vivo incompatible no abre el modal;
- una respuesta rechazada intenta una sola vez el respaldo sin otra llamada de
  IA;
- un respaldo incompatible no se devuelve;
- una idea compatible conserva el flujo actual;
- un cambio de perfil antes de guardar bloquea la persistencia y la contribución;
- los mensajes permanecen localizados y no exponen datos del perfil.

### Regresión

- ejecutar todos los tests unitarios de `app`;
- compilar Kotlin debug;
- verificar que no se modifica Firebase, Remote Config ni el esquema Room;
- revisar que `docs/project-context.md` documenta la nueva barrera y la
  limitación de las condiciones clínicas.

## Criterios de aceptación

1. Ninguna idea generada incompatible de forma detectable se muestra, guarda o
   comparte.
2. Generación en vivo y respaldo usan el mismo validador.
3. Público, embarazo, veganismo, alérgenos y evitaciones concretas participan en
   la decisión.
4. La solución no añade llamadas ni tokens de IA y no relaja el perfil.
5. Las condiciones clínicas no se presentan como certificadas.
6. Todas las nuevas reglas tienen tests adversos y la suite completa permanece
   verde.
7. No se registra ni muestra información sensible del perfil en mensajes o
   Analytics.
