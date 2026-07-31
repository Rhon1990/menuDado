# Buscador y filtros del catálogo de menús

Fecha: 2026-07-31
Estado: diseño aprobado pendiente de revisión documental

## Objetivo

Añadir búsqueda y filtros a las pantallas que muestran colecciones completas de menús, con el mínimo espacio permanente y el lenguaje visual existente de MenuDado. La solución debe servir para entradas desde Adulto, Peques, Bebé y Favoritos, respetar el ámbito fijo de cada entrada y descartar siempre el estado transitorio al abandonar la pantalla.

## Diseño visual aprobado

### Barra compacta conectada a la cabecera

- Mostrar una franja verde inmediatamente debajo de la marca y unida a la cabecera.
- Dentro de la franja, presentar:
  - un campo crema para buscar por plato o ingrediente;
  - un botón de filtros independiente, con objetivo táctil mínimo de 48 dp;
  - un distintivo terracota con la cantidad de filtros activos, oculto cuando no exista ninguno.
- La franja solo se muestra en las pantallas de catálogo de menús. No aumenta la altura de la cabecera en Inicio, Perfil, Mercado, Mi zona o Acerca de.
- La barra forma parte del contenido superior y desaparece al avanzar por la lista; no queda fija para no reducir el área útil.

### Hoja verde de filtros

El botón abre una hoja modal inferior que reutiliza `MenuDadoActionSheet`:

- fondo `MenuDadoColors.HeaderGreen`;
- esquinas superiores de 28 dp;
- tirador y divisor blancos translúcidos;
- botón de cierre de 48 dp;
- título y contenido blancos;
- iconos blancos dentro de círculos blancos translúcidos;
- cierre mediante X, Back o toque fuera.

La lista principal se adapta al ámbito actual.

En Adulto, Peques o Bebé contiene:

1. `Necesidad alimentaria`, con la selección actual como descripción.
2. `Solo favoritos`, con interruptor.
3. `Solo saludables`, con interruptor.

En Favoritos contiene:

1. `Para quién`, con la selección actual como descripción.
2. `Necesidad alimentaria`, con la selección actual como descripción.
3. `Solo saludables`, con interruptor.

Favoritos no muestra `Solo favoritos` porque esa condición ya forma parte del ámbito fijo. En la pantalla Favoritos, `Para quién` abre una sublista verde de selección única:

- Todos;
- Adulto;
- Peques;
- Bebé.

`Necesidad alimentaria` abre otra sublista de selección única:

- Ninguna;
- Embarazo;
- Vegano;
- Mis alergias;
- Todo mi perfil.

La opción elegida se identifica con un control de selección visible. Al elegirla se vuelve a la lista principal y los resultados se actualizan inmediatamente; no existe un botón adicional `Aplicar`.

## Alcance del catálogo

La misma experiencia se usa al entrar desde cualquier colección completa de menús:

- Adulto;
- Peques;
- Bebé;
- Favoritos.

La entrada define un ámbito base fijo que los filtros nunca pueden ampliar:

- Adulto contiene únicamente menús con `FoodMenu.audience == ADULT`;
- Peques contiene únicamente menús con `FoodMenu.audience == CHILD`;
- Bebé contiene únicamente menús con `FoodMenu.audience == BABY`;
- Favoritos contiene únicamente menús con `FoodMenu.isFavorite == true`.

Los menús de un perfil desactivado continúan excluidos por la regla de seguridad existente. En Favoritos sí se puede reducir el ámbito por público, pero elegir `Todos` sigue significando todos los favoritos, no todos los menús de la app. Para pasar de Adulto a Bebé se vuelve a Inicio y se entra mediante `Ver más` de Bebé.

## Reinicio obligatorio al entrar

La búsqueda y los filtros son estado de presentación local, no preferencias persistentes.

Cada nueva entrada al catálogo debe:

- vaciar el texto de búsqueda;
- restaurar `Necesidad alimentaria` a `Ninguna`;
- desactivar `Solo saludables`;
- desactivar `Solo favoritos` en Adulto, Peques o Bebé;
- restaurar `Para quién` a `Todos` en Favoritos;
- descartar cualquier selección temporal de una visita anterior.

