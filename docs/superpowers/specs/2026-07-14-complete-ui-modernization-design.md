# Diseño: modernización visual completa de MenuDado

## Objetivo

Renovar todas las pantallas de MenuDado con una interfaz más limpia, moderna y sencilla que facilite la primera acción y resulte cómoda para usuarios recurrentes. La dirección aprobada es **Calma editorial**: superficies crema, jerarquía tipográfica clara, espacio generoso, verde para marca y selección y terracota para la acción principal.

El rediseño toma como referencia la paleta de la imagen aportada por el usuario y conserva la identidad oficial de MenuDado.

## Resultado de diseño aprobado

- Fondo crema dominante y superficies cálidas de bajo contraste.
- Una sola acción visualmente dominante por contexto.
- Verde para identidad, selección, navegación activa y estados positivos.
- Terracota suave para la siguiente acción principal.
- Tarjetas amplias, bordes suaves y elevación mínima.
- Navegación, formularios, modales y estados con el mismo lenguaje visual.
- Todas las pantallas entran en alcance.

## Alcance

El sistema se aplicará a:

- Onboarding.
- Cabecera y navegación inferior.
- Inicio y bloque `Qué comer hoy`.
- Generación IA, selección entre menús y creación manual.
- Menú reciente, carruseles, favoritos y vistas por público.
- Resultado generado, detalle, edición, acciones y confirmaciones.
- Estados vacío, cargando, bloqueado, error y cuota de IA.
- Perfil alimentario.
- Mi zona, registro, inicio de sesión y cuenta sincronizada.
- Acerca de la app y privacidad cuando esté disponible.
- Diálogos, hojas de acciones, campos, filtros y botones compartidos.

## Fuera de alcance

- Cambiar ViewModels, repositorios, Room, Firebase, modelos o contratos de datos por motivos puramente visuales.
- Alterar destinos, reglas de navegación o callbacks existentes.
- Añadir llamadas a Firebase AI Logic, reintentos o consumo adicional.
- Cambiar eventos o parámetros de Analytics.
- Añadir dependencias de UI o una fuente descargable.
- Generar imágenes con IA.
- Introducir animaciones decorativas que retrasen una acción.

## Situación actual

La app ya usa Jetpack Compose y una paleta cercana a la referencia. `MenuDadoTheme.kt` define colores, pero todavía delega tipografía y formas en los valores por defecto. Las pantallas repiten radios pequeños, estilos y decisiones cromáticas dentro de `MenuDadoScreen.kt`; autenticación vive en `MenuDadoAuthScreen.kt` con variantes propias.

La modernización debe consolidar esos patrones sin reescribir el estado ni la lógica. `docs/project-context.md` seguirá siendo el índice funcional, pero los componentes y destinos reales se validarán contra código.

## Sistema visual

### Roles de color

- `BrandGreen`: `#2F765D`, identidad, selección y navegación activa.
- `SelectionGreen`: `#E7F0EB`, fondos seleccionados o positivos de baja intensidad.
- `ActionTerracotta`: `#D66548`, acción principal del contexto.
- `Background`: `#FFF9EC`, fondo general.
- `Surface`: `#FFFCF4`, tarjetas, diálogos y navegación flotante.
- `SoftSand`: `#F2DFC1`, separación, bordes y estados neutros.
- `Ink`: `#263238`, texto principal.
- `MutedInk`: `#5F6B66`, texto secundario.

`Tomato` se conserva para error, eliminación o estado no saludable. No se reutilizará como color semántico de éxito ni se confundirá con `ActionTerracotta`.

Todo color nuevo tendrá un rol semántico; las pantallas no introducirán variantes locales sin justificar.

### Tipografía

Se utilizará la familia del sistema para evitar dependencias y mantener buen rendimiento. El `Typography` de Material 3 se configurará explícitamente:

- Display: 32 sp, peso fuerte, interlineado compacto.
- Encabezado: 24 sp.
- Título de sección: 20 sp.
- Título de componente: 16 sp.
- Cuerpo principal: 16 sp.
- Cuerpo secundario: 14 sp.
- Etiqueta: 12 sp.

El contenido debe crecer con la escala de fuente del sistema; no se fijarán alturas que recorten texto traducido o ampliado.

### Formas, espaciado y elevación

- Escala de espaciado: 8, 12, 16, 24 y 32 dp.
- Tarjetas y superficies principales: radio de 24 dp.
- Campos y botones: radio de 16 dp.
- Filtros: forma de píldora.
- Navegación flotante: radio de 20 dp.
- Sombra solo en navegación flotante, bottom sheets y superficies realmente superpuestas.
- El resto usa bordes suaves para separar contenido.

