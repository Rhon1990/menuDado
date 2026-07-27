# Diseño: respaldo colaborativo de ideas IA

## Objetivo

Mantener el dado IA animado y entregar una idea de menú compatible cuando una
petición real a Gemini falle por timeout, conectividad, respuesta inválida,
cuota del proveedor o indisponibilidad temporal. El respaldo reutiliza ideas IA
guardadas previamente por usuarios de MenuDado, sin mezclar ni exponer las
colecciones privadas bajo `users/{uid}`.

La solución debe:

- conservar el flujo actual de revisión antes de guardar;
- respetar tipo de comida, público y perfil alimentario;
- evitar menús duplicados o conceptualmente equivalentes en la colmena;
- no añadir servicios de pago, Cloud Functions, embeddings ni llamadas extra a
  Gemini;
- minimizar lecturas y escrituras de Firestore;
- mantener la contribución invisible en el flujo de guardado, pero explicada de
  forma general en la política de privacidad.

## Límite de la garantía

La aplicación ofrecerá un respaldo de mejor esfuerzo, no una garantía absoluta
en cualquier entorno. Una primera instalación sin conexión, una colección vacía
o un perfil excepcional sin candidato compatible pueden impedir el resultado.
MenuDado no relajará alergias, embarazo, veganismo, edad ni restricciones libres
para aparentar éxito.

El alcance no incluye un catálogo editorial empaquetado dentro del APK. Mantener
ese catálogo actualizado y cubrir todas las combinaciones posibles aumentaría
el alcance y no resolvería restricciones libres arbitrarias.

## Enfoques considerados

### 1. Colección Firestore separada desde el cliente — elegido

Usar una colección compartida con documentos anónimos, reglas específicas,
App Check y autenticación Firebase ya existentes. La app crea o amplía un
documento mediante un identificador semántico determinista y consulta un grupo
pequeño de candidatos compatibles cuando la IA falla.

Ventajas:

- reutiliza la infraestructura actual;
- no requiere un servicio nuevo ni una segunda llamada de IA;
- mantiene aislados los datos privados;
- permite deduplicación concurrente por ID de documento;
- puede aprovechar la caché local de Firestore.

Riesgos:

- consume parte de las cuotas de lectura, escritura y almacenamiento de
  Firestore;
- las reglas validan estructura y mutaciones, pero no pueden demostrar que una
  receta fue realmente generada por Gemini;
- App Check reduce abuso, pero no sustituye a un backend de confianza.

### 2. Cloud Functions

Una función recibiría las contribuciones, aplicaría deduplicación y devolvería
candidatos. Aporta un perímetro de servidor más claro, pero el repositorio no
tiene infraestructura Functions y su despliegue requiere el plan Blaze. Queda
descartado por coste y mantenimiento.

### 3. Catálogo exclusivamente local

Evita lecturas remotas durante el respaldo, pero no crea la colmena solicitada y
no puede cubrir perfiles arbitrarios sin aumentar mucho el APK. Queda
descartado.

## Arquitectura

### Responsabilidades

Se añadirá un contrato específico, separado de la sincronización privada:

- `AiMenuHiveDataSource`: lectura y escritura remota de documentos compartidos.
- `FirebaseAiMenuHiveDataSource`: implementación Firestore.
- `AiMenuHiveRepository`: normalización, huellas, compatibilidad,
  deduplicación, selección y caché.
- `MenuDadoViewModel`: decide cuándo intentar IA, cuándo activar el respaldo y
  cómo representar sus fases.
- Compose: solo representa la fase de carga y sigue abriendo el modal existente.

`MenuDadoRemoteDataSource` seguirá dedicado a `users/{uid}`. No se reutilizará
su colección privada para búsquedas cruzadas.

### Inyección

`MenuDadoApplication` construirá el repositorio de colmena y `MainActivity` lo
entregará al `MenuDadoViewModel`. Los tests usarán un fake en memoria.

## Modelo compartido

Colección:

```text
sharedAiMenus/{semanticHash}
```

