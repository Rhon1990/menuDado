# Diseño de lista de mercado inteligente

## Objetivo

Añadir a MenuDado una lista de mercado persistente y sincronizada que transforme los productos de los menús procesados por IA en una utilidad cotidiana. La experiencia debe requerir el mínimo esfuerzo, evitar cantidades, consolidar productos repetidos y conservar el control del usuario.

## Principios de producto

- La IA es la única fuente de productos. El usuario no escribe ni edita productos manualmente.
- Un menú generado con IA recibe sus productos en la misma solicitud que genera la receta.
- Un menú escrito manualmente recibe productos cuando el usuario ejecuta `Analizar IA`, en la misma solicitud que devuelve análisis saludable y calorías.
- El procesamiento por lote también devuelve productos para cada menú analizado.
- La lista no muestra cantidades ni unidades; cada fila contiene únicamente el nombre del producto.
- Guardar un menú generado muestra `Añadir a la lista de mercado` activado por defecto. El usuario puede desmarcarlo.
- Los productos generados quedan asociados al menú aunque inicialmente no se añadan a la lista global. Esto permite añadirlos después sin otra solicitud de IA.
- Un fallo al obtener productos no debe descartar un menú o análisis válido. La app conserva el resultado disponible e informa que la lista no pudo prepararse.

## Enfoques considerados

### Lista completamente automática

Añadir todos los productos sin confirmación reduce acciones, pero puede llenar la lista con menús que el usuario solo quería explorar. Se descarta por pérdida de control y ruido.

### Listas o viajes múltiples

Permite planificar compras separadas, pero añade nombres, fechas, selección de lista y mantenimiento antes de validar la utilidad principal. Se descarta para esta versión.

### Lista global consolidada vinculada a menús

Cada menú conserva sus productos y el usuario decide si activarlos. La pantalla `Mercado` reúne los productos activos sin duplicados. Este es el enfoque elegido porque combina automatización, control y una razón recurrente para volver a MenuDado.

## Experiencia de usuario

### Menú generado con IA

1. La respuesta de IA incluye nombre, receta, análisis, calorías y `shopping_products`.
2. El modal de detalle muestra una tarjeta `Lista para mercar` con nombres de productos.
3. Debajo aparece `Añadir a la lista de mercado`, activado por defecto.
4. Al guardar el menú, sus productos se persisten siempre. Solo se activan en la lista global si el control está marcado.
5. Si la IA no devuelve productos válidos, el menú sigue disponible y la tarjeta muestra una recuperación breve.

### Menú escrito manualmente

1. El menú se guarda inicialmente sin productos.
2. Al pulsar `Analizar IA`, la respuesta incluye análisis, calorías y `shopping_products`.
3. Tras un resultado válido, la app muestra los productos y ofrece añadirlos a la lista global.
4. Los menús antiguos analizados antes de esta función, o con una respuesta sin productos, muestran `Crear lista de mercado con IA`. Esta acción es explícita y consume una solicitud de IA.

### Detalle de un menú guardado

- Si tiene productos inactivos, muestra `Añadir a la lista de mercado`.
- Si tiene productos activos, muestra `Quitar de la lista de mercado`.
- Si no tiene productos, muestra `Crear lista de mercado con IA`.
- Quitar un menú desactiva sus relaciones sin borrar el menú ni su lista original.
- Eliminar un menú elimina sus relaciones. Un producto global permanece si otro menú activo todavía lo utiliza.
- Reanalizar o editar mediante IA reemplaza la lista original. Los productos sin cambios conservan su estado; los nuevos empiezan pendientes.

### Cobertura en tarjetas

- El bloque existente `Productos para este menú` es la única referencia visual y funcional; no se crea una variante nueva.
- Las tarjetas del carrusel `Favoritos` muestran ese bloque cuando el menú tiene productos.
- Las tarjetas de `Ver más`, tanto en `Favoritos` como en cada público, muestran el mismo bloque.
- Los carruseles normales por público conservan su composición actual.
- El bloque muestra únicamente nombres de productos, sin cantidades, unidades ni marcas.
- Si el menú no tiene productos, la tarjeta no reserva espacio ni muestra un estado vacío.

### Pantalla Mercado

