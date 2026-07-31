# Búsqueda por todo el texto visible de las tarjetas

## Objetivo

Permitir que el buscador de las colecciones completas encuentre un menú mediante cualquier texto visible en su tarjeta, usando exactamente el idioma activo de la aplicación y conservando los campos buscables actuales.

## Comportamiento aprobado

La búsqueda seguirá ignorando mayúsculas y acentos. Mantendrá nombre, descripción, notas, cocina e ingredientes/productos y añadirá:

- tipo de comida: por ejemplo, `Desayuno`, `Breakfast` o `Petit-déjeuner`;
- público: `Adulto`, `Peques` o `Bebé`, únicamente en Favoritos, donde la tarjeta lo muestra;
- estado de salud, incluido el estado sin análisis: por ejemplo, `Saludable`, `Healthy`, `Sain` o `Sin analizar`;
- calorías cuando son visibles: tanto el número como el texto completo, por ejemplo `450` y `450 kcal`.

No se indexarán descripciones accesibles de los iconos ni textos externos a la tarjeta, como el título de pantalla o los contadores de colección.

## Diseño técnico

`MenuDadoScreen` construirá un único objeto de etiquetas de búsqueda con los mismos recursos localizados que ya renderizan tipo de comida, público, cocina, estado saludable y calorías. `MenuCatalogFilters` recibirá ese objeto y compondrá el texto buscable de cada menú sin introducir traducciones manuales ni depender de las etiquetas en español de los enums.

El estado `Sin analizar` se incluirá porque la tarjeta siempre muestra ese chip cuando no existe análisis. Las calorías se añadirán solo bajo la misma condición que la UI utiliza para mostrarlas. El público se añadirá únicamente para el ámbito Favoritos, porque las tarjetas de Adulto, Peques y Bebé no lo repiten.

## Alcance y compatibilidad

- Se aplica a las colecciones completas de Adulto, Peques, Bebé y Favoritos.
- No modifica el ámbito fijo de cada colección, el orden, la hoja de filtros ni el reinicio al volver a entrar.
- Una coincidencia textual continúa intersectándose con los filtros activos; nunca incorpora menús fuera del público o de Favoritos.
- Se reutilizan los recursos existentes de español, inglés y francés; no se agregan cadenas traducibles nuevas.

## Validación

- Pruebas unitarias para tipo de comida, público en Favoritos, todos los estados de salud y calorías visibles.
- Pruebas de regresión para nombre, descripción, notas, cocina e ingredientes/productos.
- Casos con etiquetas en español, inglés y francés para demostrar que el filtro consume el texto localizado recibido.
- Comprobación de que calorías ocultas y público no mostrado no producen coincidencias.
- Suite unitaria y compilación debug del módulo antes de integrar.

## Fuera de alcance

- Buscar dentro de acciones de iconos, contenido del modal o texto de otras pantallas.
- Cambiar el placeholder del buscador o el diseño de la tarjeta.
- Añadir búsqueda aproximada, sinónimos o traducción automática entre idiomas.
