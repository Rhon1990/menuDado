# Ajuste visual del botón de filtros del catálogo

## Objetivo

Integrar visualmente el botón de filtros con la cabecera verde de MenuDado y centrar de forma perceptible el número del contador activo, sin cambiar el funcionamiento de la búsqueda ni de los filtros.

## Diseño aprobado

- Mantener la barra de búsqueda crema existente y el espacio compacto entre ambos controles.
- Sustituir el fondo verde menta aislado del botón por una superficie translúcida sobre el verde de la cabecera.
- Añadir un borde crema y usar el icono de filtro en crema para que el control nazca visualmente de la cabecera.
- Conservar el tamaño táctil mínimo de 48 dp y el radio de control existente.
- Mantener el badge terracota, aumentarlo a 20 dp y desplazarlo ligeramente hacia la esquina superior derecha para que no comprima el icono.
- Centrar el número eliminando el padding tipográfico de Android y definiendo una altura de línea explícita igual al tamaño de texto.

## Alcance técnico

El cambio se limita a `MenuCatalogSearchBar`. No modifica consulta, ámbito fijo de Adulto/Peques/Bebé/Favoritos, conteo de filtros, apertura de la hoja, reinicio de estado ni reglas de compatibilidad del perfil.

## Accesibilidad y estados

- La superficie táctil conserva 48 dp.
- La descripción accesible actual continúa anunciando la cantidad de filtros activos.
- Sin filtros activos no se muestra badge.
- Con uno o más filtros se muestra el contador centrado; el icono mantiene contraste suficiente sobre la cabecera verde.

## Validación

- Prueba de componente para comprobar que el badge aparece solo cuando corresponde y conserva la interacción del botón.
- Prueba unitaria de los tokens visuales relevantes para evitar regresar al fondo menta o al padding tipográfico.
- Compilación debug y pruebas unitarias del módulo.
- Revisión visual en la pantalla real con cero y al menos un filtro activo.

## Fuera de alcance

- Rediseñar la hoja de filtros.
- Cambiar filtros disponibles, sus reglas o el número que cuenta como activo.
- Modificar la barra de búsqueda, la cabecera o las tarjetas de menú.
