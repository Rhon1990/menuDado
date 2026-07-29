# Rotacion mundial de cocinas para ideas IA

## Objetivo

Hacer que las ideas generadas por MenuDado sorprendan con inspiraciones culinarias de distintas partes del mundo, evitando que Gemini repita los mismos formatos, sin aumentar el consumo de tokens, el numero de llamadas ni el esquema JSON actual.

## Principios de producto

- La variedad debe ser controlada por la app y no depender de que el modelo elija espontaneamente una cocina diferente.
- Las recetas deben sentirse nuevas, pero seguir siendo saludables, sencillas y posibles con ingredientes comunes de supermercado.
- La inspiracion cultural orienta el sabor, la combinacion y la tecnica; no obliga a reproducir una receta tradicional si contradice el tipo de comida, el publico o el perfil alimentario.
- Seguridad, alergias, restricciones, edad, embarazo, tipo de comida e ingredientes solicitados mantienen prioridad sobre la variedad.
- El nombre y el contenido generado siguen usando el idioma visible de la app.

## Catalogo culinario

La primera version usa un catalogo local y cerrado de inspiraciones:

- mediterranea
- italiana
- griega
- espanola
- mexicana
- andina o peruana
- caribena
- brasilena
- india
- levantina
- magrebi
- africana occidental
- japonesa
- coreana
- sudeste asiatico
- nordica

Los identificadores son internos, estables y no se muestran directamente en la interfaz.

## Rotacion local

Un componente pequeno y reutilizable selecciona la siguiente inspiracion antes de generar:

- mantiene una secuencia independiente por combinacion de tipo de comida y publico;
- empieza en un desplazamiento local para que todos los usuarios no reciban la misma primera cocina;
- no repite una inspiracion hasta recorrer el catalogo completo;
- persiste el siguiente indice en preferencias locales para conservar la variedad tras cerrar la app;
- solo avanza la rotacion cuando Gemini devuelve una idea valida;
- un error o timeout conserva la inspiracion pendiente para que el reintento no altere el comportamiento de forma silenciosa.

La rotacion no se guarda en Room ni Firestore porque es un detalle local de generacion y no forma parte del menu del usuario.

## Flujo de datos

1. El `ViewModel` valida tipo, publico, perfil, ingredientes, cuota y throttle como hasta ahora.
2. Obtiene del selector local la inspiracion pendiente para esa combinacion.
3. La inspiracion cruza el repositorio y el analizador como un valor tipado.
4. `MenuGenerationPrompt` incluye una unica linea compacta con esa inspiracion.
5. Gemini responde con el mismo objeto JSON existente.
6. Tras parsear una respuesta valida, el `ViewModel` confirma el avance de la rotacion y recuerda la idea para evitar repeticiones.
7. Si la llamada o el parseo falla, no se confirma el avance.

## Contrato de tokens y coste

La funcionalidad no puede aumentar el uso de tokens de IA:

- se mantiene exactamente una llamada `generateContent` por intento valido;
- no se agrega una segunda consulta para escoger cocina;
- no se agrega ningun campo al JSON de respuesta;
- no cambia la cantidad de ideas previas enviadas, actualmente limitada a ocho;
- la linea larga actual que enumera cremas, sopas, bowls, wraps y otros formatos se elimina;
- se reemplaza por una linea mas corta: `Inspiracion culinaria: <tema>.`;
- para los mismos datos de usuario, el prompt completo nuevo debe tener una longitud menor o igual que el prompt anterior;
- la estructura y longitud esperada de la respuesta permanecen iguales.

La seleccion, persistencia y rotacion del tema ocurren completamente en el dispositivo y no consumen tokens.

## Compatibilidad y seguridad

- Bebes y peques reciben adaptaciones seguras aunque la cocina original use ingredientes, texturas o niveles de sal no adecuados.
- Perfiles veganos, alergias, embarazo y otras exclusiones prevalecen sobre la inspiracion.
- Los ingredientes base del usuario siguen siendo obligatorios cuando son compatibles.
- Las cenas conservan el limite actual de tiempo, ingredientes y esfuerzo.
- No se cambia la cuota diaria, el throttle, el timeout ni el modelo Gemini.

## Pruebas

### Selector local

- entrega todas las inspiraciones sin repetir antes de completar el ciclo;
- mantiene secuencias independientes por tipo de comida y publico;
- persiste el indice entre instancias;
- no avanza ante una generacion fallida;
- vuelve a iniciar el ciclo correctamente.

### Prompt

- contiene solo la inspiracion seleccionada;
- elimina la lista generica anterior;
- mantiene prioridades de seguridad y restricciones;
- conserva el esquema JSON sin campos nuevos;
- mantiene o reduce la longitud total frente al prompt anterior con entradas equivalentes.

### ViewModel e integracion

- transmite la inspiracion en una unica llamada;
- confirma la rotacion solo tras una respuesta valida;
- conserva la inspiracion ante error;
- mantiene el historial de ocho ideas a evitar;
- no altera cuota, throttle, timeout, idioma ni flujo de revision antes de guardar.

## Fuera de alcance

- Mostrar filtros o nombres de cocinas en la interfaz.
- Permitir que el usuario elija manualmente una cocina.
- Generar imagenes o consultar catalogos externos.
- Guardar la cocina como nuevo campo del menu o sincronizarla con Firestore.
- Cambiar el modelo, la cuota o el limite de tokens de salida.

## Criterios de aceptacion

- Generaciones consecutivas recorren inspiraciones mundiales sin repetir antes de completar el catalogo.
- Las recetas siguen respetando publico, tipo, perfil alimentario e ingredientes solicitados.
- Solo existe una llamada a Gemini por generacion.
- El JSON de respuesta no cambia.
- El prompt nuevo no supera la longitud del prompt anterior con las mismas entradas.
- Tests unitarios, build debug y prueba manual del flujo de generacion pasan correctamente.
