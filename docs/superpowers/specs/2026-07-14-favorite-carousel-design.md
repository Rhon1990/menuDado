# Diseño: carrusel diferencial de Favoritos

## Objetivo

Hacer que `Favoritos` se reconozca inmediatamente como una colección personal, añadir un acceso visible a todos los favoritos y conservar las acciones existentes sin introducir nuevos datos ni llamadas de red.

## Alternativas consideradas

1. **Tarjetas horizontales compactas (elegida).** Portadas redondeadas dentro de tarjetas de superficie limpia, borde arena y acento terracota lateral. Diferencia Favoritos de los carruseles cuadrados sin crear una sección pesada ni depender solo del color.
2. **Lista vertical compacta.** Mejora la lectura de nombres largos, pero ocupa demasiada altura en Inicio y reduce la visibilidad del resto de públicos.
3. **Colección circular destacada.** Diferencia por forma, pero en uso real se ve demasiado pesada, deja huecos visuales y compite con la jerarquía limpia de Inicio.

## Diseño elegido

- La sección de Inicio usa encabezado sin contenedor pesado: icono de corazón en acento terracota, título `Favoritos`, contador compacto y la acción `Ver más` siempre que exista al menos un favorito.
- Cada favorito se presenta como una tarjeta horizontal de 252 dp, superficie clara, borde `SoftSand`, acento terracota lateral y portada redondeada de 88 dp.
- La tarjeta muestra nombre en dos líneas, tipo de comida, texto de apoyo breve, corazón para quitar favorito y menú de tres puntos accesible.
- Se omiten chips de salud y calorías en esta franja para mantenerla rápida de escanear y claramente distinta de las tarjetas normales.
- Tocar la tarjeta abre el detalle existente. El corazón quita el favorito y el menú de tres puntos conserva las acciones actuales.

## Flujo `Ver más`

- `Ver más` abre una pantalla interna con cabecera y retroceso existentes.
- La pantalla muestra todos los favoritos, ordenados del más reciente al más antiguo, en la grilla reutilizable de dos columnas.
- Se reutilizan el detalle, acciones, favorito, edición y eliminación existentes.
- Si el usuario quita el último favorito, la pantalla vuelve automáticamente a Inicio para evitar un destino vacío.

## Arquitectura y alcance

- Se extiende la ruta interna de detalle de listas con una ruta cerrada para Favoritos.
- Se crea un componente Compose específico para la tarjeta horizontal de Inicio.
- La grilla completa reutiliza `MenuCarouselItem`; no se modifican Room, Firebase, ViewModel ni contratos de sincronización.
- Se añade analítica mediante el evento existente de apertura `Ver más`, sin incluir datos personales ni nombres de menús.

## Accesibilidad y UX

- Objetivos táctiles mínimos de 48 dp para corazón, menú y `Ver más`.
- El contenido descriptivo mantiene el nombre del menú y las acciones usan los textos accesibles existentes.
- La diferenciación combina orientación horizontal, acento, superficie y jerarquía; no depende solo del color.
- Los textos se añaden en español, inglés y francés.

## Validación

- Pruebas unitarias para ruta de Favoritos, orden y visibilidad de `Ver más`.
- Pruebas de regresión de las rutas por público y del filtrado de favoritos.
- `testDebugUnitTest`, `assembleDebug` y revisión visual en emulador.
- Prueba manual de abrir `Ver más`, volver, abrir detalle, quitar favorito y comprobar el comportamiento al quedar sin favoritos.
