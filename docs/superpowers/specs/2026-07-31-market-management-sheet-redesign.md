# Rediseño de gestión de la lista de Mercado

Fecha: 2026-07-31  
Estado: diseño aprobado

## Objetivo

Reducir el peso visual de las acciones destructivas de Mercado y alinearlas con el lenguaje de MenuDado. La funcionalidad existente de `Vaciar lista` y `Limpiar comprados` se conserva; solo cambia su presentación y el acceso a ellas.

## Diseño aprobado

### Cabecera

- Retirar el botón con texto `Vaciar lista` de la cabecera.
- Mostrar un botón de tres puntos verticales a la derecha de `Lista de mercado` únicamente cuando exista al menos un producto.
- Reutilizar `ic_more_vertical`, el tamaño táctil mínimo de 48 dp y los colores del icono de opciones ya usados en las tarjetas de menú.
- Su descripción accesible será `Gestionar lista`, localizada en español, inglés y francés.

### Hoja de gestión

Al tocar los tres puntos se abrirá una hoja modal inferior titulada `Gestionar lista`.

La hoja reutilizará el patrón visual de `MenuActionsSheet`:

- fondo `MenuDadoColors.HeaderGreen`;
- esquinas superiores de 28 dp;
- tirador blanco translúcido;
- botón de cierre de 48 dp;
- título y contenido blancos;
- divisor blanco translúcido;
- cierre mediante la X, Back o toque fuera de la hoja.

Las filas seguirán el patrón de las opciones de foto: icono blanco dentro de un círculo blanco translúcido, título destacado y descripción breve.

Acciones:

1. `Limpiar comprados`
   - Solo se muestra cuando existen productos comprados.
   - Descripción: conserva los productos que todavía están pendientes.
   - Usará un nuevo vector reutilizable `ic_check`, diferenciable del borrado total.
2. `Vaciar toda la lista`
   - Se muestra siempre que Mercado contenga algún producto.
   - Descripción: retira todos los productos sin borrar los menús.
   - Reutiliza `ic_delete`.

La hoja nunca ejecuta una eliminación directamente.

### Sección Comprados

- Retirar la papelera situada junto al título.
- Presentar `Comprados` como una cabecera contenida y cálida, con su cantidad y el indicador de expandir o contraer.
- Toda la cabecera alterna la expansión, sin acciones anidadas que puedan provocar toques ambiguos.

## Flujo

1. El usuario toca los tres puntos.
2. Se registra únicamente la apertura categórica de `Gestionar lista`.
3. Se abre la hoja verde.
4. El usuario selecciona una acción.
5. La hoja se cierra y se abre la confirmación específica existente.
6. Solo al confirmar se llama a `clearPurchasedMarketProducts()` o `clearAllMarketProducts()`.

Cancelar la hoja o la confirmación no modifica datos. Los errores continúan usando el mensaje localizado existente del ViewModel.

La hoja de gestión se incorporará a la prioridad modal actual. No podrá superponerse con mensajes de IA, privacidad, onboarding, edición, detalles u otras superficies modales.

## Arquitectura y reutilización

- Mantener sin cambios `MarketDao`, `MenuRepository` y `MenuDadoViewModel`.
- Reutilizar los tokens, colores, cierre, forma y comportamiento de la hoja de acciones existente.
- Extraer solo las piezas visuales mínimas necesarias para compartir el contenedor y la fila de acción entre `MenuActionsSheet` y la nueva hoja de Mercado.
- Mantener `MarketClearAction` como contrato de las dos operaciones y reutilizar las confirmaciones ya probadas.
- No introducir una librería, navegación nueva, cambio de esquema Room ni cambio de Firebase.

## Accesibilidad y analítica

- Área táctil mínima de 48 dp para tres puntos, cierre y filas.
- Descripciones accesibles localizadas para abrir y cerrar la hoja.
- Títulos y descripciones legibles con contraste blanco sobre verde.
- Orden de foco: cierre, título, `Limpiar comprados` cuando exista y `Vaciar toda la lista`.
- Analytics enviará solo nombres categóricos de CTA; nunca productos, IDs de menú ni datos alimentarios.

## Pruebas

- Pruebas unitarias de visibilidad y orden de acciones según pendientes y comprados.
- Pruebas de recursos, iconos, colores, tamaños y textos localizados.
- Pruebas Compose para:
  - abrir la hoja desde los tres puntos;
  - ocultar `Limpiar comprados` si no corresponde;
  - cerrar mediante X y Back sin mutación;
  - seleccionar cada acción y mostrar su confirmación sin ejecutar todavía;
  - alternar `Comprados` sin disparar una limpieza;
  - respetar la prioridad modal.
- Ejecutar la suite unitaria completa, las pruebas instrumentadas relacionadas y `assembleDebug`.

## Fuera de alcance

- Cambiar la lógica o sincronización de las limpiezas.
- Modificar las confirmaciones actuales.
- Cambiar el contenido de la lista de productos.
- Desplegar Firebase, publicar en Play Console o generar una release.
