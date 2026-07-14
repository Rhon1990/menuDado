# Tu último menú y detalle sin borde blanco

## Objetivo

Hacer que el acceso rápido de Inicio describa con precisión su comportamiento y eliminar la cuña blanca visible en las esquinas superiores del detalle de un menú guardado.

## Comportamiento

- El bloque mantiene la selección actual: muestra el menú con mayor `createdAt` y abre su detalle al tocarlo.
- El título visible pasa de `Continúa donde lo dejaste` a `Tu último menú`.
- La traducción inglesa será `Your latest menu` y la francesa `Votre dernier menu`.
- No se añade persistencia de borradores, seguimiento del último menú abierto ni cambios de navegación, datos o analítica.

## Diseño visual

- El detalle de un menú guardado usa un único radio de 28 dp para la tarjeta exterior y la cabecera visual.
- El detalle generado con IA reutiliza el mismo contrato de radio para evitar divergencias futuras.
- Se conservan colores, altura de cabecera, contenido, acciones y elevación actuales.

## Validación

- Test unitario que exige un radio compartido de 28 dp para ambos detalles.
- Verificación de que el menú más reciente sigue seleccionándose por `createdAt`.
- Suite `testDebugUnitTest`, ensamblado debug y revisión visual en emulador.
- Comprobación de traducciones en español, inglés y francés.

