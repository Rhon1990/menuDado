# Actualización quirúrgica del contexto de MenuDado

## Objetivo

Actualizar `docs/project-context.md` sin reescribir su especificación funcional, de forma que distinga con precisión el estado actual del código, la evidencia histórica y las validaciones pendientes. El documento seguirá siendo el índice funcional y técnico principal para desarrollo, QA y redacción de tareas Jira.

## Estado actual observado

- `docs/project-context.md` existe, está versionado y contiene la visión, identidad visual, comportamiento funcional, dirección técnica, principios de UX y foco de validación de MenuDado.
- La configuración Android actual declara `compileSdk=36`, `targetSdk=36`, `minSdk=23`, `versionName=1.3.0` y `versionCode=15`.
- La rama actual es `release/Version_1.3.0` y no contiene cambios locales al iniciar esta actualización.
- El documento mezcla estado actual con evidencia histórica de QA y conserva referencias contradictorias a `versionCode` 14/15 y a las claves `about_description_v2`/`about_description_v3`.
- Google Play solo reconocerá el cumplimiento de API 36 después de que una versión de producción compatible sea publicada y procesada; la configuración local por sí sola no demuestra que la ficha de Play esté actualizada.

## Enfoque aprobado

Se realizará una actualización quirúrgica:

- Conservar la especificación funcional y las decisiones de producto vigentes.
- Corregir únicamente afirmaciones obsoletas, contradictorias o ambiguas.
- Añadir un bloque de estado técnico verificado con fecha, alcance de la verificación y fuentes de verdad.
- Separar evidencia vigente, evidencia histórica y controles todavía pendientes.
- No reordenar de forma masiva el documento ni convertirlo en una especificación nueva.

## Cambios previstos

### Estado técnico actual

La sección `Dirección Técnica` indicará explícitamente:

- Los niveles SDK y la versión declarados actualmente en `app/build.gradle.kts`.
- Que la rama ya está orientada a Android 16/API 36.
- Que el aviso de Google Play no implica por sí mismo que la rama local siga en API 35; el cierre de la política requiere un AAB compatible publicado en producción y procesado por Google Play.
- Que `app/build.gradle.kts` y el manifiesto resultante prevalecen sobre evidencia documental anterior cuando exista una discrepancia.

### Coherencia de configuración remota

Las referencias activas de `Acerca de la app` se normalizarán a `about_description_v3`, `about_created_by` y `about_contact`, de acuerdo con el contrato actual documentado y el código vigente. Las menciones a `about_description_v2` se conservarán únicamente cuando estén identificadas como evidencia histórica y no como configuración actual.

### Auditoría QA

La auditoría distinguirá tres estados:

1. Verificación estática actual: configuración y fuentes contrastadas el 18 de agosto de 2026.
2. Evidencia histórica: comandos y pruebas ejecutados anteriormente, manteniendo su fecha y versión exactas.
3. Validación pendiente: generación firmada, publicación en producción, procesamiento de Google Play y regresión manual con servicios productivos.

La prueba manual que mostró `Versión 1.3.0 (14)` se describirá como evidencia de un artefacto anterior y no como estado actual, porque la configuración vigente declara `versionCode=15`.

## Fuera de alcance

- Cambiar código, Gradle, recursos, Firebase o Google Play Console.
- Publicar, firmar o subir un App Bundle.
- Revalidar toda la especificación funcional de 371 líneas si no existe una señal concreta de obsolescencia.
- Reorganizar la arquitectura del proyecto o introducir decisiones de producto nuevas.
- Presentar builds o pruebas históricas como evidencia ejecutada en esta actualización.

## Validación

- Comparar los datos de versión y SDK del documento con `app/build.gradle.kts`.
- Confirmar mediante búsquedas dirigidas que no quedan contradicciones activas sobre `versionCode` ni sobre las claves `about_description`.
- Revisar que la advertencia de Google Play quede descrita como un estado externo pendiente de publicación, no como un fallo local ya demostrado.
- Ejecutar `git diff --check` y revisar el diff completo del documento.
- Ejecutar la verificación documental o técnica mínima necesaria para no convertir evidencia histórica en evidencia actual.

## Riesgos y mitigaciones

- **Documento demasiado extenso:** se limita el cambio a las secciones afectadas y no se reescribe el contenido funcional.
- **Confundir configuración con publicación:** se separa expresamente el cumplimiento local del reconocimiento en Google Play.
- **Caducidad de la evidencia QA:** cada resultado mantiene su fecha y se etiqueta como actual, histórico o pendiente.
- **Pérdida de decisiones previas:** se conserva la estructura existente y no se eliminan contratos funcionales sin evidencia en código.
