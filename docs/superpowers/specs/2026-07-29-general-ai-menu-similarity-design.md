# Deduplicación general de recetas similares en la colmena IA

## Objetivo

Evitar que `sharedAiMenus` trate como alternativas distintas recetas que
representan el mismo plato aunque Gemini cambie el idioma, el orden, la posición
de un concepto, el nombre comercial o un ingrediente secundario.

El caso reportado queda como regresión obligatoria:

```text
toast|avocado|egg
tostada|aguacate|huevo
```

Ambas claves deben producir una única identidad de rotación y un único candidato
visible. La solución no añade llamadas a IA, embeddings, campos al JSON ni
lecturas de Firestore.

## Causa confirmada

El hash SHA-256 funciona correctamente, pero se calcula después de una
canonicalización limitada. Las claves reportadas contienen los mismos conceptos
culinarios en idiomas distintos y no existen reglas para hacerlos converger.
Como consecuencia, se crean dos IDs válidos y la rotación los considera menús
independientes.

Confiar únicamente en que Gemini respete la instrucción de escribir la clave en
inglés no es suficiente: las capturas prueban que el proveedor puede devolver
una clave estructuralmente válida en otro idioma.

## Alternativas

### Alias exclusivos para la receta reportada

Corrige las dos tostadas, pero no crea una defensa reutilizable. Se descarta.

### Identidad conceptual determinista y similitud conservadora

Es la opción elegida. Separa la normalización culinaria de la generación,
compara conceptos en vez de texto visible y aplica un umbral alto sobre los
doce candidatos ya descargados.

### Comparación textual, embeddings o una segunda llamada a IA

Puede detectar semejanzas subjetivas, pero añade coste, latencia, dependencia
remota y falsos positivos difíciles de explicar. Queda fuera de alcance.

## Diseño

### Normalizador culinario reutilizable

La identidad local mantendrá el formato existente:

```text
<familia>|<ingredientes principales>|<preparación>
```

Un normalizador determinista convertirá cada componente en conceptos canónicos:

- elimina acentos, puntuación, conectores y diferencias de mayúsculas;
- normaliza singular y plural únicamente mediante equivalencias explícitas;
- agrupa sinónimos culinarios en español, inglés y francés;
- procesa frases mediante alias de frase antes de procesar palabras;
- ordena y elimina conceptos duplicados;
- conserva técnicas distintas como `fried`, `baked`, `boiled` y `stewed`.

Las equivalencias estarán organizadas por concepto, no por receta. Por ejemplo,
`toast/tostada/tartine`, `avocado/aguacate/avocat` y
`egg/huevo/oeuf` serán entradas del vocabulario general igual que las familias,
ingredientes y preparaciones ya soportadas.

No se aplicará stemming genérico ni traducción aproximada. El vocabulario será
ampliable sin modificar el algoritmo.

### Dos niveles de identidad

1. **Identidad canónica persistida:** seguirá respetando las tres posiciones de
   la clave y determinará el ID del documento. Evita duplicados exactos después
   de normalizar idioma, orden y sinónimos.
2. **Firma conceptual de rotación:** reunirá los conceptos canónicos de las tres
   posiciones. Permite reconocer como equivalentes claves que contienen los
   mismos conceptos aunque Gemini coloque uno en una posición incorrecta.

Las dos claves reportadas convergerán en ambos niveles. Las nuevas
contribuciones usarán el mismo ID canónico y los documentos antiguos se
recanonicalizarán al leerlos.

### Agrupación conservadora de candidatos similares

Antes de aplicar la política de vistos/no vistos, el repositorio:

1. canonicaliza y valida los candidatos;
2. los ordena por hash para elegir siempre el mismo representante;
3. agrupa identidades conceptuales exactas;
4. agrupa variantes con la misma familia canónica y una similitud de Jaccard
   igual o superior a `0.80` entre sus conceptos culinarios;
5. conserva un representante por grupo para la rotación.

El umbral incluye familia, ingredientes y preparación. Esto permite ignorar un
adorno menor cuando el núcleo del plato es idéntico, pero mantiene separadas
recetas que cambian un ingrediente principal o una técnica relevante.

Ejemplos que deben agruparse:

```text
toast|avocado+egg|assembled
tostada|aguacate+huevo+cilantro|montada
```

Ejemplos que deben permanecer separados:

```text
potato|potato|fried
potato|potato|baked

salad|chicken+tomato|mixed
salad|chicken+avocado|mixed
```

La agrupación ocurre sobre la consulta actual de hasta doce documentos, por lo
que no aumenta lecturas ni cambia el índice de Firestore.

### Migración y compatibilidad

La implementación equivalente del normalizador se mantendrá en el migrador de
Node y compartirá fixtures con Kotlin. La migración:

- seguirá siendo `dry-run` por defecto;
- realizará backup antes de aplicar;
- consolidará identidades conceptuales exactas de forma automática;
- al aplicar, releerá y bloqueará cada grupo en una transacción para incorporar
  actualizaciones concurrentes de `eligibilityKeys` antes de borrar copias;
- informará los grupos detectados solo por umbral como revisión manual, sin
  borrarlos automáticamente;
- verificará el documento canónico antes de eliminar duplicados exactos.

La similitud por umbral no provocará eliminaciones automáticas porque una
decisión incorrecta sería irreversible. Aunque esos documentos permanezcan en
Firestore, la app no los ofrecerá como alternativas distintas dentro del mismo
conjunto de candidatos.

Se mantiene `schemaVersion: 1` e `identityVersion: 2`; no cambia el contrato de
Firestore ni la compatibilidad con versiones publicadas.

## Pruebas y garantía

Se seguirá TDD con estos casos mínimos:

- las dos claves exactas de las capturas producen la misma identidad;
- variantes equivalentes en los tres idiomas convergen;
- el orden y la posición accidental de conceptos no crean otra alternativa;
- una variante con un adorno menor supera el umbral y se agrupa;
- ingredientes principales y preparaciones diferentes no colisionan;
- dos documentos reportados cuentan como un único candidato;
- Kotlin y Node producen las mismas identidades para los fixtures compartidos;
- el migrador consolida equivalencias exactas y no elimina automáticamente
  coincidencias basadas solo en el umbral.

La garantía es determinista: el caso reportado y cualquier entrada que cumpla
estas reglas no se mostrará como una receta diferente. No se promete detectar
semejanzas subjetivas fuera del vocabulario y del umbral definidos.

## Archivos previstos

- `app/src/main/java/com/menudado/domain/AiMenuHiveIdentity.kt`
- `app/src/main/java/com/menudado/data/AiMenuHiveRepository.kt`
- tests unitarios de identidad y repositorio
- fixtures compartidos bajo `app/src/test/resources/`
- `qa/ai-menu-hive-identity-v2.mjs`
- tests y migrador de `qa/`
- `docs/project-context.md`

No se modifican la UI, la cuota, el proveedor de IA, el número de consultas, el
perfil alimentario ni el guardado privado.

## Riesgos

- **Falso positivo:** umbral alto, misma familia obligatoria y pruebas negativas
  de ingredientes y técnicas.
- **Vocabulario incompleto:** alias por concepto ampliables y fixtures
  multilingües.
- **Diferencias Kotlin/Node:** fixtures compartidos y comparación de resultados.
- **Datos existentes:** lectura defensiva inmediata; limpieza solo con `dry-run`,
  backup y eliminación automática limitada a identidades exactas.
