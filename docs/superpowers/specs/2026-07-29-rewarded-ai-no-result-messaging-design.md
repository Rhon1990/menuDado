# Anuncios bonificados y mensajes de generación sin resultado

**Fecha:** 2026-07-29  
**Estado:** aprobado para planificación

## Contexto

La oferta de anuncio bonificado se decide con el contador del ámbito activo
(`guest` o `account:<uid>`), mientras que la llamada real a Firebase AI Logic se
protege con un contador técnico compartido de 20 solicitudes diarias.

En el caso observado, el ámbito de la cuenta todavía podía obtener recompensas,
pero el contador técnico ya estaba en 20. La app mostró el anuncio, concedió y
consumió el crédito, evitó correctamente una llamada adicional al proveedor y
consultó el respaldo interno. Al no existir una propuesta compatible para
`Almuerzo · Bebé (6-24 meses)`, mostró el aviso diario genérico.

Este comportamiento tiene dos problemas:

1. permite ofrecer un anuncio nuevo cuando ya no existe capacidad para realizar
   la llamada de IA prometida;
2. cuando la generación y el respaldo interno no producen resultado, el aviso
   no explica el motivo ni el contexto solicitado.

## Objetivos

- No ofrecer un anuncio bonificado nuevo si el contador técnico ya alcanzó
  20 llamadas.
- Conservar el comportamiento existente de los usos gratuitos y créditos
  obtenidos previamente cuando el proveedor ya no está disponible: pueden
  intentar resolverse mediante el respaldo interno sin efectuar otra llamada.
- Mostrar un aviso localizado y accionable cuando no se obtiene un menú,
  distinguiendo la causa real del fallo.
- Personalizar el aviso con tipo de comida, público y rango de edad efectivos.
- Mantener completamente invisible para el usuario la existencia del respaldo
  compartido.
- Aplicar la solución a Persona adulta, Peques y Bebé, incluidos rangos de edad
  configurados.

## No objetivos

- Poblar manualmente `sharedAiMenus` o garantizar que exista una receta para
  todas las combinaciones posibles.
- Relajar público, edad, alergias, restricciones o seguridad alimentaria para
  forzar un resultado.
- Cambiar los límites gratuitos, el máximo de recompensas o el máximo técnico
  de 20 llamadas.
- Cambiar reglas, índices o documentos de Firestore.
- Exponer en el aviso alergias, evitaciones, contenido compartido o detalles de
  infraestructura.

## Diseño

### 1. Resolución de acceso al anuncio

La política por ámbito seguirá resolviendo primero uno de estos estados:
`FREE`, `REWARDED_CREDIT`, `REWARDED_OFFER` o `HARD_LIMIT`.

El ViewModel aplicará después la disponibilidad del proveedor:

- `REWARDED_OFFER` con contador técnico menor que 20 mantiene la oferta.
- `REWARDED_OFFER` con contador técnico igual a 20 se convierte en
  `HARD_LIMIT`; el CTA de anuncio desaparece y tocar el dado muestra el aviso
  diario contextual.
- `FREE` y `REWARDED_CREDIT` no se invalidan al llegar a 20. Son autorizaciones
  ya existentes y conservan la consulta directa al respaldo interno.

Así se evita pedir un vídeo nuevo cuando no puede iniciarse una llamada real,
sin compartir saldos entre invitado y cuentas ni eliminar créditos ya ganados.

### 2. Presentación contextual del fallo

La clasificación técnica existente seguirá determinando la pausa, el reintento
y la analítica. Solo la presentación de un fallo de generación sin resultado
incorporará el contexto de la solicitud validada.

El contexto visible tendrá este formato conceptual:

`<tipo de comida> · <público> (<rango de edad>)`

Ejemplos:

- `Almuerzo · Persona adulta (18+ años)`
- `Cena · Peques (2-12 años)`
- `Desayuno · Bebé (6-24 meses)`

