# Diseño: compatibilidad amplia de perfiles en la colmena IA

## Objetivo

Permitir que una receta IA válida guardada sin editar pueda reutilizarse por
otros perfiles alimentarios compatibles, sin mezclar públicos de edades
distintas y sin relajar ninguna restricción de seguridad.

Una receta creada para `Adulto`, `Peques` o `Bebé` permanecerá siempre dentro
de ese público. Veganismo, embarazo, alergias y alimentos a evitar se
comprobarán al consultar la colmena, usando el perfil vigente.

El guardado privado continuará siendo local-first y no dependerá del resultado
de la contribución compartida.

## Problema actual

La colmena indexa cada receta mediante una `eligibilityKey` que combina idioma,
tipo de comida, público y el perfil alimentario completo. La consulta exige esa
huella exacta antes de ejecutar la validación local.

Esto provoca que una receta apta para varios perfiles solo sea visible para la
combinación exacta con la que se guardó. Por ejemplo, una receta vegana
registrada desde un perfil vegano no puede encontrarse desde un perfil adulto
sin restricciones, aunque también sea compatible.

Además, `MenuDadoViewModel` lanza la contribución en segundo plano e ignora su
`Result`. El menú privado puede guardarse correctamente mientras una
contribución omitida o fallida queda sin evidencia diagnóstica. En el caso
investigado se confirmó el guardado local, la colmena habilitada y el perfil
adulto vegano, pero la receta no apareció en `sharedAiMenus`; el contrato actual
no permite reconstruir después si la contribución fue omitida o falló.

## Alternativas consideradas

### 1. Mantener la huella exacta de perfil

Es el menor cambio, pero conserva el problema: perfiles distintos no pueden
reutilizar una receta compatible. Queda descartado.

### 2. Eliminar también la separación por público

Maximiza la reutilización, pero permitiría consultar para bebés o peques una
receta creada para adultos. Aunque exista validación local, amplía
innecesariamente el riesgo ante textos ambiguos. Queda descartado.

### 3. Ámbito estable más validación local estricta — elegido

Se añadirá una huella de ámbito que combine únicamente:

- versión del ámbito;
- idioma;
- tipo de comida;
- público (`Adulto`, `Peques` o `Bebé`).

La consulta recuperará recetas del mismo ámbito y aplicará después el contrato
local completo de perfil. Así se reutilizan recetas entre perfiles compatibles
sin cruzar edades, idiomas ni momentos de comida.

## Modelo de datos

`sharedAiMenus/{semanticHash}` conservará la identidad semántica actual y
añadirá:

```text
scopeKeys: [sha256(version|language|mealType|audience)]
```

Se usa una lista porque una misma receta semántica podría validarse y guardarse
legítimamente para más de un tipo de comida o público en momentos distintos.
Una transacción incorporará como máximo una nueva `scopeKey` mediante
`arrayUnion`.

`eligibilityKeys` se conservará durante la transición para que las versiones
publicadas y los documentos existentes sigan funcionando. No se almacenarán
valores legibles del perfil, UID, ingredientes libres ni identificadores del
dispositivo.

Los documentos existentes sin `scopeKeys` seguirán siendo válidos. No es
posible reconstruir su ámbito desde la `eligibilityKey`, porque es una huella
irreversible; se utilizará una consulta heredada como respaldo hasta que esos
documentos reciban una nueva contribución compatible.

## Flujo de contribución

Solo se contribuirá cuando se mantengan las condiciones actuales:

- propuesta procedente de IA en vivo;
- guardada sin editar;
- identidad semántica, calorías y análisis válidos;
- compatibilidad confirmada con el perfil y público que originaron la receta.

La compatibilidad del perfil de origen seguirá siendo una barrera de entrada:
una receta detectada como incompatible no se compartirá.

Al contribuir:

1. se valida de nuevo la receta con el perfil y público vigentes;
2. se calcula la identidad semántica v2;
3. se calcula la `scopeKey` sin restricciones alimentarias;
4. se crea el documento o se añade el ámbito al documento existente;
5. el guardado privado no se bloquea ni se revierte si la colmena falla.

Una receta recuperada como respaldo no vuelve a contribuir.

## Flujo de consulta

Ante un fallo real de la IA:

1. consultar por la `scopeKey` exacta de idioma, comida y público;
2. canonicalizar y deduplicar los documentos recuperados;
3. validar cada candidato con `DietaryProfile.accepts`, incluyendo edad,
   embarazo, veganismo, alérgenos y alimentos libres a evitar;
4. descartar cualquier candidato dudoso o incompatible;
5. aplicar la rotación local y la preferencia por ingredientes solicitados;
6. si no existe candidato nuevo compatible, consultar la `eligibilityKey`
   heredada para conservar acceso a documentos antiguos;
7. si tampoco existe coincidencia, devolver `miss` y mantener el error original.