- Se añade `MERCADO` como cuarto destino persistente de la navegación inferior: `Inicio`, `Perfil`, `Mercado` y `Mi zona`.
- La cabecera muestra `Lista de mercado`, número de pendientes y número de menús activos.
- `Por comprar` muestra productos únicos ordenados alfabéticamente, con checkbox y sin cantidades.
- Al marcar un producto, se mueve a una sección plegada `Comprados`.
- `Comprados` permite restaurar productos y ofrece `Limpiar comprados`.
- Limpiar comprados retira esos productos del viaje actual. Volver a añadir un menú que los necesite los crea nuevamente como pendientes.
- El estado vacío explica que la lista se crea al guardar o analizar menús con IA y ofrece volver a Inicio.
- La lista funciona sin conexión. La sincronización remota ocurre después y no bloquea la interacción.

## Contrato de IA

### Generación individual

El JSON de generación añade:

```json
{
  "shopping_products": ["Pollo", "Aguacate", "Tomate"]
}
```

### Análisis individual

El JSON de análisis añade el mismo campo `shopping_products`.

### Análisis por lote

Cada elemento de `results` añade su propio `shopping_products`.

### Reglas del prompt

- Devolver entre 1 y 20 productos reales de supermercado.
- Usar nombres breves, comunes y en el idioma visible de la app.
- No incluir cantidades, unidades, pasos de preparación ni marcas.
- Usar preferentemente singular y una denominación consistente.
- Excluir agua y productos opcionales que no formen parte real de la receta.
- Respetar las restricciones alimentarias del menú.

### Parseo tolerante

- Nombre, receta y calorías conservan los requisitos actuales de validez.
- `shopping_products` se valida y normaliza de forma independiente.
- Productos vacíos, con saltos de línea, excesivamente largos o repetidos se descartan.
- La ausencia o malformación exclusiva de `shopping_products` produce una lista vacía, no el fallo completo del menú o análisis.

## Modelo de dominio

Se introduce `ShoppingProduct`:

- `key`: hash determinista y seguro para usar como clave local y como ID de documento remoto.
- `normalizedName`: forma normalizada usada para consolidar.
- `displayName`: nombre visible localizado procedente de IA.

`FoodMenu` y `GeneratedMenu` incorporan `shoppingProducts`.

El resultado de análisis deja de transportar solo `HealthAnalysis` y pasa a un contrato `MenuAiDetails` que contiene:

- `healthAnalysis`
- `shoppingProducts`

El análisis individual y por lote usan el mismo contrato para evitar caminos inconsistentes.

## Persistencia local

La base Room pasa de versión 10 a 11.

### `menu_shopping_products`

Relación persistente entre menú y producto:

- Clave primaria compuesta: `menuId`, `productKey`.
- `normalizedName`
- `displayName`
- `isActive`

Esta tabla conserva la lista original aunque el usuario no la active. Las relaciones activas forman la lista global.

### `market_product_states`

Estado global por producto:

- Clave primaria: `productKey`.
- `displayName`
- `isPurchased`
- `purchasedAt`
- `updatedAt`
- Campos de sincronización y tombstone equivalentes al patrón actual de menús.

Las escrituras que guardan un menú y sus productos, activan o desactivan una lista, marcan comprados o limpian productos deben ejecutarse en transacciones Room.

## Consolidación

- La clave normalizada usa `trim`, minúsculas, eliminación de diacríticos y compactación de espacios.
- `productKey` se calcula como SHA-256 de la forma normalizada para impedir caracteres inválidos o rutas accidentales en Firestore.
- No se intenta inferir equivalencia semántica entre palabras distintas.
- Variantes de mayúsculas, tildes y espacios se consolidan.
- Si un producto tiene varias relaciones activas, aparece una sola vez.
- Desactivar o eliminar un menú elimina el producto global solo cuando no quedan otras relaciones activas.
- Limpiar un producto comprado desactiva sus relaciones activas y crea el tombstone remoto correspondiente; su lista original continúa asociada al menú y puede añadirse de nuevo.
- Si se añade un menú que necesita un producto comprado o limpiado anteriormente, el producto vuelve a `Por comprar`.

## Sincronización Firestore

