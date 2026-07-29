# Diseño: equilibrio visual de la tarjeta de Favoritos

## Objetivo

Mejorar la legibilidad y el equilibrio vertical de la tarjeta horizontal de `Favoritos`. El nombre debe disponer de más ancho y margen superior, mientras que la inspiración culinaria y el metadato de tipo y público deben leerse como un bloque continuo, sin el hueco provocado por la altura táctil del corazón.

## Alternativas consideradas

1. **Redistribución interna y ampliación moderada (elegida).** Amplía ligeramente la tarjeta, reduce de forma mínima la portada y desacopla las acciones del flujo vertical de los textos. Resuelve las dos causas sin cambiar el patrón del carrusel.
2. **Aumentar únicamente la altura.** Añade aire alrededor del título, pero mantiene el hueco bajo la inspiración culinaria y hace más pesada la sección.
3. **Convertir Favoritos en tarjetas verticales.** Ofrece más espacio al contenido, pero reduce la exploración horizontal y rompe la coherencia con el resto de colecciones.

## Diseño aprobado

- La tarjeta pasa de 280 dp a 300 dp de ancho y mantiene 148 dp como altura mínima, pudiendo crecer cuando la escala de fuente necesita más espacio.
- La portada pasa de 96 dp a 92 dp para recuperar ancho útil sin perder protagonismo visual.
- El bloque de contenido se centra verticalmente, usa 12 dp de margen interno y conserva un máximo de tres líneas para el nombre, de modo que el aire superior e inferior sea equivalente.
- El menú de tres puntos permanece en la esquina superior derecha y el corazón en la inferior derecha, ambos con su objetivo táctil mínimo de 48 dp, pero dejan de determinar la altura de las filas de texto.
- El título reserva el espacio necesario para el menú de acciones sin quedar pegado al borde superior.
- `Cocina …` y `Cena · Persona adulta` se muestran consecutivamente, con una separación visual corta y constante.
- El metadato inferior reserva el espacio del corazón para evitar solapamientos o truncados inesperados.
- Se conservan el borde arena, el acento terracota, los colores, la navegación y todas las acciones actuales.

## Arquitectura y alcance

- El cambio se limita a `FavoriteMenuCarouselItem` y a sus tokens de presentación en `MenuDadoScreen.kt`.
- `MenuCardUiStateTest` fijará ancho, altura, portada, márgenes y separación entre metadatos.
- `docs/project-context.md` se actualizará para reflejar la composición compacta y continua de los metadatos.
- No cambian ViewModel, navegación, Room, Firebase, analítica, orden ni contratos de datos.

## Accesibilidad y comportamiento responsive

- El corazón y el menú de tres puntos conservan sus descripciones y áreas táctiles actuales.
- El nombre mantiene elipsis tras tres líneas para que textos extremos no expandan la tarjeta indefinidamente.
- La anchura de 300 dp mantiene visible parte de la tarjeta siguiente en teléfonos habituales, conservando la señal visual del carrusel.
- Los textos localizados largos siguen limitados por líneas y elipsis, sin solaparse con las acciones.
- La altura no se fija: el contenido ampliado puede hacer crecer la tarjeta y el acento terracota acompaña toda la altura resultante.

## Validación

- Ejecutar primero la prueba de regresión de tokens y confirmar que falla con los valores actuales.
- Implementar la composición mínima y volver a ejecutar `MenuCardUiStateTest`.
- Ejecutar la suite unitaria de debug y `assembleDebug`.
- Revisar en emulador un favorito con nombre de tres líneas e inspiración culinaria, comprobando margen superior, proximidad entre metadatos, elipsis, objetivos táctiles y visibilidad parcial de la siguiente tarjeta.

## Riesgos

- En pantallas muy estrechas se verá menos contenido de la siguiente tarjeta, aunque seguirá existiendo una pista del desplazamiento horizontal.
- Nombres o traducciones excepcionalmente largos seguirán usando elipsis; el detalle completo continúa accesible al abrir el menú.
