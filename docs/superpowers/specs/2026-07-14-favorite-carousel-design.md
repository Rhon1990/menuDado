# Diseño: carrusel diferencial de Favoritos

## Objetivo

Hacer que `Favoritos` se reconozca inmediatamente como una colección personal, añadir un acceso visible a todos los favoritos y conservar las acciones existentes sin introducir nuevos datos ni llamadas de red.

## Alternativas consideradas

1. **Colección circular destacada (elegida).** Portadas circulares dentro de una superficie verde suave, con corazón terracota, nombre y tipo de comida. Es la alternativa que más diferencia Favoritos de los carruseles normales y mantiene una exploración rápida.
2. **Lista vertical compacta.** Mejora la lectura de nombres largos, pero ocupa demasiada altura en Inicio y reduce la visibilidad del resto de públicos.
3. **Tarjetas rectangulares con otro color.** Tiene bajo riesgo, pero la forma sigue pareciéndose demasiado a los carruseles actuales y la diferencia depende casi por completo del color.

## Diseño elegido

- La sección de Inicio usa una superficie `SoftGreen` de ancho completo, esquinas amplias y separación interna consistente con `Calma editorial`.
- El encabezado contiene icono de corazón, título `Favoritos`, contador de menús y la acción `Ver más` siempre que exista al menos un favorito.
- Cada favorito se presenta como una portada circular de 104 dp con borde crema, corazón terracota superpuesto y menú de tres puntos accesible.
- Bajo la portada se muestra el nombre en un máximo de dos líneas y el tipo de comida como información secundaria. Se omiten chips de salud y calorías en esta franja para conservar una silueta ligera y claramente distinta.
- Tocar la portada o el nombre abre el detalle existente. El corazón quita el favorito y el menú de tres puntos conserva las acciones actuales.

## Flujo `Ver más`

- `Ver más` abre una pantalla interna con cabecera y retroceso existentes.
- La pantalla muestra todos los favoritos, ordenados del más reciente al más antiguo, en la grilla reutilizable de dos columnas.
- Se reutilizan el detalle, acciones, favorito, edición y eliminación existentes.
- Si el usuario quita el último favorito, la pantalla vuelve automáticamente a Inicio para evitar un destino vacío.

## Arquitectura y alcance

- Se extiende la ruta interna de detalle de listas con una ruta cerrada para Favoritos.
- Se crea un componente Compose específico para la tarjeta circular de Inicio.
- La grilla completa reutiliza `MenuCarouselItem`; no se modifican Room, Firebase, ViewModel ni contratos de sincronización.
- Se añade analítica mediante el evento existente de apertura `Ver más`, sin incluir datos personales ni nombres de menús.

## Accesibilidad y UX

- Objetivos táctiles mínimos de 48 dp para corazón, menú y `Ver más`.
- El contenido descriptivo mantiene el nombre del menú y las acciones usan los textos accesibles existentes.
- La diferenciación combina forma, superficie y jerarquía; no depende solo del color.
- Los textos se añaden en español, inglés y francés.

## Validación

- Pruebas unitarias para ruta de Favoritos, orden y visibilidad de `Ver más`.
- Pruebas de regresión de las rutas por público y del filtrado de favoritos.
- `testDebugUnitTest`, `assembleDebug` y revisión visual en emulador.
- Prueba manual de abrir `Ver más`, volver, abrir detalle, quitar favorito y comprobar el comportamiento al quedar sin favoritos.