Campos permitidos:

- `schemaVersion`: versión del contrato.
- `semanticHash`: copia inmutable del ID.
- `semanticKey`: clave canónica no visible.
- `language`: idioma del contenido.
- `name`, `description`, `notes`: respuesta IA sin edición posterior.
- `mealType`: desayuno, almuerzo o cena.
- `audience`: persona adulta, peques o bebé.
- `calories`, `healthStatus`, `healthReason`, `healthSuggestion`.
- `cuisineInspiration`.
- `shoppingProducts`: productos estructurados devueltos por la IA.
- `compatibilityKeys`: huellas anónimas de perfiles compatibles.
- `randomScore`: número estable derivado del hash para repartir resultados.
- `createdAt`, `updatedAt`: timestamps del servidor.

No se almacenarán:

- UID, correo, modo de autenticación o identificador del dispositivo;
- foto o URI local;
- ingredientes base escritos por el usuario;
- perfil alimentario legible;
- favoritos, fecha de última elección o productos marcados como comprados;
- nombre del creador ni procedencia de una cuenta concreta.

El idioma forma parte de la huella para que el respaldo siempre pueda mostrar
contenido en el idioma visible. Las variantes traducidas se consideran
contenidos distintos; no se intentará traducir durante una caída de IA.

## Deduplicación semántica

### Clave canónica

La respuesta de generación actual añadirá un único campo técnico:
`deduplication_key`. Se pedirá en inglés, con formato estable:

```text
<familia de plato>|<ingredientes principales>|<preparación>
```

Ejemplo:

```text
pasta|tomato|sauce
```

La instrucción agrupará variantes comunes: `spaghetti`, `macaroni` y `noodles`
de trigo dentro de `pasta` cuando la preparación y los ingredientes principales
sean equivalentes.

No se hará otra llamada a Gemini. El incremento de salida se limita a esa clave
corta dentro de la respuesta existente.

### Normalización local

Antes de calcular la huella:

- minúsculas y Unicode normalizado;
- espacios y puntuación colapsados;
- componentes ordenados cuando el orden no cambia el concepto;
- sinónimos frecuentes ES/EN/FR mapeados a familias controladas;
- combinación con los productos principales estructurados.

El ID será un SHA-256 de idioma y clave canónica normalizada. Si la IA omite o
devuelve una clave inválida, la idea podrá mostrarse y guardarse de forma
privada, pero no se aportará a la colmena. No se usará una huella débil que
pueda introducir duplicados.

### Escritura concurrente

Una transacción lee `sharedAiMenus/{semanticHash}`:

- si no existe, crea el documento saneado;
- si existe, conserva la receta original y solo puede incorporar con
  `arrayUnion` una nueva `compatibilityKey` válida;
- nunca crea un segundo documento con otro ID para el mismo hash;
- los fallos de contribución no bloquean ni revierten el guardado local.

La deduplicación semántica es determinista para claves equivalentes, pero no
puede garantizar que Gemini clasifique de forma idéntica todos los conceptos
posibles. No se añadirán embeddings ni comparaciones con una segunda llamada
porque incumplirían el requisito de coste.

## Compatibilidad alimentaria

### Huella del perfil

`compatibilityKey` será una huella SHA-256 local creada a partir de:

- público;
- franja de edad normalizada;
- embarazo;
- veganismo;
- alérgenos ordenados;
- restricción libre normalizada;
- versión del algoritmo.

No se guarda el perfil que originó la huella. La consulta exige coincidencia
exacta; no se intentará inferir compatibilidad entre perfiles distintos.

### Filtros obligatorios

Un candidato debe cumplir:

- mismo idioma;
- mismo tipo de comida;
- mismo público;
- presencia de la `compatibilityKey` exacta;
- estructura completa y versión soportada;
- validación local adicional del texto y productos frente al perfil actual;
- no coincidir con la idea que acaba de fallar ni con hashes recientes mostrados
  al usuario, siempre que exista otra alternativa.

