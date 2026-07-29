# Microcopy cercana para la creación con IA

## Objetivo

Hacer que el bloque principal de creación con IA hable primero de la situación cotidiana del usuario y después explique la ayuda de la IA. El cambio será puntual: no se reescribirá el resto de la aplicación.

## Alcance

Solo se modificarán estas seis claves en español, inglés y francés:

- `home_today_title`
- `home_today_subtitle`
- `home_ai_dice_title`
- `form_base_ingredients`
- `form_base_ingredients_placeholder`
- `dice_roll_ai_with_count`

No se modificarán navegación, lógica, estados, cuotas, errores, prompts, analítica, Firebase, `Analizar IA` ni otros textos visibles.

## Textos aprobados

### Español

| Clave | Antes | Después |
|---|---|---|
| `home_today_title` | `Qué comer hoy` | `¿No sabes qué preparar hoy?` |
| `home_today_subtitle` | `Genera una idea saludable con IA o escribe tu propio menú.` | `No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable.` |
| `home_ai_dice_title` | `Generar idea saludable` | `Encontremos algo rico` |
| `form_base_ingredients` | `Ingredientes base opcionales` | `¿Qué tienes en casa? (Opcional)` |
| `form_base_ingredients_placeholder` | `Ej. berenjena, tomate` | `Ej. tomate, arroz o pollo` |
| `dice_roll_ai_with_count` | `Lanzar con IA (%1$d)` | `Ayúdame a elegir (%1$d)` |

### Inglés

| Clave | Después |
|---|---|
| `home_today_title` | `Not sure what to make today?` |
| `home_today_subtitle` | `No worries. Tell us who the menu is for and AI will help with a healthy idea.` |
| `home_ai_dice_title` | `Let's find something tasty` |
| `form_base_ingredients` | `What do you have at home? (Optional)` |
| `form_base_ingredients_placeholder` | `E.g. tomato, rice or chicken` |
| `dice_roll_ai_with_count` | `Help me choose (%1$d)` |

### Francés

| Clave | Después |
|---|---|
| `home_today_title` | `Vous ne savez pas quoi préparer aujourd’hui ?` |
| `home_today_subtitle` | `Pas de souci. Dites-nous à qui s’adresse le menu et l’IA vous proposera une idée équilibrée.` |
| `home_ai_dice_title` | `Trouvons quelque chose de bon` |
| `form_base_ingredients` | `Qu’avez-vous à la maison ? (Facultatif)` |
| `form_base_ingredients_placeholder` | `Ex. tomate, riz ou poulet` |
| `dice_roll_ai_with_count` | `Aidez-moi à choisir (%1$d)` |

Las versiones inglesa y francesa son adaptaciones naturales, no traducciones literales.
La formulación no presupone que el usuario cocine para otra persona: el menú puede ser
para sí mismo o para cualquier otro público configurado.

## Comportamiento

No cambia el comportamiento de la pantalla. El contador diario mantiene el placeholder `%1$d` y continúa mostrando los usos restantes. La IA permanece explícita en el texto introductorio aunque el botón use una acción más conversacional.

## Validación

- Comprobar las seis claves exactas en los tres idiomas.
- Verificar que `%1$d` se conserve en `dice_roll_ai_with_count`.
- Ejecutar las pruebas unitarias de la aplicación.
- Ejecutar `lintRelease` y compilar el bundle de producción.
- Revisar visualmente el bloque en español y, si hay dispositivo disponible, en inglés y francés con tamaño de fuente normal y ampliado.

## Riesgos

- Los títulos y etiquetas nuevas son algo más largos; deben revisarse con fuente ampliada.
- La IA ya no aparece en el botón, pero sigue claramente identificada en el texto introductorio para evitar ocultar cómo se genera la idea.
