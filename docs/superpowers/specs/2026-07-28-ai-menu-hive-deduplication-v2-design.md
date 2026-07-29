# Deduplicación v2 de la colmena de menús IA

## Objetivo

Impedir que `sharedAiMenus` almacene varias recetas culinariamente equivalentes
cuando Gemini expresa la misma clave técnica con pequeñas variaciones, y
consolidar de forma segura los duplicados ya existentes.

Las tres variantes observadas deben converger en una sola identidad:

```text
salad|lentils+vegetables|mix
salad|lentils+vegetable|mixed
salad|lentils+vegetables|mixed
```

La solución no añadirá llamadas a IA, campos a la respuesta JSON ni longitud al
prompt actual.

## Causa confirmada

El SHA-256 actual es determinista y funciona correctamente. El problema ocurre
antes de calcularlo: `AiMenuHiveIdentity` normaliza algunos sinónimos, pero no
unifica variantes como `vegetable`/`vegetables` o `mix`/`mixed`. Por tanto,
Gemini puede describir el mismo concepto con claves canónicas distintas y cada
una produce un documento diferente.

La deduplicación por ID de Firestore solo puede ser efectiva si la identidad
local que recibe es realmente canónica.

## Alternativas consideradas

### 1. Añadir únicamente los alias observados

Es el cambio más pequeño, pero solo corrige estas capturas. Otras formas
equivalentes volverían a crear documentos distintos.

### 2. Canonicalización local controlada y versionada

Es la opción elegida. Usa vocabularios separados por posición de la clave,
refuerza de forma compacta el contrato ya existente y migra los datos actuales.
Mantiene el coste de IA y la arquitectura actuales.

### 3. Similitud mediante embeddings o una segunda evaluación de IA

Detectaría equivalencias más difusas, pero añade infraestructura, latencia,
coste, posibles falsos positivos y otra llamada remota. Queda fuera de alcance.

## Diseño

### Identidad canónica v2

`AiMenuHiveIdentity` seguirá aceptando exactamente:

```text
<familia>|<ingredientes principales>|<preparación>
```

La normalización será local, determinista y específica para cada componente:

- familia: agrupa nombres equivalentes de familias de plato;
- ingredientes: normaliza variantes controladas, elimina duplicados y ordena;
- preparación: agrupa formas gramaticales y técnicas equivalentes.

No se aplicará stemming genérico ni se eliminará cualquier `s` final. Esas
reglas podrían convertir palabras distintas en la misma identidad. Se usarán
alias explícitos y conservadores, por ejemplo:

```text
vegetable, vegetables -> vegetables
lentil, lentils, lenteja, lentejas -> lentils
mix, mixing, mixed, toss, tossed -> mixed
```

Con ello, las tres claves observadas producirán:

```text
salad|lentils+vegetables|mixed
```

y el mismo hash para un mismo idioma.

Las diferencias culinarias relevantes se conservarán. Por ejemplo:

- `salad|lentils+vegetables|mixed` y
  `stew|lentils+vegetables|stewed` seguirán siendo distintas;
- lentejas y garbanzos no se agruparán;
- los idiomas seguirán formando parte del hash.

### Defensa en lectura

Los documentos recuperados se volverán a canonicalizar localmente antes de
seleccionar una alternativa. Los candidatos con la misma identidad v2 se
agruparán y la rotación utilizará el hash v2, aunque el documento antiguo tenga
otro ID.

Así, una instalación actualizada no mostrará sucesivamente duplicados antiguos
mientras se completa o verifica la migración.

### Escritura y concurrencia

Las nuevas contribuciones calcularán siempre la identidad v2 antes de llegar a
Firestore. El documento seguirá usando el hash canónico como ID y la transacción
actual conservará una sola receta, incorporando únicamente nuevas
`eligibilityKeys`.

Se añadirá `identityVersion: 2` al documento, manteniendo `schemaVersion: 1`
para que versiones anteriores de la app puedan seguir leyendo el contenido. Las
reglas permitirán nuevas creaciones únicamente con `identityVersion == 2`.
Las contribuciones de versiones antiguas podrán fallar de forma silenciosa y no
bloqueante, igual que cualquier fallo actual de la colmena; guardar el menú
privado seguirá funcionando.

### Contrato del prompt sin coste adicional

El prompt no crecerá. Se sustituirán las instrucciones actuales de
`deduplication_key` por una redacción más corta que:

- mantenga exactamente el mismo campo y formato;
- indique nombres canónicos para los casos principales;
- pida una forma canónica de preparación;
- no añada ejemplos de salida ni campos nuevos.

La canonicalización local será la fuente de verdad. El prompt solo reducirá la
variabilidad de entrada y no será necesario para garantizar la deduplicación.