Se usará el rango efectivo del perfil seleccionado. Si está vacío, se usará el
rango predeterminado del público. Cuando el perfil tenga restricciones, el
aviso añadirá únicamente `con tu perfil actual`; nunca enumerará alergias,
embarazo, veganismo u otras evitaciones.

### 3. Mensajes por causa

Los textos definitivos se localizarán en español, inglés y francés siguiendo
estas plantillas semánticas:

| Causa | Mensaje en español |
| --- | --- |
| Límite técnico diario | `Hoy no podemos preparar más ideas para <contexto>. Vuelve a intentarlo mañana.` |
| Cuota o alta demanda | `La IA está con mucha demanda y no pudo preparar una idea para <contexto><perfil>. Inténtalo más tarde.` |
| Timeout | `La IA tardó demasiado y no pudo preparar una idea para <contexto><perfil>. Revisa tu conexión e inténtalo de nuevo.` |
| Conectividad o fallo genérico | `No pudimos preparar una idea para <contexto><perfil>. Revisa tu conexión e inténtalo de nuevo.` |
| Servicio o configuración temporal | `No pudimos preparar una idea para <contexto><perfil> en este momento. Inténtalo nuevamente más tarde.` |

`<perfil>` será vacío o ` con tu perfil actual`, según existan restricciones.

Si el respaldo interno devuelve un menú válido, no se mostrará ningún aviso: se
abrirá el detalle generado como hasta ahora.

### 4. Privacidad y abstracción

Ningún texto visible mencionará:

- colmena o `hive`;
- Firestore;
- otras personas o contenido compartido;
- búsqueda secundaria, fallback o fuente alternativa;
- ausencia de documentos compatibles.

Los nombres internos, eventos agregados y métricas técnicas pueden conservarse
porque no se presentan al usuario ni contienen recetas, perfiles o
identificadores publicitarios.

## Estrategia de pruebas

### Acceso y anuncios

- Con ámbito agotado, sin crédito y proveedor en 19, se mantiene
  `REWARDED_OFFER`.
- Con ámbito agotado, sin crédito y proveedor en 20, se obtiene `HARD_LIMIT` y
  `requestRewardedGeneration()` no puede abrir el anuncio.
- Un crédito ya obtenido sigue autorizado con proveedor en 20 y consulta el
  respaldo interno sin llamar al proveedor.
- Los saldos de invitado y cuentas continúan independientes.

### Mensajes

- Cada categoría de fallo produce su acción correcta: revisar conexión,
  reintentar más tarde o volver mañana.
- Persona adulta, Peques y Bebé muestran sus etiquetas y rangos efectivos.
- Un rango personalizado sustituye al rango predeterminado.
- Las restricciones solo añaden `con tu perfil actual`.
- Los avisos ES, EN y FR no contienen términos que revelen el respaldo interno.
- Un resultado válido del respaldo abre el detalle y no muestra el aviso.

### Regresión

- Ejecutar los tests unitarios de `AiDailyUsagePolicy` y
  `MenuDadoViewModelTest`.
- Ejecutar todos los tests unitarios de `debug`.
- Compilar `debug`.
- Realizar una prueba guiada en dispositivo con proveedor en 20:
  comprobar que no se ofrece un anuncio nuevo y que el aviso refleja el público
  seleccionado.

## Riesgos y mitigaciones

- **Avisos largos:** mantener una sola frase principal y una acción breve;
  validar visualmente los tres idiomas en una pantalla estrecha.
- **Rango vacío o inconsistente:** usar el rango predeterminado del público como
  fallback visual.
- **Confusión entre fallo y seguridad:** no relajar el perfil ni presentar una
  receta incompatible; el aviso explica que no se pudo preparar la idea.
- **Regresión de créditos existentes:** cubrir explícitamente que
  `REWARDED_CREDIT` sigue autorizado cuando el proveedor está en 20.

## Despliegue

El cambio será exclusivamente de aplicación Android, tests y contexto del
proyecto. No requiere despliegue de Firestore, Remote Config ni otros recursos
Firebase.