Los ingredientes base son opcionales en el formulario. Primero se priorizarán
candidatos que contengan alguno de ellos; si no hay ninguno, podrán ignorarse
sin relajar el perfil alimentario.

Si todos los candidatos compatibles ya se mostraron, se elegirá el menos
reciente para mantener el respaldo. Esto no crea duplicados en Firestore.

## Qué ideas alimentan la colmena

Solo se aporta una idea cuando:

- proviene de una generación IA válida;
- el usuario pulsa `Guardar en mis menús`;
- nombre, descripción, notas, tipo y público no fueron editados después de la
  generación;
- conserva análisis, productos y clave semántica coherentes.

Los menús manuales, los análisis posteriores de menús manuales y las recetas IA
editadas no se comparten. Una idea recuperada de la propia colmena tampoco crea
un documento nuevo; su hash ya existe.

El guardado seguirá siendo local-first. La contribución se ejecutará después,
en segundo plano y sin retrasar el Snackbar de éxito.

## Flujo de respaldo

### Petición normal

1. Validar tipo, público, conflicto de ingredientes, throttle y límites actuales.
2. Bloquear dobles acciones y mostrar el dado 3D animado.
3. Lanzar la única petición actual a Gemini con timeout de 45 segundos.
4. Tras 12 segundos sin resultado, mantener la animación y cambiar el mensaje a:
   `La IA está tardando más de lo habitual. Seguimos buscando una idea para ti.`
5. Si Gemini responde correctamente, abrir el modal actual y no consultar la
   colmena.

### Fallo real de IA

1. Ante timeout, conectividad, respuesta inválida, cuota del proveedor o fallo
   temporal, cambiar la fase interna a `SearchingHive`.
2. Mantener el mismo dado animado; no mostrar primero el error actual.
3. Consultar como máximo un lote pequeño de candidatos compatibles y elegir uno
   localmente.
4. Si no responde el servidor, repetir la selección desde la caché Firestore.
5. Si hay candidato, rellenar el mismo borrador y abrir el mismo modal. Sigue
   siendo una idea generada con IA, aunque se generara anteriormente.
6. Si no hay candidato seguro, detener el dado y mostrar el error localizado
   actual. No mostrar una receta incompatible.

El respaldo no se activa cuando faltan datos obligatorios, existe un conflicto
alimentario local, se detecta doble tap/throttle o el usuario alcanzó un límite
de producto local. Esos estados son reglas deliberadas, no fallos de Gemini.

## Estado y UI

Se sustituirá el booleano conceptual único por una fase explícita, manteniendo
compatibilidad con la UI actual:

- `Idle`
- `Generating`
- `GeneratingSlow`
- `SearchingHive`

El dado continuará animado en las tres fases activas. Los textos serán recursos
localizados ES/EN/FR. No se mencionará Firestore, la colmena ni otros usuarios.

No se añadirá un badge distinto al resultado recuperado. El usuario conserva
las mismas acciones: guardar, probar otra idea y descartar.

## Coste

La implementación no añadirá:

- Cloud Functions;
- plan de pago nuevo;
- embeddings ni búsqueda vectorial;
- otra llamada a Gemini;
- imágenes generadas;
- servicios externos.

Cada contribución válida necesita como máximo una transacción sobre un documento
compartido. El respaldo consulta como máximo 12 documentos y solo después de un
fallo real; no se precarga en cada apertura ni se escucha la colección en tiempo
real.

Firestore ofrece una cuota gratuita diaria, pero no existe garantía de coste
operativo cero si el volumen supera esa cuota. La función tendrá un interruptor
de Remote Config reutilizando la infraestructura actual para poder desactivar
lecturas y contribuciones sin publicar una nueva versión si el consumo crece.

A fecha de este diseño, la cuota gratuita oficial incluye 50.000 lecturas y
20.000 escrituras de documentos al día y 1 GiB almacenado. No se solicitará un
cambio de plan Firebase. Si el proyecto ya está en Blaze, las operaciones que
superen la cuota gratuita pueden facturarse; si está en Spark, el exceso puede
dejar la función temporalmente sin servicio. El consumo debe revisarse en el
panel de Firestore antes y después del lanzamiento:
https://firebase.google.com/docs/firestore/quotas