Por tanto, Favoritos se vuelve a abrir mostrando todos los favoritos y Adulto se vuelve a abrir mostrando todos los menús de Adulto, pero ninguna de las dos entradas conserva búsquedas, necesidades o filtros activados manualmente en la visita anterior. Back, cambio de destino o una nueva apertura deben producir el mismo reinicio. El ámbito base no cuenta como filtro activo ni aparece en el distintivo.

## Semántica de búsqueda

La búsqueda es local, inmediata y no realiza llamadas de red ni de IA. Debe ignorar mayúsculas, minúsculas y acentos y consultar:

- nombre;
- ingredientes o descripción;
- notas;
- inspiración culinaria;
- nombres de productos de Mercado asociados al menú.

Los grupos sin resultados desaparecen. Los menús restantes mantienen el orden actual por `createdAt` descendente dentro de Desayuno, Almuerzo y Cena. El contador de cabecera refleja únicamente los resultados visibles.

## Semántica de filtros

Los filtros se combinan mediante intersección:

1. ámbito base de entrada;
2. texto de búsqueda;
3. público, solo dentro de Favoritos;
4. necesidad alimentaria;
5. favoritos, solo dentro de Adulto, Peques o Bebé;
6. saludable.

### Público

Adulto, Peques y Bebé usan el campo persistido `FoodMenu.audience`, por lo que sus ámbitos son deterministas. La selección adicional `Para quién` solo existe en Favoritos y nunca incorpora menús no favoritos.

### Necesidad alimentaria

- `Ninguna`: no añade una restricción alimentaria.
- `Embarazo`: solo está disponible en Adulto o en Favoritos filtrado por Adulto y descarta recetas con conflictos detectados para embarazo.
- `Vegano`: descarta recetas con alimentos no veganos detectados.
- `Mis alergias`: usa los alérgenos configurados para el público del ámbito. En Favoritos con `Todos`, evalúa cada menú contra el perfil de su propio público.
- `Todo mi perfil`: aplica veganismo, embarazo cuando corresponda, alérgenos y otros alimentos a evitar del perfil asociado a cada menú dentro del ámbito fijo.

En Favoritos, si el usuario cambia desde Adulto a Peques, Bebé o Todos mientras `Embarazo` está activo, la necesidad vuelve a `Ninguna`. `Mis alergias` se muestra deshabilitado cuando el ámbito elegido no contiene ningún alérgeno configurado.

Embarazo, veganismo y alergias significan `sin conflictos detectados en la información guardada de la receta`; no significan que el menú fuera creado históricamente bajo esa configuración ni constituyen una certificación médica. No se añadirá una migración Room ni una instantánea de perfil al menú en este alcance.

### Favoritos y saludable

- `Solo favoritos` usa `FoodMenu.isFavorite`.
- `Solo saludables` incluye únicamente menús con `HealthStatus.HEALTHY`; excluye Intermedio, No saludable y Sin analizar.

## Estado vacío y errores

Cuando no existan coincidencias:

- mostrar `No encontramos menús`;
- explicar de forma breve que se pruebe otra búsqueda o filtro;
- ofrecer `Limpiar búsqueda y filtros`;
- restaurar el contexto inicial de la entrada, no un estado global arbitrario.

La búsqueda y el filtrado son operaciones locales puras. Un fallo de persistencia o red no participa en este flujo. Si la información de un menú es incompleta, el filtro alimentario solo puede evaluar el texto disponible y nunca debe presentar el resultado como garantía clínica.

## Arquitectura y reutilización

