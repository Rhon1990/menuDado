# Badge de cocina en el detalle generado por IA

## Objetivo

Mostrar en el modal de la idea generada la inspiración culinaria elegida localmente, para que el usuario entienda inmediatamente si la propuesta es mexicana, india, griega u otra cocina del catálogo mundial.

## Ubicación y jerarquía

- El título `Idea generada con IA` conserva una línea propia y todo el ancho disponible.
- Debajo aparece una fila responsive de metadatos en este orden: cocina, estado saludable y calorías.
- El badge de cocina usa fondo verde suave, texto verde oscuro, forma redondeada y altura visual coherente con los chips existentes.
- La fila permite salto automático de línea para cocinas largas y traducciones extensas, sin reducir la legibilidad del título.
- No se agrega información al hero porque ya prioriza tipo de comida, público y nombre del plato.

## Datos y estado

- `MenuDadoUiState` conserva temporalmente la `CuisineInspiration` usada en la idea visible.
- El `ViewModel` asigna la inspiración al recibir una generación válida.
- Descartar la idea, cambiar el contexto del formulario o limpiar el borrador elimina esa inspiración.
- `GeneratedMenuDetailDialog` recibe el valor como parámetro y solo muestra el badge cuando está disponible.
- La inspiración sigue siendo metadata efímera del borrador: no se agrega a `FoodMenu`, Room, Firestore ni al JSON de Gemini.

## Localización

- Se agregan recursos Android para el formato `Cocina %1$s`, `%1$s cuisine` y `Cuisine %1$s`.
- Las 16 inspiraciones tienen etiqueta visible en español, inglés y francés.
- El texto interno usado por el prompt permanece separado de la etiqueta visible.

## Coste y compatibilidad

- No se agrega ninguna llamada de IA.
- No se modifica el prompt ni el JSON de respuesta.
- No se consumen tokens adicionales.
- Las ideas generadas antes de esta versión o estados sin inspiración simplemente omiten el badge.

## Pruebas

- Una generación válida expone en el estado la misma inspiración enviada a Gemini.
- Un error no muestra una inspiración como si existiera una idea válida.
- Descartar o limpiar el borrador elimina la inspiración.
- El mapeo de las 16 inspiraciones apunta a recursos localizados.
- La fila responsive mantiene el orden cocina, salud y calorías.
- Tests unitarios completos y build debug finalizan correctamente.

## Criterios de aceptación

- El modal muestra `Cocina <inspiración>` debajo de `Idea generada con IA` y antes de salud/calorías.
- Los nombres largos no comprimen el título ni salen de la pantalla.
- Español, inglés y francés muestran textos naturales.
- Guardar, probar otra idea y descartar conservan su comportamiento.
- La cantidad de llamadas y tokens de Gemini no cambia.