## Seguridad y privacidad

Las reglas mantendrán intacto:

```text
users/{uid}/...
```

Para `sharedAiMenus`:

- lectura solo con Firebase Auth activa, incluida sesión anónima;
- creación y ampliación de compatibilidad solo con Auth y App Check en los
  entornos donde se aplique enforcement;
- lista cerrada de claves, tipos, tamaños máximos y enums;
- `semanticHash`, contenido y timestamps originales inmutables tras creación;
- actualización limitada a compatibilidades válidas y timestamp;
- eliminación desde cliente denegada.

`docs/privacy-policy.md` explicará que MenuDado puede reutilizar de forma
anónima propuestas IA guardadas para ofrecer respaldo a otros usuarios. No
habrá checkbox ni aviso durante el guardado, pero la práctica no quedará oculta
en la documentación legal.

## Telemetría

Se ampliará el evento de finalización sin contenido personal:

- origen `live_ai` o `hive_fallback`;
- resultado de consulta `hit`, `cache_hit`, `miss` o `error`;
- duración por fase;
- tipo de fallo que activó el respaldo.

No se enviarán nombres, ingredientes, hashes, restricciones ni IDs de documentos
a Analytics.

## Estrategia de pruebas

### Unitarias

- normalización y hash estable para equivalentes como `pasta con tomate` y
  `espaguetis en salsa de tomate`;
- separación de recetas realmente diferentes;
- clave ausente o inválida no contribuye;
- coincidencia exacta del perfil y rechazo de incompatibles;
- prioridad opcional de ingredientes base;
- selección sin repetición reciente;
- transición `Generating -> GeneratingSlow -> SearchingHive`;
- éxito IA evita lecturas de colmena;
- timeout, red, cuota de proveedor y JSON inválido activan respaldo;
- validaciones, throttle y límites locales no activan respaldo;
- hit remoto, hit de caché, miss y error;
- guardado editado o manual nunca se comparte;
- contribución fallida no revierte el guardado local.

### Firestore y reglas

- un usuario autenticado puede leer candidatos;
- no puede leer menús privados de otro UID;
- no se aceptan campos personales o desconocidos;
- el contenido compartido no puede sobrescribirse;
- la misma huella no crea otro documento;
- un cliente no puede borrar documentos compartidos.

### Verificación Android

- tests unitarios del módulo;
- compilación Kotlin;
- lint de release;
- build de debug o release según disponibilidad;
- `git diff --check`;
- paridad de recursos ES/EN/FR.

### QA manual guiada

- IA rápida: texto y modal actuales, sin consulta de respaldo;
- IA lenta: el dado no se detiene y el texto cambia;
- timeout/red/respuesta inválida: aparece un candidato compatible;
- candidato en caché sin conexión;
- ningún candidato: error honesto, sin mezclar perfiles;
- guardar idea sin editar: contribución no bloqueante;
- editar antes de guardar: no contribuye;
- dos nombres equivalentes: un solo documento;
- perfiles adulto, peques, bebé, embarazo, veganismo y cada alérgeno;
- verificar consumo de Firestore y el interruptor remoto.

## Archivos previstos

El plan de implementación concretará rutas exactas, pero el alcance esperado es:

- dominio/parser/prompt de generación;
- nuevo contrato y repositorio de colmena;
- implementación Firestore e inyección;
- `MenuDadoViewModel` y overlay de carga;
- Room/modelo solo si hace falta conservar la clave IA hasta el guardado;
- reglas e índices Firestore;
- strings ES/EN/FR;
- tests unitarios y de reglas;
- `docs/project-context.md` y `docs/privacy-policy.md`.

No se incluye despliegue automático de reglas ni Remote Config en producción.
Esos cambios externos requerirán verificación explícita del proyecto Firebase.