- Compartir la experiencia de búsqueda entre `MenuAudienceDetailScreen` y `FavoriteMenusDetailScreen` sin eliminar sus ámbitos diferenciados.
- Mantener como fuente `menuVisibleMenus(...)` y aplicar después el ámbito fijo de entrada para respetar públicos activos y aislamiento de cuenta.
- Modelar explícitamente el ámbito de entrada para que ninguna combinación de filtros pueda ampliarlo.
- Modelar el estado transitorio con un contrato pequeño: consulta, público, necesidad, favoritos y saludable.
- Mantener las funciones de filtrado y reinicio como funciones puras para probarlas sin Compose.
- Exponer en `MenuDadoUiState` los perfiles alimentarios necesarios por público, sin sustituir el perfil seleccionado que usa la pantalla Perfil.
- Reutilizar la compatibilidad determinista de `DietaryProfileCompatibility`; extraer únicamente la representación común necesaria para evaluar también `FoodMenu`.
- Extender `Header` con contenido inferior opcional para conectar la barra al mismo fondo verde sin duplicar la marca.
- Reutilizar `MenuDadoActionSheet` y ampliar de forma compatible su fila para admitir valor secundario, selección o interruptor. Las hojas de acciones y Mercado no deben cambiar de aspecto ni comportamiento.
- No introducir nuevas dependencias, navegación externa, llamadas de IA, lecturas Firebase, cambios de Room ni cambios de Firestore.

## Localización, accesibilidad y privacidad

- Añadir todos los textos en español, inglés y francés.
- El campo debe indicar claramente su propósito y ofrecer acción accesible para borrar el texto.
- El botón de filtros anuncia si existen filtros activos.
- Filas, cierre, campo y controles tienen objetivos táctiles mínimos de 48 dp.
- Las selecciones no dependen exclusivamente del color.
- El foco de las sublistas sigue su orden visual y anuncia la opción seleccionada.
- No se envían a Analytics texto de búsqueda, recetas, alergias, embarazo, alimentos a evitar ni datos de perfil. Cualquier medición nueva, si se mantiene, será exclusivamente categórica.

## Pruebas y QA

### Pruebas unitarias

- búsqueda insensible a mayúsculas y acentos en todos los campos admitidos;
- combinación de texto, público, necesidad, favoritos y saludable;
- filtrado determinista de Adulto, Peques y Bebé;
- compatibilidad de embarazo, veganismo, alérgenos y perfil completo;
- `Embarazo` se limpia al cambiar el público de Favoritos desde Adulto a Peques, Bebé o Todos;
- ausencia de alérgenos configurados;
- orden y agrupación después de filtrar;
- contador y estado vacío;
- reinicio desde cada contexto de entrada;
- exclusión de menús pertenecientes a públicos inactivos.

### Pruebas Compose

- barra visible solo en catálogos de menús;
- apertura y cierre de la hoja mediante botón, X, Back y toque exterior;
- navegación entre lista principal y sublistas;
- indicador de selección única;
- interruptores de Favoritos y Saludables;
- distintivo con cantidad de filtros activos;
- limpieza del texto;
- estado vacío y restauración al contexto inicial;
- nueva entrada sin filtros residuales;
- ausencia de cambios visuales en `MenuActionsSheet` y `MarketManagementSheet`.

### Verificación manual

- pantalla estrecha y texto grande;
- teclado abierto, escritura y borrado;
- entrada separada en Adulto, Peques, Bebé y Favoritos;
- Favoritos → público Adulto → Todos sin mostrar nunca un menú no favorito;
- perfiles con y sin embarazo, veganismo, alérgenos y alimentos a evitar;
- menús manuales con información incompleta;
- español, inglés y francés;
- TalkBack y áreas táctiles.

Ejecutar las pruebas unitarias relacionadas, la suite unitaria de la app y `assembleDebug`. Si el entorno lo permite, ejecutar también las pruebas instrumentadas de la hoja y el catálogo.

## Documentación

Tras implementar y verificar, actualizar `docs/project-context.md` para describir:

- la barra compacta del catálogo;
- los puntos de entrada compatibles;
- el ámbito fijo y el reinicio por entrada;
- la semántica exacta de público y necesidad alimentaria;
- la limitación de compatibilidad basada en la información guardada.

## Fuera de alcance

- Persistir búsquedas o filtros entre visitas.
- Guardar en cada menú una instantánea histórica del perfil alimentario.
- Certificar clínicamente una receta como apta.
- Buscar en contenido remoto o generar resultados con IA.
- Cambiar la creación, edición, sincronización o borrado de menús.
- Permitir que un filtro amplíe el ámbito fijo de Adulto, Peques, Bebé o Favoritos.
- Modificar Inicio, Perfil, Mercado o Mi zona fuera de sus enlaces actuales al catálogo.
- Desplegar Firebase, publicar en Play Console o generar una release.
