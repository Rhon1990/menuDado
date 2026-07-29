# Diseño: jerarquía de contenido en Favoritos

## Objetivo

Reducir la repetición visual de `Favoritos` en Inicio y convertir el nombre del menú en el contenido dominante de cada tarjeta, sin perder la acción rápida para quitarlo de la colección.

## Alternativas consideradas

1. **Limpieza editorial con acción directa (elegida).** El título de sección identifica la colección; cada tarjeta conserva un único corazón interactivo, elimina la etiqueta repetida y reserva más espacio al nombre.
2. **Tarjeta totalmente minimalista.** Oculta también el corazón y mueve la acción al menú de tres puntos. Mejora la limpieza, pero hace más difícil descubrir y deshacer el estado favorito.
3. **Tarjeta vertical grande.** Da todavía más espacio al nombre, pero ocupa demasiada altura y reduce la exploración rápida del resto de colecciones.

## Diseño aprobado

- La cabecera conserva `Favoritos`, el contador y `Ver más`, pero elimina el corazón decorativo.
- La tarjeta elimina el texto interno `Favoritos` porque el contexto de la sección ya comunica la colección.
- El nombre usa una jerarquía tipográfica superior y admite hasta tres líneas.
- El menú de tres puntos permanece al final de la fila del nombre, con fondo transparente y fuera de la portada.
- La fila inferior conserva únicamente el corazón como acción directa para quitar el menú de favoritos.
- Tipo de comida y público permanecen como metadatos secundarios.
- La tarjeta crece moderadamente para evitar truncados frecuentes sin convertir el carrusel en una lista pesada.

## Arquitectura y alcance

- El cambio se limita a `FavoriteMenuCarouselSection`, `FavoriteMenuCarouselItem` y sus tokens de presentación en `MenuDadoScreen.kt`.
- No cambian navegación, ViewModel, Room, Firebase, analítica ni contratos de datos.
- Se actualiza `docs/project-context.md` para que la descripción funcional refleje la nueva jerarquía.

## Accesibilidad y comportamiento

- El corazón mantiene su descripción accesible existente para quitar de favoritos.
- El menú de tres puntos conserva su acción y objetivo táctil; su posición visual cambia, no su contrato.
- El nombre sigue formando parte de la descripción semántica de la tarjeta.
- La diferenciación de la colección conserva el acento terracota lateral y no depende de repetir texto o iconos.

## Validación

- Prueba unitaria de los tokens de jerarquía: ancho, alto, portada y máximo de líneas del título.
- Prueba unitaria que fija la ausencia del icono decorativo y de la etiqueta repetida.
- Ejecución de `MenuCardUiStateTest`, suite unitaria de debug y `assembleDebug`.
- Revisión visual en emulador con un nombre largo, comprobando que el título domine, que no aparezca la etiqueta repetida y que corazón y menú sigan siendo accionables.