La consulta amplia estará acotada a un máximo de 36 documentos. La colmena solo
se consulta cuando la generación real falla, por lo que este límite prioriza la
probabilidad de encontrar un candidato seguro sin introducir lecturas
ilimitadas. No se harán llamadas adicionales a Gemini.

### Separación de públicos

La `scopeKey` hace imposible que una consulta de `Bebé` recupere una receta
registrada únicamente para `Adulto`. Una receta solo añade otro público a
`scopeKeys` cuando una nueva generación para ese público supera su validación
específica.

La validación local sigue siendo obligatoria aunque el ámbito coincida. La
clave limita el conjunto remoto; no sustituye la barrera de seguridad.

## Observabilidad

`AiMenuHiveGateway.contribute` devolverá un resultado explícito que distinga:

- `saved`: la contribución se creó o actualizó correctamente;
- `skipped_disabled`: la colmena estaba desactivada;
- `skipped_incompatible`: la receta no superó la validación;
- `skipped_invalid_identity`: faltaba una identidad semántica válida;
- `error`: la escritura remota falló.

`MenuDadoViewModel` registrará un único evento técnico terminal,
`ai_menu_hive_contribution`, con `result` y una causa cerrada cuando aplique.
No incluirá perfil, público, nombre, descripción, ingredientes, UID ni texto
libre.

El evento no mostrará mensajes adicionales al usuario: `Guardar en mis menús`
seguirá confirmando exclusivamente el guardado privado. La finalidad es detectar
fallos de la colmena sin presentar como fallido un menú que sí se conservó.

## Reglas y compatibilidad

Las reglas de Firestore:

- aceptarán `scopeKeys` únicamente como lista de hashes SHA-256;
- exigirán exactamente una `scopeKey` al crear documentos nuevos;
- permitirán añadir como máximo una nueva `scopeKey` por actualización;
- impedirán eliminar ámbitos existentes o modificar la receta compartida;
- seguirán aceptando lecturas autenticadas de documentos heredados;
- mantendrán la prohibición de borrado desde clientes.

El mapper podrá leer documentos sin `scopeKeys`, pero toda creación nueva
escribirá el campo. No se cambiará la colección ni la identidad del documento.

## Archivos previstos

- `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`
- `app/src/main/java/com/menudado/ui/MenuDadoViewModel.kt`
- contratos e implementación de Analytics
- tests unitarios correspondientes
- `firestore.rules`
- `qa/firestore-rules.test.mjs`
- `docs/project-context.md`

No se modificará la interfaz visual, la generación IA, el prompt, las cuotas,
el guardado privado ni la rotación local.

## Estrategia QA

### Repositorio y dominio

- Dos perfiles adultos distintos producen la misma `scopeKey`.
- Adulto, peques y bebé producen ámbitos diferentes.
- Idioma o tipo de comida distintos producen ámbitos diferentes.
- Una receta vegana válida puede recuperarse para un adulto sin restricciones.
- Una receta con lácteos se descarta para un perfil vegano o alérgico.
- Una receta adulta nunca aparece en una consulta de bebé.
- Una restricción libre incompatible produce `miss`.
- La consulta heredada se usa solo cuando la consulta amplia no encuentra un
  candidato seguro.
- La rotación y la deduplicación v2 conservan su comportamiento.

### Contribución y observabilidad

- Una propuesta IA intacta escribe `scopeKeys` y conserva `eligibilityKeys`.
- Una receta incompatible no se escribe.
- Una identidad inválida no se escribe.
- Colmena desactivada y error remoto producen resultados distintos.
- Cada intento registra un único resultado terminal sin datos sensibles.
- Un fallo de colmena no revierte ni retrasa el guardado privado.

### Firestore

- Las reglas aceptan una creación válida con una sola `scopeKey`.
- Rechazan ámbitos sin hash, múltiples ámbitos iniciales o campos extra.
- Permiten añadir un ámbito y rechazan eliminar o reemplazar los existentes.
- Los documentos heredados continúan siendo legibles.

### Verificación final

- tests unitarios dirigidos;
- suite unitaria de la app;
- tests de reglas con el emulador;
- compilación de la variante debug;
- prueba manual guiada con perfiles adultos vegano y sin restricciones;
- prueba negativa separada para bebé;
- verificación en el proyecto Firebase debug de que la contribución existe y
  contiene únicamente metadatos anónimos permitidos.

## Riesgos y mitigaciones

- **Más lecturas por fallback:** límite remoto de 36 y uso exclusivo tras un
  fallo real de IA.
- **Candidato incompatible:** validación local conservadora antes de mostrar.
- **Mezcla de edades:** ámbito remoto estricto por público y segunda validación.
- **Datos heredados sin ámbito:** consulta exacta de compatibilidad como
  respaldo temporal.
- **Fallo remoto invisible:** resultado tipado y Analytics terminal.
- **Abuso de escritura:** autenticación, App Check, reglas de forma e
  inmutabilidad del contenido existente.