## Arquitectura de UI

La lógica y el flujo de estado permanecen sin cambios:

`ViewModel state/callbacks -> composables de pantalla -> componentes visuales compartidos -> Material 3`

### Theme

`MenuDadoTheme.kt` concentrará colores semánticos, tipografía y formas. El tema será la fuente única para estilos globales.

### Componentes compartidos

Se reutilizarán o extraerán primitivas pequeñas cuando al menos dos pantallas compartan el patrón:

- Tarjeta de sección.
- Botón principal y botón secundario.
- Selector tipo chip.
- Estilo de campos de texto.
- Encabezado de sección.
- Aviso inline.
- Estado vacío.
- Cabecera y navegación inferior.
- Contenedor de diálogo o bottom sheet.

No se creará un wrapper si Material 3 ya resuelve el caso con colores, forma y modifier compartidos. La extracción debe reducir duplicación sin ocultar callbacks ni state hoisting.

### Pantallas existentes

Los composables actuales adoptarán el sistema visual manteniendo firmas, callbacks y estado. `MenuDadoScreen.kt` no se dividirá como refactor independiente; solo se extraerán componentes visuales que sean necesarios y reutilizables para este alcance. `MenuDadoAuthScreen.kt` consumirá los mismos estilos de campos y botones.

## Diseño por flujo

### Onboarding

- Marca centrada dentro de una superficie verde redondeada.
- Una única promesa de valor.
- `Crear mi primer menú` como CTA terracota dominante.
- `Explorar por mi cuenta` como acción textual.
- Confianza visible: sin registro y datos bajo control.

### Inicio

- Cabecera más compacta para priorizar contenido.
- Título orientado a tarea: decidir qué comer.
- Tipo de comida, público e ingredientes agrupados en una tarjeta.
- Generación IA como CTA principal.
- Elegir guardados y escribir manualmente permanecen visibles como opciones secundarias.
- Menú reciente y colección se separan mediante encabezados y espacio, no mediante grandes bloques de color.

### Menús guardados

- Favoritos primero.
- Agrupación por público actual sin cambiar filtros ni orden funcional.
- Portadas con mayor protagonismo y acciones secundarias discretas.
- `Ver más` mantiene la navegación existente a la vista completa del público.

### Resultado y detalle

- Bottom sheet o diálogo amplio con lectura por bloques.
- Nombre, contexto y calorías antes de detalles secundarios.
- Evaluación saludable en superficie semántica.
- `Guardar en mis menús` como CTA terracota.
- `Probar otra idea` secundaria y `Descartar` terciaria.
- Edición, foto, compartir y eliminar conservan el comportamiento actual.

### Perfil alimentario

- Público en chips superiores.
- Opciones agrupadas por intención.
- Switches con explicación breve.
- Alérgenos en chips accesibles.
- Campos libres al final del grupo relacionado.

### Mi zona, acceso y acerca de

- Hero verde compacto para identidad y estado de cuenta.
- Registro e inicio de sesión comparten estilos con el resto de formularios.
- Google, correo y contraseña mantienen validaciones actuales.
- Datos sincronizados, Acerca de, privacidad y cierre de sesión se organizan como filas consistentes.

La navegación inferior real conserva `Inicio`, `Perfil` y `Mi zona`. `Acerca de` sigue abriéndose desde `Mi zona`.

## Estados y errores

### Carga IA

Se mantiene el overlay bloqueante y el dado animado para impedir doble toque. El fondo y la tarjeta adoptan el nuevo sistema, con un mensaje breve y sin añadir solicitudes.

### Vacío

Cada vacío explicará qué falta y ofrecerá una única recuperación principal. No se añadirán ilustraciones remotas ni llamadas externas.

### Acción bloqueada

El motivo se muestra junto al control relacionado: falta de tipo, público, perfil o pausa real de IA. El color acompaña al texto; nunca será la única señal.

### Error recuperable

El mensaje describe que la acción no pudo completarse y ofrece reintento solo cuando el flujo actual lo permite. No se atribuye automáticamente el fallo a la conexión.

### Offline

Las funciones locales siguen disponibles. Solo las acciones que necesitan IA o sincronización comunican su limitación.

## Accesibilidad y adaptabilidad

