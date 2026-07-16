# Diseño: acción secundaria protagonista para menú aleatorio

## Objetivo

Elevar `Elegir un menú al azar` como segunda acción protagonista del bloque `Qué comer hoy`, manteniendo `Lanzar con IA` como acción principal.

## Enfoques considerados

1. Botón sólido verde de marca con textos blancos. Es el enfoque elegido porque mejora contraste, jerarquía y reconocimiento como acción sin competir con el terracota de IA.
2. Mantener el botón delineado actual. Conserva una jerarquía demasiado baja para el objetivo solicitado.
3. Usar un fondo verde tonal claro con texto oscuro. Aumenta presencia, pero se percibe menos accionable que un botón sólido.

## Diseño aprobado

- Mantener posición, ancho, forma redondeada, copy y comportamiento actuales.
- Cambiar el contenedor activo a `MenuDadoColors.BrandGreen`.
- Mostrar título, texto de apoyo e indicador de progreso en blanco; el texto de apoyo puede usar una opacidad leve para conservar jerarquía interna.
- Mantener un estado deshabilitado perceptible mediante menor opacidad, sin confundirlo con una acción disponible.
- No cambiar la lógica de selección aleatoria, filtros, analítica ni navegación.

## Validación

- Prueba unitaria de los colores activos del botón.
- Suite unitaria completa y compilación del APK debug.
- Verificación visual en dispositivo de contraste, jerarquía y estados activo/deshabilitado.