Se verificará con una entrada fija que:

- `generateMenu` continúa haciendo una única llamada;
- el objeto JSON conserva exactamente los campos actuales;
- el prompt completo tiene igual o menor número de caracteres y palabras que
  el prompt anterior;
- el parser mantiene compatibilidad con respuestas existentes.

No se usará `countTokens`, otra consulta a Gemini ni ninguna API adicional para
esta comprobación.

## Consolidación de Firebase

Se preparará una utilidad administrativa de una sola ejecución con este flujo:

1. leer `sharedAiMenus` del proyecto indicado explícitamente;
2. guardar un backup JSON local antes de cualquier escritura;
3. recalcular la identidad v2 de cada documento;
4. mostrar en modo `dry-run` los grupos, documentos conservados y documentos
   candidatos a eliminación;
5. conservar como representante el documento más antiguo válido;
6. unir todas las `eligibilityKeys` sin duplicados;
7. crear o actualizar el documento con ID v2 e `identityVersion: 2`;
8. releer y validar el documento consolidado;
9. eliminar las copias solo después de esa validación.

El modo por defecto será `dry-run`. El modo de aplicación exigirá el ID exacto
del proyecto y una opción explícita. Las credenciales y el backup no se
guardarán en Git.

La utilidad tendrá fixtures compartidos con los tests Kotlin para comprobar que
la implementación administrativa y la app producen las mismas identidades en
los casos soportados.

## Compatibilidad y despliegue

El orden seguro será:

1. implementar y validar la identidad v2, la lectura defensiva y las reglas;
2. ejecutar la migración primero en `dry-run`;
3. crear el backup y aplicar la consolidación;
4. desplegar reglas que exijan identidad v2 en nuevas contribuciones;
5. publicar la app actualizada.

Las versiones antiguas podrán leer los documentos consolidados porque
`schemaVersion` no cambia y el mapper actual ignora campos adicionales.

## Archivos previstos

- `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- `app/src/main/java/com/menudado/backend/AiMenuHiveDataSource.kt`
- `app/src/main/java/com/menudado/ai/MenuGenerationPrompt.kt`
- tests unitarios correspondientes de dominio, repositorio, mapper y prompt
- `firestore.rules`
- `qa/firestore-rules.test.mjs`
- utilidad y tests de migración bajo `qa/`
- `docs/project-context.md`

No se modificará el flujo visual, la cuota, el timeout, la rotación culinaria ni
el guardado privado de menús.

## Estrategia QA

### Pruebas automatizadas

- Las tres claves de las capturas producen identidad y hash idénticos.
- Variaciones admitidas de familia, ingrediente y preparación convergen.
- Ingredientes o técnicas realmente distintos no colisionan.
- Dos documentos antiguos equivalentes cuentan como un solo candidato.
- La rotación recuerda el hash v2 y no repite otra variante antigua.
- El mapper escribe `identityVersion: 2` y sigue leyendo documentos anteriores.
- Las reglas aceptan documentos v2 válidos y rechazan nuevas identidades
  antiguas o inconsistentes.
- El prompt no aumenta su tamaño ni cambia el contrato de respuesta.
- La migración agrupa, fusiona y conserva correctamente en fixtures locales.

### Verificación técnica

- tests unitarios dirigidos;
- suite unitaria de la app;
- tests de reglas con el emulador de Firestore;
- compilación Kotlin de la variante de desarrollo;
- `dry-run` contra Firebase y revisión del informe antes de aplicar;
- comprobación posterior de que las tres variantes observadas dejan un único
  documento canónico.

## Riesgos y mitigaciones

- **Falso positivo:** alias conservadores y específicos por componente; tests
  negativos para recetas distintas.
- **Nueva variante no contemplada:** el prompt reduce variabilidad y los alias
  se pueden ampliar sin cambiar el contrato.
- **Pérdida durante la limpieza:** backup obligatorio, `dry-run`, escritura y
  relectura antes de borrar.
- **Cliente antiguo que vuelve a contribuir:** las reglas bloquean nuevas
  identidades sin versión 2 sin afectar el menú privado del usuario.
- **Divergencia entre migración y Android:** fixtures de paridad compartidos.

## Criterios de aceptación

- Las tres recetas mostradas comparten una única identidad v2.
- No se crea más de un documento para variantes equivalentes bajo concurrencia.
- Los duplicados antiguos no se sirven como ideas distintas.
- Firebase queda consolidado sin perder `eligibilityKeys`.
- No aumenta el número de llamadas a IA.
- El prompt y la respuesta no aumentan el coste de tokens respecto al flujo
  actual.
- Recetas realmente distintas continúan almacenándose por separado.