- Contraste objetivo mínimo de 4.5:1 para texto normal y 3:1 para texto grande e indicadores gráficos relevantes.
- Áreas táctiles mínimas de 48 dp.
- Estados comunicados mediante texto o icono además del color.
- Descripciones de contenido en iconos accionables.
- Orden de foco coherente con el orden visual.
- Scroll disponible con teclado, traducciones largas y fuente ampliada.
- Validación mínima en ancho compacto de 360 dp y con fuente grande.
- Textos nuevos o modificados disponibles en español, inglés y francés.

## Analítica y medición de producto

No se añaden eventos en este alcance. Los eventos existentes permiten observar el impacto sin enviar contenido personal:

- Onboarding mostrado y completado.
- Primer menú creado.
- Inicio y resultado de generación IA.
- Guardar, probar otra idea y descartar resultado generado.
- Apertura de menús y navegación por CTA.

La mejora de captación o retención no se expresará como porcentaje garantizado. Se compararán las tasas posteriores al despliegue con un periodo equivalente anterior.

## Estrategia de implementación

La entrega se hará por capas pequeñas para reducir riesgo:

1. Añadir pruebas de contrato para tokens y helpers visuales antes de cambiar implementación.
2. Configurar theme, tipografía, formas y roles cromáticos.
3. Crear únicamente los componentes compartidos demostrados por uso real.
4. Migrar onboarding, cabecera y navegación.
5. Migrar Inicio, menús y detalles.
6. Migrar perfil, Mi zona, autenticación, acerca de y estados auxiliares.
7. Actualizar `docs/project-context.md` con el sistema final.
8. Ejecutar validación automatizada y recorrido manual completo.

Cada bloque conservará callbacks y state hoisting. No se mezclará con cambios del prompt IA salvo que el plan indique una tarea separada y verificable.

## Estrategia QA

### Automatizada

- Pruebas unitarias de roles cromáticos, formas y helpers visuales extraídos.
- Pruebas existentes de navegación, onboarding, tarjetas, autenticación, ViewModel, Analytics y generación IA.
- Compilación Kotlin.
- Ensamblado de la variante aplicable.
- Lint comparado con la línea base conocida; incidencias preexistentes se separan de regresiones nuevas.

### Manual guiada

- Onboarding: comenzar y explorar sin cuenta.
- Inicio: IA, selección guardada y creación manual.
- Resultado: guardar, regenerar y descartar.
- Menús: favorito, detalle, editar, foto, compartir, eliminar y ver más.
- Perfil: públicos, switches, alérgenos y texto libre.
- Mi zona: invitado, registro, inicio de sesión, cuenta, acerca de y cierre.
- Estados de carga, vacío, bloqueo, error y cuota.
- Pantalla de 360 dp, teclado abierto, español/inglés/francés y fuente ampliada.

## Criterios de aceptación

- Todas las pantallas usan la dirección `Calma editorial` aprobada.
- Los colores tienen roles consistentes y el terracota principal no sustituye el rojo de error.
- Tipografía, formas y espaciado vienen del sistema compartido.
- Existe una sola acción visualmente dominante por contexto.
- La navegación conserva `Inicio`, `Perfil` y `Mi zona` y no cambia comportamientos.
- Firebase, Room, Analytics, ViewModels y contratos de datos no sufren cambios visuales indirectos.
- No aumentan las llamadas IA ni se añaden dependencias.
- Las áreas táctiles, contraste y fuente ampliada cumplen el contrato de accesibilidad.
- Las pruebas nuevas y existentes quedan en verde y el recorrido manual no detecta regresiones críticas.

## Riesgos y mitigación

- **Alcance amplio:** migración por componentes y pantallas con verificación tras cada bloque.
- **Regresión funcional al mover UI:** preservar firmas, callbacks y state hoisting; pruebas antes y después.
- **Abstracción excesiva:** extraer solo patrones usados al menos dos veces y mantener APIs pequeñas.
- **Texto recortado:** evitar alturas rígidas y validar traducciones y fuente grande.
- **Contraste insuficiente en tonos cálidos:** verificar ratios antes de aceptar cada combinación.
- **Confusión entre CTA y error:** separar `ActionTerracotta` de `Tomato`.

## Justificación técnica

El enfoque moderniza toda la experiencia desde una única capa visual y reutilizable, evitando cambios en la lógica estable. Los roles semánticos reducen inconsistencias futuras; la migración incremental limita regresiones y permite validar cada pantalla. Mantener Material 3 y la fuente del sistema evita dependencias, coste de carga y mantenimiento innecesario.