- Los documentos existentes `users/{uid}/menus/{menuId}` incorporan `shoppingProducts`, incluyendo clave, nombre visible y estado activo.
- Los estados globales se guardan en `users/{uid}/marketProducts/{productKey}`.
- Cada producto remoto incluye nombre visible, estado comprado, fechas y tombstone.
- Las mutaciones se serializan por `productKey`, igual que las mutaciones de menú se serializan por `menuId`.
- La app escribe primero en Room, marca pendiente y sincroniza después.
- Al iniciar sesión con una cuenta registrada, las listas locales se marcan como pendientes y se fusionan bajo el nuevo `uid`, igual que los menús.
- Si existen mutaciones locales pendientes, la hidratación remota no puede sobrescribirlas.
- Las reglas actuales bajo `users/{uid}/**` siguen aislando los datos por usuario.

## Errores y recuperación

- Fallo de IA completo: se conserva el comportamiento de error y cuota actual.
- Menú o análisis válido sin productos: éxito parcial con aviso no bloqueante y acción de reintento.
- Fallo local de persistencia: no se muestra confirmación de guardado.
- Fallo remoto: la lista continúa disponible localmente y queda pendiente de sincronización.
- Conflicto entre dispositivos: gana la mutación más reciente por `updatedAt`, sin sobrescribir mutaciones locales pendientes.
- El botón y los checkboxes bloquean dobles pulsaciones mientras su transacción está activa.

## Privacidad y analítica

Los eventos permitidos son:

- `market_list_opened`
- `market_menu_added`
- `market_menu_removed`
- `market_product_checked`
- `market_product_restored`
- `market_purchased_cleared`
- `market_ai_products_failed`

Solo se envían booleanos, conteos agregados, tipo de comida y público objetivo cuando ya estén permitidos por el contrato actual. Nunca se envían nombres de productos, recetas, nombres de menú, IDs ni datos del perfil alimentario.

## Localización y accesibilidad

- Todos los textos se añaden en español, inglés y francés.
- Los nombres generados respetan el idioma visible al ejecutar la IA.
- Los checkboxes tienen descripciones diferenciadas para comprar y restaurar.
- Las filas mantienen un objetivo táctil mínimo de 48 dp.
- El estado comprado no depende únicamente del tachado; incluye checkbox y sección explícita.
- La pantalla soporta escalado de fuente y listas largas.

## Validación QA

### Dominio e IA

- Parseo individual y por lote con listas válidas, ausentes, malformadas y duplicadas.
- Normalización de mayúsculas, tildes y espacios.
- La falta exclusiva de productos no invalida un menú o análisis válido.
- Generar o analizar produce productos en la misma llamada; no incrementa el consumo de IA de forma adicional.

### Room y repositorio

- Migración 10 a 11 preserva todos los menús existentes.
- Guardado atómico de menú y productos.
- Activación, desactivación, compra, restauración y limpieza.
- Consolidación cuando varios menús usan el mismo producto.
- Eliminación de un menú sin afectar productos usados por otro.
- Reaparición como pendiente al añadir un menú nuevo.

### Firestore

- Mapeo compatible con documentos antiguos sin productos.
- Sincronización pendiente, tombstones, hidratación y fusión al iniciar sesión.
- Un dispositivo no sobrescribe una mutación local pendiente de otro flujo.
- Las reglas impiden acceder a listas de otro `uid`.

### UI

- Modal de menú generado con control activado por defecto.
- Menú manual sin lista hasta ejecutar `Analizar IA`.
- Cuarto destino `Mercado`, estado vacío, pendientes y comprados.
- Persistencia tras cerrar y abrir la app.
- Uso sin conexión y sincronización posterior.
- Navegación atrás, escalado de fuente, TalkBack y pantallas compactas.

### Regresión

- Guardar, editar, eliminar, sincronizar y analizar menús conserva el comportamiento anterior.
- Cuota, throttle y timeout de IA no cambian.
- Perfil alimentario, favoritos, fotos, filtros y dado aleatorio no se ven afectados.

## Fuera de alcance

- Cantidades y unidades.
- Edición manual de productos.
- Listas con nombre, fecha, tienda o múltiples viajes.
- Catálogo global de productos o traducción retroactiva al cambiar el idioma.
- Compartir la lista, precios, presupuesto, inventario doméstico o integración con supermercados.
