# Título discreto según el origen de la idea generada

## Objetivo

Permitir que el desarrollador distinga visualmente si la idea mostrada procede de Gemini o de la colmena sin añadir iconos, colores, insignias ni explicaciones visibles sobre el funcionamiento interno.

## Alcance

El cambio afecta únicamente al título del modal de detalle de una idea recién generada. No modifica tarjetas de menús guardados, datos persistidos, Analytics, cuotas, anuncios ni el comportamiento de generación y fallback.

## Comportamiento

El estado ya expone `GeneratedMenuOrigin` con dos valores:

- `LIVE_AI`: mantiene el título que atribuye explícitamente la idea a la IA.
- `HIVE_FALLBACK`: utiliza un título neutral que no menciona IA ni colmena.

Los textos localizados serán:

| Origen | Español | Inglés | Francés |
| --- | --- | --- | --- |
| `LIVE_AI` | `Idea generada con IA` | `AI-generated idea` | `Idée générée par l’IA` |
| `HIVE_FALLBACK` | `Idea generada` | `Generated idea` | `Idée générée` |

Si por un estado inesperado el origen es nulo mientras el modal está visible, se utilizará el título neutral. De este modo la interfaz nunca atribuye falsamente a Gemini una idea cuyo origen no está confirmado.

## Diseño técnico

`MenuDadoScreen` pasará `state.generatedOrigin` a `GeneratedMenuDetailDialog`. Una función pequeña y comprobable seleccionará el recurso de texto correspondiente. El estilo, la posición y el espaciado del título permanecerán sin cambios.

Se añadirá un nuevo recurso localizado para el título neutral y se conservará `generated_menu_detail_title` para el título de Gemini, evitando duplicar estilos o componentes.

## Validación

- Verificar que `LIVE_AI` selecciona el título con referencia a IA.
- Verificar que `HIVE_FALLBACK` selecciona el título neutral.
- Verificar que un origen nulo usa el título neutral.
- Comprobar que los recursos existen en español, inglés y francés.
- Ejecutar las pruebas unitarias relacionadas, la suite debug completa y la compilación Kotlin de release.

## Riesgos

El riesgo es bajo porque el origen ya se calcula en el `ViewModel` y el cambio solo modifica la presentación del título. No se debe inferir el origen desde mensajes de error ni desde el contenido de la receta.
