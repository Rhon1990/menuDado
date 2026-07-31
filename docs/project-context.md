# MenuDado - Contexto del Proyecto

## Visión

MenuDado es una app Android nativa para planificar menús de comida y elegir qué comer hoy cuando el usuario no sabe qué cocinar. La app mantiene comportamiento local-first con Room para funcionar sin conexión, sincroniza los datos guardados en Firebase Firestore mediante Firebase Auth y usa IA online para evaluar si un menú es saludable. La primera pantalla siempre es Inicio y la app entra por defecto como invitado; desde `Mi zona` el usuario puede crear cuenta o iniciar sesión con correo o Google para recuperar datos tras reinstalar.

## Nombre del Producto

- Nombre: MenuDado
- Significado: un menú elegido con un dado y un menú "dado" por la app.
- Tono: cercano, práctico, simple y de uso diario.
- Nomenclatura obligatoria: cualquier referencia nueva debe usar `MenuDado` para producto/proyecto y `menuDado` solo cuando el contexto requiera el nombre de carpeta local. No introducir nombres anteriores o alternativos en código, documentación, recursos, rutas de configuración ni textos visibles.

## Identidad Visual

### Concepto del Logo

El icono oficial de app usa el dado de comida sin wordmark. En cabeceras internas se usa la marca separada: dado sin fondo/icono de app y wordmark "MenuDado" como asset independiente. El dado muestra tres propuestas visuales: tostada con huevo y aguacate, ensalada y pasta. La media luna crema del fondo refuerza la idea de comida del día y elección rápida.

- Archivo vectorial: `assets/logo/menu-dado-logo.svg`.
- Archivo raster: `assets/logo/menu-dado-logo.png`.
- Recurso de icono Android: `app/src/main/res/drawable-nodpi/menu_dado_symbol.png`.
- Recursos de cabecera Android: `app/src/main/res/drawable-nodpi/menu_dado_symbol.png` y `app/src/main/res/drawable/menu_dado_wordmark.xml`.
- Splash screen: usa el logo completo `app/src/main/res/drawable/menu_dado_logo.png` centrado sobre fondo sólido `#2F765D`; en Android 12+ se oculta el icono nativo del sistema para evitar un splash previo duplicado.
- Uso recomendado: el SVG como fuente principal de marca y el PNG para previsualizaciones o exportaciones temporales.
- Estilo: ilustrado, cálido, apetitoso y cercano, con lectura clara como icono de app.

### Paleta de Colores

- Verde MenuDado: `#2F765D` - marca, cabecera, barra de estado, selección y acciones secundarias.
- Terracota de acción: `#D66548` - llamadas a la acción principales como generar, guardar, registrarse o empezar.
- Verde de selección: `#E7F0EB` - fondos suaves de beneficios, estados seleccionados y ayudas contextuales.
- Crema cálido: `#FFF7E7` - dado, fondos claros y contraste suave.
- Crema del dado de platos: `#FBF2DC` - fondo específico de las caras del dado ilustrado.
- Fondo app: `#FFF9EC` - fondo general cálido y limpio.
- Superficie: `#FFFCF4` - tarjetas principales con lectura clara.
- Superficie formulario: `#FFEBC7` - bloque de creación de menú.
- Arena suave: `#F2DFC1` - bordes discretos, estados neutros y separación sin sombras pesadas.
- Rojo tomate: `#E35D3E` - reservado para error, eliminación y cierre de sesión; no se usa como CTA principal.
- Sistema visual `Calma editorial`: tarjetas de 24 dp, controles de 16 dp, objetivos táctiles mínimos de 48 dp, tipografía jerárquica y barra inferior flotante sobre superficie cálida.
- Marrón contorno: `#9A6A45` - bordes y detalles secundarios.
- Marrón borde dado: `#C49460` - trazo fino de las caras del dado 3D.
- Aguacate: `#79A85B` - selección y estados saludables.
- Amarillo huevo: `#F4B43E` - desayuno, energía y estados intermedios.
- Tinta: `#263238` - texto principal.
- Tinta secundaria: `#5F6B66` - textos de apoyo.

## Funcionalidades Principales

### Idioma de la app

- La UI debe usar recursos Android y seguir el idioma del móvil cuando esté en español, inglés o francés; cualquier otro idioma debe caer en español.
- Las etiquetas visibles de público en español son `Persona adulta`, `Peques` y `Bebé`.
- La generación y el análisis con IA deben pedir nombre, descripción, notas, razón y sugerencia en el mismo idioma visible de la app para evitar mezclar idiomas en menús creados por IA.

1. Crear menús y decidir qué comer hoy.
   - Inicio abre con `¿No sabes qué preparar hoy?` y el apoyo `No pasa nada. Dinos para quién es el menú y la IA te ayudará con una idea saludable.`; la tarjeta de IA se presenta como `Encontremos algo rico`.
   - La ayuda de IA es el estado inicial porque es la acción con mayor adopción observada y reduce decisiones antes de recibir una idea saludable. Su CTA visible es `Ayúdame a elegir` y muestra los usos restantes.
   - El tipo de comida es obligatorio y viene sugerido automaticamente segun la hora local del movil: desayuno por la mañana, almuerzo al mediodia/tarde y cena por la noche; el usuario puede cambiarlo antes de guardar o generar.
   - El público objetivo también es obligatorio. El usuario puede elegir persona adulta, peques o bebé entre los públicos activos del perfil alimentario, y la última selección válida del formulario se conserva localmente entre aperturas.
   - Si solo hay un público activo, el selector se muestra siempre con ese público aunque la selección guardada sea otra. Si hay dos o más, restaura la última selección únicamente cuando continúa activa; sin historial válido queda vacío y obliga al usuario a elegir. La hidratación remota de perfiles debe refrescar este estado en el `ViewModel` para no conservar una selección incompatible ni una lista de públicos desactualizada tras sincronizar.
   - En el estado principal se muestra la selección de tipo y público, el campo `¿Qué tienes en casa? (Opcional)` con el ejemplo `Ej. tomate, arroz o pollo`, y el dado IA como acción principal. Al lanzarlo, MenuDado genera una idea saludable usando perfil alimentario, tipo de comida, público objetivo e ingredientes base.
   - Si el dado de IA está bloqueado por falta de tipo de comida, público objetivo o pausa de IA, el propio bloque del dado debe explicar el motivo fuera del botón del dado, usar un aviso cálido alineado con la paleta de MenuDado dentro del bloque amarillo, mantener el botón en un color neutral de marca claramente distinto del naranja activo y recordar que el usuario puede cambiar a `Escribir menú`. El aviso usa el naranja del dado solo como acento/título y deja el motivo en texto oscuro para mejorar legibilidad. Al agotar los usos gratuitos diarios, el botón no se bloquea: ofrece de forma opcional `Ver anuncio · obtener 1 idea más`, indica `Límite gratuito diario alcanzado` y solo lanza la generación cuando el anuncio entrega la recompensa. Ambos textos admiten salto de línea para mantenerse completos en pantallas estrechas.
   - Cuando la generación completa una idea, se abre un modal de detalle con nombre, descripción, notas, calorías, análisis saludable y sugerencia. El título diferencia discretamente el origen: una respuesta en vivo muestra `Idea generada con IA`; una recuperación desde la colmena muestra `Idea generada`, con equivalentes localizados en inglés y francés y sin añadir iconos, colores ni menciones visibles a la colmena. Debajo del título, el primer metadato identifica la inspiración culinaria seleccionada localmente (`Cocina mexicana`, `Cocina mediterránea`, etc.), seguido del estado saludable y las calorías en una fila responsive que puede saltar de línea. Al guardar la idea, esta inspiración se conserva como metadato opcional del menú en Room y Firebase para mostrarla después; no añade llamadas, tokens ni campos al JSON de Gemini. `Guardar en mis menús` es la acción principal; `Probar otra idea` reutiliza tipo, público e ingredientes base y respeta cuota, throttle y timeout; `Descartar` sigue disponible como acción terciaria.
   - Al tocar `Escribir mi menú` se muestran nombre del plato o menú, ingredientes o descripción y notas opcionales, además de `Guardar menú`; se puede volver a IA sin perder los selectores.
   - Si existen menús guardados, Inicio muestra `Tu último menú` con el menú de mayor `createdAt` y abre su detalle al tocarlo; no representa un borrador ni una edición pendiente.
   - Si el usuario cambia el tipo de comida o el público objetivo del formulario, se limpia el borrador actual del menú para evitar mezclar contenido de contextos distintos; editar los campos de texto conserva el resto del formulario.
   - Calorías estimadas opcionales.
   - Se guarda con una única acción principal: `Guardar menú`.
   - El análisis IA no forma parte del guardado inicial; se ejecuta después desde la tarjeta del menú guardado.
   - Tras persistir correctamente un menú nuevo en Room, la app muestra de inmediato un Snackbar breve y localizado confirmando `Menú guardado con éxito`; las validaciones bloqueadas o los límites de invitado no deben mostrar esa confirmación. La subida pendiente a Firestore continúa como tarea cancelable y no retrasa el feedback local-first cuando la red está lenta. Las mutaciones remotas del mismo menú se serializan por ID para que un guardado pendiente nunca sobrescriba una edición o eliminación posterior.
   - Desde el modo `Generar con IA` se puede generar una idea con IA según el tipo seleccionado.
   - Mientras se genera una idea con IA, la app debe mostrar un loading bloqueante con el dado 3D animado de MenuDado para evitar dobles acciones, reutilizando el estilo, la cara y el progreso de animación del dado del botón de lanzamiento para que ambos dados se vean sincronizados; la animación debe continuar hasta que termine la respuesta de IA y desacelerar gradualmente sin llegar a pararse. No debe depender del GIF plano anterior. El texto debe atribuir la preparación a la IA y no sugerir que el dado por sí solo prepara la receta.
   - Si la generación en vivo supera 12 segundos, el dado continúa girando y el texto indica que la IA está tardando más de lo habitual. Solo cuando la petición real termina con error, MenuDado consulta hasta 36 propuestas de `sharedAiMenus` del mismo idioma, tipo de comida y público; vuelve a validar localmente edad, embarazo, veganismo, alergias, alimentos a evitar y seguridad antes de mostrar una alternativa. Los documentos heredados sin ámbito conservan una consulta por perfil exacto como respaldo. Nunca mezcla adulto, peques y bebé ni relaja el perfil para conseguir un resultado.
   - Carga, detalle generado y avisos de IA son superficies mutuamente excluyentes. Ante un fallo del proveedor, el aviso se difiere hasta terminar la búsqueda alternativa: una coincidencia abre directamente el menú y la ausencia de coincidencias muestra un único aviso. Si la generación no produce un resultado, el aviso diferencia límite diario, alta demanda, timeout, conectividad o indisponibilidad temporal y muestra únicamente tipo de comida, público y rango de edad; cuando existen restricciones añade `con tu perfil actual` sin enumerarlas. El modal de pausa evita lenguaje técnico y usa un tono cercano: explica que esta vez no se encontró la idea, conserva las preferencias y avisa cuando se puede volver a intentar. Ningún texto visible menciona Firestore, contenido compartido, fallback ni la existencia o ausencia de candidatos internos. Durante una pausa o al alcanzar el máximo diario, el detalle conserva el menú actual y bloquea `Probar otra idea` sin cerrar el modal.
   - La colmena de respaldo es anónima y separada de `users/{uid}`. Solo recibe una propuesta creada por IA cuando el usuario la guarda sin modificar; no guarda UID, correo, foto, ingredientes base escritos ni el contenido legible del perfil. Las recetas nuevas se indexan con una `scopeKey` anónima de idioma, comida y público, y su identidad remota v3 incluye ese ámbito: una receta adulta y otra para bebé nunca comparten documento aunque tengan la misma identidad culinaria. Una receta apta sí puede reutilizarse entre perfiles alimentarios distintos del mismo público. Las recetas recuperadas no vuelven a contribuir.
   - Los equivalentes culinarios se deduplican mediante una clave semántica incluida en la misma respuesta de Gemini y una identidad local v2 antes del hash SHA-256. La canonicalización es específica por familia, ingredientes y preparación: ordena conceptos y unifica alias conservadores en español, inglés y francés sin aplicar stemming genérico que pueda juntar recetas distintas. Además crea una firma independiente de la posición para tolerar que Gemini coloque un concepto en otro segmento. Las instrucciones técnicas del prompt sustituyen cuatro líneas anteriores por una más corta; no añaden llamadas, campos JSON ni longitud al prompt.
   - La canonicalización culinaria sigue usando identidad local v2, pero las nuevas contribuciones de `sharedAiMenus` incluyen `identityVersion: 3` y calculan el ID con la `scopeKey`; las reglas rechazan nuevas identidades antiguas sin bloquear el guardado privado y mantienen el ámbito inmutable. Al leer, la app vuelve a canonicalizar los candidatos y agrupa tanto identidades exactas como recetas de la misma familia con un solapamiento conceptual mínimo de `0.80` antes de aplicar la rotación. `qa/migrate-ai-menu-hive-v2.mjs` continúa documentando y consolidando los documentos v2 heredados con `dry-run` por defecto, backup obligatorio y confirmación exacta del proyecto. Las coincidencias basadas únicamente en el umbral se informan para revisión y nunca se eliminan automáticamente.
   - Cada invitado y cada cuenta mantienen en el teléfono una rotación independiente de colmena con un máximo de 24 `semanticHash`, sin guardar recetas ni datos de perfil. Dentro del conjunto acotado de candidatos compatibles se muestran primero los no vistos; al completar el ciclo se evita repetir inmediatamente si existe otra alternativa. Este historial no se sincroniza, no se incluye en copias de seguridad ni aumenta las lecturas de Firestore.
   - El fallback se controla con Remote Config `ai_menu_hive_enabled`. Si está desactivado no hace lecturas ni escrituras. Una colmena vacía, sin conexión o sin coincidencia segura conserva el error original: no existe garantía absoluta en una primera instalación sin datos compatibles. Cada entrada al respaldo registra `ai_menu_hive_fallback_started` con tipo de comida y causa técnica. El evento terminal `ai_menu_hive_fallback` conserva `hit`, `cache_hit`, `miss` o `error`. Cada contribución iniciada registra un único `ai_menu_hive_contribution`: `saved`, `skipped_disabled`, `skipped_incompatible`, `skipped_invalid_identity` o `error`. Ninguno incluye perfil, receta, UID, ingredientes ni texto libre.
   - Las acciones visibles de IA deben bloquearse de forma inmediata antes de lanzar la corrutina para evitar taps repetidos, doble consumo local y multiples llamadas a Gemini.
   - La generación bloquea inmediatamente los taps repetidos mientras su corrutina está activa, pero una respuesta ya completada no deja una pausa local visible ni impide solicitar otra idea. El throttle compartido continúa protegiendo análisis individual y por lote frente a llamadas casi simultáneas.
   - Una recuperación conocida de Gemini, incluida `RESOURCE_EXHAUSTED`, no bloquea el dado ni se presenta como `IA descansando`: hasta que venza el plazo, las generaciones válidas omiten llamadas inútiles al proveedor y buscan directamente una alternativa compatible en la colmena. Al vencer la recuperación, la siguiente generación vuelve a intentar Gemini. Si la colmena no encuentra una coincidencia segura, se muestra un único aviso contextual sin exponer Firebase, cuotas ni el fallback interno.
   - La pausa local corta de IA no debe sobrevivir al arranque de la pantalla ni restaurarse desde backup de Android. Los SharedPreferences volátiles de uso diario, cuota temporal y throttle de IA quedan excluidos de Auto Backup/Data Extraction para evitar que una reinstalación restaure estados como `(18)` o `IA descansando` que ya no corresponden a la sesión actual.
   - La generación con IA puede usar uno o más ingredientes base escritos por el usuario, por ejemplo `berenjena`; si algún ingrediente contradice el perfil alimentario configurado, la app debe mostrar un aviso y no llamar a la IA. El prompt debe pedir incluir esos ingredientes de forma natural, sin obligarlos como `base principal`, para evitar instrucciones demasiado rígidas en desayunos o cenas que puedan hacer que Firebase AI Logic tarde o falle.
   - Las ideas generadas por IA deben ser saludables, ricas, simples y con ingredientes comunes de supermercado.
   - Si el tipo seleccionado es cena, la idea generada por IA debe ser ligera, rapida y de baja energia para la noche: maximo 10 minutos, pocos ingredientes y preparacion similar de sencilla a un desayuno; debe evitar horno, guarniciones multiples y recetas con varios pasos.
   - La generación de idea con IA debe devolver también el análisis saludable y calorías en la misma llamada. Si el usuario guarda esa idea sin modificarla, el menú debe quedar ya analizado sin hacer una segunda llamada a Firebase IA.
   - Esa misma respuesta IA incluye `shopping_products`: nombres de productos reales de supermercado, sin cantidades, unidades, marcas ni duplicados. En el detalle generado se muestran los productos y la opción `Añadir a la lista de mercado al guardar`, activa por defecto.
   - La app no debe generar imágenes con IA para evitar costes y APIs adicionales. La foto del menú es opcional, debe venir de la cámara del móvil o de una imagen elegida desde la biblioteca, y solo se puede agregar o cambiar después de crear el menú desde `Tus menus` o `Editar menu`.
   - Si el usuario modifica manualmente nombre, descripción, notas o tipo después de generar la idea, la evaluación precalculada debe descartarse para evitar guardar un análisis desactualizado y debe mostrarse el aviso: `Modificaste la receta generada. Para verla como analizada, guarda el menu y toca Analizar IA.`
   - Si el usuario edita solo la foto de un menú guardado, el análisis IA y las calorías existentes deben conservarse porque la receta no cambió.
   - Cada generación debe intentar diferenciarse de menús guardados del mismo tipo y del mismo público objetivo, además de la idea actual del formulario, evitando repetir plato, base principal, proteína o preparación.
   - Durante el mismo día, la app recuerda las últimas ideas generadas con IA por tipo de comida y público objetivo para enviarlas como ideas a evitar en siguientes generaciones, incluso si el usuario no las guarda.
   - Cada generación usa una inspiración culinaria mundial elegida localmente entre 16 opciones, incluidas mediterránea, italiana, griega, latinoamericana, india, africana y asiática. La rotación es independiente por tipo de comida y público, persiste entre reinicios y no repite una inspiración hasta completar el catálogo; solo avanza tras recibir una idea válida.
   - La selección culinaria no añade llamadas ni campos al JSON de Gemini. El prompt sustituye la antigua enumeración larga de formatos por una única línea corta `Inspiración culinaria: <tema>`, por lo que conserva o reduce la longitud enviada y mantiene las restricciones alimentarias, la edad, el tipo de comida y los ingredientes del usuario como prioridades superiores.
   - El prompt de generación prioriza seguridad, público, tipo de comida, ingredientes base y variedad en ese orden; exige nombre específico, cantidades aproximadas para una ración, preparación accionable en 2 a 4 pasos, tiempo total y un consejo útil dentro del JSON existente, sin añadir llamadas ni campos.
   - Los menús ya guardados se pueden editar desde un diálogo `Editar menu` abierto desde la propia tarjeta para evitar que el usuario pierda el contexto.
   - Si al editar un menú se cambia nombre, tipo, público objetivo, descripción o notas, el análisis IA y las calorías asociadas se descartan para evitar mostrar una evaluación desactualizada; el usuario puede volver a tocar `Analizar IA`.

2. Ver menús guardados.
   - Filtrar por desayuno, almuerzo, cena o todos.
   - Todo contenido de menús respeta los públicos activos del perfil alimentario: `Tu último menú`, contadores, análisis pendiente, carruseles, Favoritos, `Ver mas`, resultados del dado, detalles, edición y acciones contextuales deben excluir públicos inactivos. Desactivar un público oculta su contenido sin borrar sus menús, favoritos, fotos ni análisis, que vuelven a estar disponibles al reactivarlo.
   - La sección `Tus menus` se organiza por público objetivo: persona adulta, peques y bebé, mostrando solo públicos activos en el perfil alimentario que además tengan menús guardados.
   - Cada público se presenta como carrusel horizontal con tarjetas compactas y visuales, ordenadas siempre del menú más reciente al más antiguo y mostrando por defecto los 10 más recientes. Al guardar un menú nuevo, el carrusel de su público vuelve al inicio para mostrarlo; marcar o quitar un favorito no cambia la posición de la tarjeta ni reinicia ese carrusel. La cabecera de cada público muestra un badge con el total real de sus menús, aunque el carrusel visible esté limitado.
   - Los menús se pueden marcar como favoritos desde las tarjetas y desde el detalle. Los favoritos mantienen su posición cronológica dentro del carrusel de su público y además aparecen en una colección destacada `Favoritos` al inicio de `Tus menus`, pero solo si pertenecen a públicos activos en el perfil alimentario. Desactivar un público oculta sus favoritos sin borrar esa preferencia, para que vuelvan a aparecer al reactivarlo. La cabecera no repite iconografía y las tarjetas horizontales compactas priorizan el nombre hasta tres líneas. El menú de acciones y el corazón quedan anclados fuera del flujo textual para que la inspiración culinaria, el tipo de comida y el público formen un bloque compacto sin huecos artificiales. La tarjeta mantiene una altura mínima compacta, pero puede crecer con la escala de fuente y extiende el acento terracota a toda la altura resultante. Cada tarjeta conserva un único corazón accionable, portada redondeada, borde arena y acento terracota para diferenciarla visualmente del resto de públicos sin repetir la etiqueta de la colección ni ocupar demasiada altura.
   - La colección `Favoritos` muestra primero el menú cuyo corazón se activó más recientemente, vuelve automáticamente al inicio para hacerlo visible y conserva ese orden tras reiniciar o sincronizar. Muestra contador y la acción `Ver mas`; al tocarla abre una pantalla interna con todos los favoritos en el mismo orden en una grilla de dos columnas. Si el usuario quita el último favorito desde esa pantalla, la app vuelve a Inicio para no dejar un destino vacío.
   - Las tarjetas muestran siempre si el menú es desayuno, almuerzo o cena como metadato debajo del nombre. Cuando existe una inspiración culinaria fiable guardada, muestran antes una línea secundaria localizada `Cocina …` en `Tu último menú`, Favoritos, carruseles por público y grillas de `Ver más`; el modal guardado la presenta como primer badge de metadatos. Los menús manuales o antiguos sin este dato no reciben una clasificación inventada ni reservan un espacio vacío. También muestran una portada: si el menú tiene `imageUri`, carga esa imagen local; si no, muestra un placeholder/skeleton visual coherente con la marca y el tipo de comida.
   - La acción `Ver mas` aparece en cada sección con menús y abre una pantalla interna manteniendo el header de la app. Esa pantalla muestra todos los menús de ese público en grilla, sin scroll horizontal, separados por desayuno, almuerzo y cena.
   - Las colecciones completas de Adulto, Peques, Bebé y Favoritos muestran una barra compacta de búsqueda conectada a la cabecera y una hoja verde de filtros en listas. La búsqueda conserva nombre, descripción, notas e ingredientes y también coincide con todos los textos visibles de la tarjeta en el idioma activo: cocina completa, tipo de comida, estado saludable, calorías visibles y público cuando la tarjeta de Favoritos lo muestra; sigue ignorando mayúsculas y acentos. Cada entrada conserva un ámbito fijo: un filtro nunca incorpora menús de otro público ni menús no favoritos. La consulta y los filtros se reinician al abandonar y volver a entrar. Embarazo, Vegano, Mis alergias y Todo mi perfil evalúan conflictos deterministas en el texto guardado; no representan una certificación clínica ni una instantánea histórica del perfil. Sus interacciones se registran solo como `cta_tapped` con `screen=menu_catalog` y los ocho valores cerrados `search_started`, `clear_search`, `open_filters`, `select_audience_filter`, `select_dietary_filter`, `toggle_favorites_only`, `toggle_healthy_only` y `clear_search_and_filters`, sin enviar la consulta ni datos del perfil.
   - Al tocar un item de `Tus menus`, se abre un modal de detalle con descripción, notas, análisis IA y acciones.
   - Las tarjetas y el modal de detalle ofrecen un menú de tres puntos con acciones de MenuDado sobre fondo verde de cabecera: `Foto del menú`, `Compartir`, `Editar` y `Eliminar`. `Foto del menú` abre el selector existente para tomar una foto con la cámara o elegir una imagen de la biblioteca.
   - Desde el modal de detalle se puede compartir el menú como texto usando el sistema de compartir de Android, compatible con WhatsApp si está instalado. En esta fase no se comparte la imagen local del menú para evitar permisos y compatibilidad extra.
   - Al tocar `Eliminar`, la app debe pedir confirmación antes de borrar el menú de la lista.
   - Estado vacío que invita a crear el primer menú.
   - Si un menú tiene calorías estimadas, mostrarlas como `kcal aprox.` en tarjeta y modal solo cuando el menú ya tenga análisis IA.

3. Dado contextual.
   - El dado aparece dentro del bloque `¿No sabes qué preparar hoy?` y pertenece exclusivamente a la generación de una idea saludable nueva con IA. `Elegir un menú al azar` ejecuta la selección local sobre los guardados con un indicador de progreso propio y no anima el dado IA ni consume cuota de IA.
   - Al tocar el dado, se muestra una animación breve de lanzamiento con duración constante antes del resultado.
   - La animación debe mostrar un dado 3D con seis platos ilustrados en sus caras, bordes redondeados y acabado cálido similar al logo; no debe usar puntos, letras, icono estático ni una cara plana 2D.
   - El dado del botón de lanzamiento debe permitir ajustar su ángulo con el dedo: al mantener presionado sobre el dado y arrastrar, cambia la rotación, y al soltar conserva la posición elegida.
   - Al detenerse después de cada lanzamiento, el dado debe quedar en una orientación de reposo distinta para que se vea una cara diferente.
   - Tras `Elegir un menú al azar`, el resultado abre el mismo modal de detalle que se muestra al tocar cualquier menú guardado. Solo cuando el detalle procede de esa selección muestra `Elegir otro menú`, que repite el sorteo con los mismos filtros; los detalles abiertos desde tarjetas o listas no muestran esa acción.
   - Antes de lanzar el dado, la app sugiere desayuno, almuerzo o cena segun la hora local del movil, y el usuario debe escoger persona adulta, peques o bebé; no hay opción `Todos` y el tipo de comida sugerido se puede cambiar.
   - Los selectores de tipo y público del bloque `¿No sabes qué preparar hoy?` se usan tanto para generar con IA como para elegir menús guardados.
   - Los filtros de público solo muestran públicos activos en el perfil alimentario.
   - Si solo hay un público activo, el selector de público se muestra ya seleccionado con ese público. Si hay dos o más públicos activos, queda vacío y obliga al usuario a elegir.
   - La selección aleatoria solo considera menús del tipo y público objetivo elegidos.
   - Si falta tipo o público y se toca el dado, la app muestra un aviso pidiendo escoger desayuno, almuerzo o cena y persona adulta, peques o bebé antes de lanzar.
   - El azar no debe repetir durante el mismo día menús que ya hayan salido mientras queden candidatos nuevos para el filtro activo.
   - Si todos los menús del filtro activo ya salieron hoy, la memoria de ese filtro se reinicia automáticamente y el dado vuelve a elegir entre todos sus menús sin mostrar aviso.
   - La memoria de menús elegidos también se reinicia automáticamente al cambiar el día.
   - El resultado responde: "Qué comer hoy".
   - Si no hay menús para el filtro elegido, la app muestra una recuperación explícita: crear una idea con IA, probar otro tipo de comida manteniendo siempre el mismo público cuando haya candidatos, o cambiar filtros.

4. Análisis saludable con IA.
   - Requiere internet.
   - Proveedor recomendado: Firebase AI Logic con Gemini 2.5 Flash-Lite.
   - Devuelve un veredicto simple: saludable, intermedio o no saludable.
   - Incluye un resumen breve y una sugerencia práctica.
   - Debe devolver y guardar una estimación numérica de calorías para una ración adecuada al público objetivo.
   - Las calorías estimadas no deben mostrarse en la UI hasta que el menú tenga análisis IA. Una idea generada por IA y guardada sin cambios cuenta como ya analizada porque la misma llamada de generación trae el análisis.
   - Las respuestas IA deben parsearse de forma tolerante porque el proveedor puede envolver JSON en markdown, devolver calorías como texto o incluir caracteres escapados.
   - En la UI, el resultado debe tener protagonismo dentro de la tarjeta del menú con fondo coloreado según estado.
   - Si un menú ya tiene análisis, no debe competir con un botón principal de analizar; el foco debe estar en el resultado.
   - En tarjetas sin análisis, la fila de acciones debe mostrar `Analizar IA` ancho y una papelera compacta solo con icono rojo. En tarjetas ya analizadas, debe mostrarse un botón `Eliminar` de ancho completo con icono de papelera rojo y texto.
   - Cuando existan menús pendientes de análisis pertenecientes a públicos activos, la lista debe ofrecer una acción general `Analizar pendientes con IA` para evaluar un lote pequeño de menús visibles en una sola llamada a Gemini y reducir consumo de requests. El análisis individual por tarjeta debe mantenerse para cuando el usuario quiera evaluar un único menú.
   - El análisis por lote debe enviar menús con `id` y guardar solo los resultados válidos devueltos para cada `id`, conservando los menús que ya estaban analizados.
   - Generación y análisis comparten el contador gratuito del ámbito de identidad activo. El invitado del dispositivo dispone de 5 usos diarios cuando `guest_ai_limits_enabled=true`; cada cuenta registrada dispone de 10 usos propios asociados a su UID. Gastar usos como invitado, iniciar sesión, cerrar sesión o cambiar entre dos cuentas recupera el saldo independiente del ámbito elegido y nunca descuenta usos de otro ámbito.
   - Al agotar el tramo gratuito, solo la generación de ideas puede obtener hasta 10 intentos adicionales diarios mediante anuncios bonificados opcionales por cada ámbito de identidad. Los vídeos vistos y créditos gastados como invitado no reducen las 10 recompensas de una cuenta, y dos cuentas tampoco comparten recompensas. Cada recompensa completada concede exactamente un intento; el análisis individual o por lote nunca consume recompensas ni ofrece anuncios.
   - Un contador técnico local separado registra únicamente las llamadas reales a Firebase AI Logic y mantiene un máximo compartido de 20 al día en el dispositivo para controlar consumo y costes. Mientras haya capacidad, una generación intenta primero Firebase y usa la colmena si falla. Al alcanzar 20 llamadas, una generación autorizada por su ámbito consulta directamente la colmena sin hacer una llamada imposible; el análisis queda bloqueado porque no dispone de fallback equivalente.
   - Existe un límite local absoluto de 20 llamadas reales al proveedor por día, independiente de los saldos de producto. Al agotar las 10 recompensas del ámbito activo o alcanzar las 20 llamadas técnicas, el dado deja de ofrecer nuevos anuncios en ese ámbito. En ese estado el propio botón muestra que se alcanzó el máximo diario, queda deshabilitado y no abre un aviso redundante al tocarlo. Haber alcanzado las 20 llamadas no elimina saldos de otro ámbito: los usos gratuitos y créditos obtenidos previamente conservan su autorización e intentan resolverse con la colmena.
   - El día de uso de IA se reinicia con el día de cuota de Gemini, a medianoche Pacific Time. Los límites del proveedor se aplican por proyecto, pueden variar por tier/modelo y deben seguir monitorizándose en AI Studio/Firebase.
   - Si el proveedor agota su propia cuota o devuelve una pausa temporal, la app debe explicar que la IA esta en pausa y evitar un contador visible que prometa una reactivacion exacta. Si el proveedor devuelve `retry in`, usar ese valor internamente; si no, usar el siguiente reset diario de Gemini API a medianoche Pacific Time.
   - El mensaje de cuota debe diferenciar el tipo de límite cuando Firebase/Gemini lo expone: demasiadas solicitudes seguidas, demasiados tokens/contexto, límite diario gratuito o límite temporal genérico.
   - Los mensajes visibles de cuota deben usar lenguaje simple para público general, sin mencionar métricas técnicas como RPM, TPM, `retry in`, tokens o nombres internos de error.
   - Mientras haya una espera de cuota activa, la app no debe hacer nuevas llamadas a Gemini; debe reutilizar el aviso local hasta que venza la espera interna, incluso si la pantalla o la app se recrea. Cuando la espera llegue a cero, el aviso debe decir que ya se puede reintentar con IA.
   - Si Gemini vuelve a responder `RESOURCE_EXHAUSTED` tras vencer la espera recomendada, la app debe aplicar retroceso exponencial local persistente para reducir reintentos fallidos: primera cuota respeta el proveedor, segunda cuota consecutiva espera al menos 2 minutos, luego 4, 8, 16 y hasta un máximo de 30 minutos. Un éxito de IA reinicia ese control.
   - La generación y el análisis IA deben tener timeout local para no dejar el loading indefinido si Firebase AI Logic tarda demasiado. Si la generación se queda cargando o no termina, revisar primero el timeout local de la llamada, el modelo configurado en `BuildConfig.GEMINI_MODEL`, la disponibilidad/cuotas en Firebase AI Logic y los errores clasificados como `timeout`, `quota_requests`, `quota_daily` o `configuration`.

5. Lista de mercado.
   - La barra inferior incluye `Mercado`, que consolida en una única lista los productos de todos los menús activos.
   - Solo la IA crea productos: una idea generada los recibe en la misma llamada; un menú escrito manualmente los recibe al tocar `Analizar IA`, junto con el análisis saludable. No hay entrada manual de productos.
   - La lista muestra únicamente el nombre del producto, sin cantidades ni unidades. Los nombres se normalizan para unir mayúsculas, tildes y espacios equivalentes sin mostrar duplicados.
   - Cada menú conserva sus productos aunque el usuario lo quite temporalmente de Mercado. Desde el detalle se puede incluir o excluir el menú sin borrarlo; un producto solo desaparece de la lista global cuando ningún otro menú activo lo aporta.
   - Los productos marcados se mueven a una sección plegable `Comprados`, presentada como una cabecera cálida con contador e indicador de expansión, desde donde se pueden restaurar.
   - Cuando existen productos, Mercado muestra un botón accesible de tres puntos que abre la hoja verde `Gestionar lista`. `Vaciar toda la lista` elimina pendientes y comprados de la lista activa, pero conserva los menús y sus productos para poder volver a incluirlos después. La transacción ignora tombstones y menús `PENDING_DELETE`, por lo que nunca convierte una eliminación pendiente en una subida remota.
   - En `cta_tapped`, Mercado conserva sus valores cerrados existentes y añade únicamente `expand_purchased_products`, `collapse_purchased_products` y `close_market_management`; no duplica eventos ni envía nombres, claves o contenido de productos.
   - `Gestionar lista` muestra `Limpiar comprados` solo cuando hay productos comprados y `Vaciar toda la lista` siempre que la lista tenga productos. Ambas acciones requieren una confirmación separada; `Limpiar comprados` no afecta a los pendientes.
   - Las confirmaciones de limpieza esperan si hay otro modal prioritario visible, evitando diálogos superpuestos sin perder la acción pendiente.
   - Los menús antiguos ya analizados pero sin productos muestran `Crear lista de mercado con IA`; esta acción consume una llamada de IA porque no se inventan productos localmente.
   - Room versión 11 guarda las contribuciones por menú y el estado comprado. Firestore sincroniza los productos dentro del documento del menú y los estados globales en `users/{uid}/marketProducts/{productKey}`.
   - La telemetría no debe enviar nombres de productos, claves de producto ni IDs de menús.

6. Comportamiento local primero con backend.
   - Los menús siguen disponibles sin conexión.
   - Los menús, perfil alimentario, uso diario de IA y onboarding se guardan localmente primero y se sincronizan en Firestore bajo el usuario Firebase actual.
   - En una instalación sin sesión resuelta, MenuDado entra por defecto como invitado y conserva el comportamiento anterior con usuario anónimo; no muestra una pantalla de acceso antes de Inicio.
   - En modo invitado, MenuDado permite usar el dado local sin límite, editar y borrar menús, y marcar favoritos. `guest_limits_enabled` controla únicamente el límite diario local de 5 menús escritos manualmente; una idea ya generada con IA siempre se puede guardar desde su detalle, incluida la obtenida mediante un anuncio bonificado. El uso de Gemini se rige por el contador compartido de IA: 5 llamadas gratuitas para invitado y 10 para cuenta registrada cuando `guest_ai_limits_enabled=true`, más las recompensas opcionales permitidas solo para generar ideas.
   - La barra inferior incluye `Mi zona`, una sección de cuenta inspirada en el patrón de zona de usuario: saludo, franja/botón de ventajas que abre un modal con beneficios ampliados y acciones `Registrarme gratis` e `Iniciar sesión` cuando el usuario sigue como invitado.
   - El modal desplazable `Tu cuenta te da más` comunica cuatro ventajas verificables: 10 usos gratuitos diarios de IA para una cuenta; recuperación de menús y favoritos tras reinstalar; conservación del perfil alimentario y sus preferencias; y persistencia de los menús analizados con los productos de su lista de mercado. No compara el tramo con el del invitado porque `guest_ai_limits_enabled=false` concede 10 usos a ambos; tampoco presenta como ventaja el uso en otro móvil ni promete uso o guardado ilimitado.
   - Los formularios de `Registrarme gratis` e `Iniciar sesión` son pantallas internas reutilizables dentro de `Mi zona`, no modales bloqueantes ni pantallas previas al uso de la app. Ambas permiten continuar con Google además de correo y contraseña.
   - Si un invitado crea cuenta con correo, MenuDado vincula el usuario anónimo actual con la credencial de email para conservar el mismo `uid`; así los datos existentes bajo `users/{uid}` quedan asociados a la cuenta sin copiar documentos.
   - Si un invitado inicia sesión con Google, MenuDado vincula el usuario anónimo actual con la credencial de Google cuando Firebase lo permite para conservar el mismo `uid`; en sesiones no invitadas usa el inicio de sesión Firebase normal con Google.
   - Si ya existe una sesión con cuenta registrada por correo o Google, la app entra directamente sin pedir iniciar sesión otra vez.
   - Cuando el usuario ya inició sesión, `Mi zona` muestra `Mis datos` con el correo, el estado `Datos guardados en tu cuenta`, acceso a `Acerca de la app`, acceso a `Privacidad` solo en la variante `debug` y la acción `Cerrar sesión`.
   - Los formularios internos de cuenta, `Acerca de la app` y los detalles abiertos con `Ver mas` usan una flecha de retroceso en la cabecera verde de la app; no deben mostrar una `X` suelta ni un texto `< Volver` dentro del contenido.
   - La migración local de Room a versión 7 marca los menús activos existentes como pendientes de subida para corregir instalaciones donde datos creados antes de la sincronización remota quedaron marcados como sincronizados aunque no existían todavía en Firestore.
   - La migración local de Room a versión 8 agrega `isFavorite` a los menús existentes con valor inicial falso. El favorito forma parte del menú local-first y se sincroniza en Firestore junto con el resto del documento.
   - La migración local de Room a versión 9 agrega `favoritedAt` y usa `createdAt` como respaldo para favoritos existentes. Activar el corazón actualiza esa marca y desactivarlo la limpia; el campo se sincroniza en Firestore para conservar el orden por última selección entre sesiones y dispositivos.
   - Si una operación remota falla, la app mantiene estado pendiente local para reintentar la sincronización de menús, perfil alimentario, uso diario de IA y onboarding en un siguiente arranque.
   - El análisis con IA requiere internet y muestra un error claro si no hay conexión.

7. Onboarding de activación.
   - MenuDado muestra un onboarding breve en modal a las instalaciones nuevas y una sola vez a quienes completaron una versión de contenido anterior, sin reemplazar la pantalla principal.
   - El onboarding v7 concentra la activación en una única propuesta: recibir una idea saludable con IA, encontrar los menús guardados con búsqueda y filtros y preparar la lista de mercado.
   - La acción principal `Ayúdame a elegir` cierra el onboarding y deja Inicio en el modo IA ya seleccionado, pero no lanza una petición ni consume cuota hasta que el usuario completa los selectores y toca el dado. La secundaria `Explorar la app` permite continuar sin bloquear el uso básico.
   - Al empezar u omitir, el onboarding se marca como completado localmente y se sincroniza mediante el documento Firestore existente. La versión vigente es 7: se muestra una vez en instalación nueva o tras una versión de contenido anterior y no vuelve a mostrarse hasta que exista otra versión relevante. Conserva los IDs históricos de CTA `create_first_menu` y `explore_without_onboarding`.

8. Perfil alimentario.
   - La app ofrece acceso a `Perfil` desde la barra inferior flotante.
   - El perfil se guarda localmente en el móvil y se sincroniza en Firestore para el usuario anónimo.
   - El perfil alimentario se configura por público objetivo: persona adulta, peques y bebé.
   - Cada público tiene un interruptor `Activo`; por defecto solo `Persona adulta` viene activo, y `Peques` y `Bebé` empiezan desactivados. Si un público está desactivado, no aparece como botón seleccionable al agregar menús ni al lanzar el dado. La app impide desactivar el último público activo para que siempre quede al menos uno disponible.
   - Si el público seleccionado no está activo, el resto de switches y campos del perfil alimentario quedan deshabilitados.
   - El selector de público muestra el rango de edad como texto fijo bajo el botón seleccionado, sin campo editable visible. Valores iniciales: persona adulta `18+ años`, peques `2-12 años`, bebé `6-24 meses`.
   - En el perfil de persona adulta se puede indicar si la persona está embarazada. Esta opción solo aparece para `Persona adulta` y se envía como restricción de seguridad alimentaria a la generación con IA.
   - Permite indicar si el usuario es vegano.
   - Permite indicar si tiene alergias y seleccionar alérgenos comunes: gluten, lactosa/lácteos, huevo, frutos secos, cacahuete, soja, pescado, marisco y sésamo.
   - Permite escribir alimentos concretos a evitar o indicaciones, por ejemplo `sin picante` o `sin champiñones`. Las condiciones de salud escritas libremente orientan a la IA, pero no se presentan como certificación médica y la propia pantalla recuerda que requieren revisión profesional.
   - La generación de ideas con IA debe respetar el perfil del público objetivo seleccionado, incluyendo rango de edad, restricciones y alimentos concretos a evitar; las condiciones de salud escritas por el usuario se usan como orientación, sin cambiar la creación manual de menús.
   - Toda idea generada se valida localmente con un único contrato de público y perfil antes de mostrarse, releyendo el perfil vigente al terminar cada petición para cubrir hidrataciones o cambios concurrentes. La misma barrera se aplica a candidatos de `sharedAiMenus` y se ejecuta otra vez al guardar para cubrir cambios de perfil con el detalle abierto. Las sustituciones vegetales explícitas se aceptan solo para la familia que reemplazan, sin anular otras alergias o evitaciones concretas. Los incumplimientos detectables de edad, embarazo, veganismo, alérgenos o alimentos concretos fallan de forma conservadora, no añaden otra llamada a Gemini y nunca se guardan ni contribuyen a la colmena.

9. Acerca de la app.
   - `Acerca de la app` se abre desde `Mi zona`.
   - La descripción, el creador y el contacto visibles vienen de Firebase Remote Config mediante las variables string `about_description_v3`, `about_created_by` y `about_contact`; si no existen o están vacías, la app usa textos locales de respaldo. La clave versionada evita que contenido remoto heredado reemplace el fallback localizado.
   - La descripción local de respaldo explica la propuesta completa: ideas con IA adaptadas al perfil, menús guardados, búsqueda y filtros para encontrarlos, elección con el dado, productos de la lista de mercado, acceso inicial sin registro y creación opcional de una cuenta para conservar los datos.
   - Muestra siempre un aviso de salud independiente de Remote Config indicando que MenuDado ofrece ideas informativas, no es un dispositivo médico y no diagnostica, trata, cura ni previene condiciones médicas; también recuerda consultar con un profesional sanitario para asesoramiento, diagnóstico o tratamiento.
   - Muestra un acceso a la política de privacidad pública `https://rhon1990.github.io/menuDado/privacy-policy/`.
   - Muestra al final la versión visible de la app desde `BuildConfig.VERSION_NAME` y `BuildConfig.VERSION_CODE` en formato `versionName (versionCode)` en texto pequeño.

## Dirección Técnica

- Plataforma: Android nativo.
- Lenguaje: Kotlin.
- UI: Jetpack Compose.
- Almacenamiento local: Room.
- Backend: Firebase Auth con email/contraseña, Google Sign-In e invitado anónimo, y Firebase Firestore por usuario bajo `users/{uid}`.
- Google Sign-In requiere que el proveedor Google esté habilitado en Firebase Auth para los proyectos `menudado-debug` y `menudado-6a2da`, que las apps Android tengan las huellas SHA correctas registradas y que cada build tenga un OAuth Web Client ID. Para la app distribuida por Play Store, el proyecto productivo debe registrar en Firebase/Google Cloud la SHA-1 y SHA-256 de `App signing key certificate` de Play Console para `com.menudado`; si solo está registrada la firma debug, Google puede devolver `NoCredentialException` en release aunque debug funcione. Después de registrar esas huellas, descargar y reemplazar los `google-services.json` productivos. La app lee primero el `default_web_client_id` generado desde `google-services.json`; como fallback, Gradle puede inyectar `menudadoDebugGoogleWebClientId` o `MENUDADO_DEBUG_GOOGLE_WEB_CLIENT_ID` para `debug`, y `menudadoProductionGoogleWebClientId` o `MENUDADO_PRODUCTION_GOOGLE_WEB_CLIENT_ID` para `release` y `releaseDebuggable`. Si no existe Web Client ID, la app compila y muestra un aviso de configuración al tocar `Continuar con Google`.
- La app hidrata menús remotos desde Firestore al arrancar cuando no hay escrituras locales pendientes, usando el `uid` actual. Al registrarse o iniciar sesión con una cuenta, MenuDado marca una vez los datos locales relevantes como pendientes para fusionarlos en `users/{uid}` de esa cuenta, incluso si antes estaban sincronizados como invitado. En modo invitado, si se borra la app y Firebase crea un `uid` anónimo nuevo, los datos guardados bajo el `uid` anterior no se muestran por diseño de seguridad. Para recuperar datos entre reinstalaciones el usuario debe crear cuenta o iniciar sesión con correo.
- Arquitectura: MVVM con repositorios.
- Estado y asincronía: Kotlin coroutines y Flow.
- Integración IA: Firebase AI Logic con Gemini 2.5 Flash-Lite para análisis saludable, productos de mercado y lote de pendientes.
- Generación de ideas IA: Firebase AI Logic con Gemini 2.5 Flash-Lite para texto, análisis saludable, calorías y productos de mercado en una sola respuesta. No se usan modelos de imagen IA; las fotos de menú son opcionales, tomadas con cámara o seleccionadas por el usuario desde biblioteca, y gestionadas después de crear el menú guardado.
- Configuración IA: no tocar la configuración de Firebase AI Logic, modelo Gemini, APIs habilitadas, App Check, AI Monitoring, plantillas de instrucciones ni `google-services.json` si no es estrictamente obligatorio para resolver una incidencia confirmada. La configuración actual de IA funciona bastante bien; ante regresiones, priorizar primero ajustes locales de código, control de estado, parseo, timeouts, prompts y manejo de errores antes de cambiar configuración remota o de consola.
- Diagnóstico de regresiones IA: en la corrección de generación IA de 1.0.2 se dejó la pausa local en pocos segundos y se añadió timeout local para evitar cargas indefinidas. Si vuelve a fallar la generación, comparar primero contra ese contrato antes de cambiar prompts o contadores diarios: una idea generada debe consumir una sola llamada real, traer análisis y calorías, permitir guardar sin analizar otra vez y permitir nuevas ideas tras la pausa corta salvo cuota real del proveedor.
- Todos los build variants usan IA real con Firebase AI Logic; `debug` usa el proyecto Firebase separado `MenuDado Debug` (`menudado-debug`) con `applicationId` `com.menudado.debug`, mientras `release` y `releaseDebuggable` usan el proyecto productivo `MenuDado Production` (`menudado-6a2da`) con `applicationId` `com.menudado`.
- Perfil alimentario: configuración local por público objetivo en SharedPreferences usada como restricciones del prompt de generación IA y sincronizada en Firestore.
- Onboarding: estado local en SharedPreferences para mostrar solo en primera apertura una única propuesta de valor orientada a `Crear mi primer menú`, con alternativa `Explorar por mi cuenta`, y sincronizado en Firestore. La versión persistida se mantiene para no volver a mostrarlo a usuarios existentes.
- Metadatos backend: al abrir la app se sincronizan país de la configuración regional, zona horaria, fabricante/modelo de dispositivo, versión Android, `versionName`, `versionCode`, modo de cuenta (`none`, `guest` o `signed_in`) y correo solo cuando el usuario tiene cuenta registrada; no se solicita GPS, contactos ni identificador publicitario.
- Reglas Firestore: `firestore.rules` restringe lectura/escritura a `users/{request.auth.uid}/**`.
- Reglas Firestore: `sharedAiMenus/{semanticHash}` permite lectura autenticada y creación v3 con esquema saneado y `scopeKey`; impide cambiar el ámbito, sobrescribir o borrar recetas y solo permite añadir claves heredadas de compatibilidad dentro del mismo documento. Las reglas se validan con el emulador local.
- El respaldo compartido reutiliza Firestore y Remote Config existentes. No añade migración Room, Cloud Functions, embeddings, búsqueda vectorial, servicios nuevos ni llamadas adicionales a Gemini.
- Remote Config:
  - La visibilidad de publicidad se controla con la variable booleana `ads_enabled`; solo si vale `true` se solicita consentimiento, se inicializa AdMob y se pueden mostrar formatos publicitarios habilitados. El valor por defecto local es `false`.
  - El contenido de `Acerca de la app` se controla con las variables string `about_description_v3`, `about_created_by` y `about_contact`; sus valores por defecto locales conservan la descripción completa de la propuesta de valor, el creador `Rhonal A. Delgado Padilla` y el contacto `rhonal.delgado@gmail.com`. Un `about_description_v3` remoto no vacío prevalece sobre el fallback localizado; las claves anteriores quedan retiradas del contrato actual.
  - Nota histórica (2026-07-29): se eliminó únicamente la clave sin uso `about_description` en `menudado-debug` (template v15) y `menudado-6a2da` (template v12), después de exportar ambos templates, validar el diff y ejecutar dry-run. La relectura posterior confirmó que cada proyecto conserva `about_contact`, `about_created_by`, `ads_enabled`, `ai_menu_hive_enabled`, `guest_ai_limits_enabled`, `guest_limits_enabled` y `rewarded_ai_enabled` con sus valores previos. En ese momento `about_description_v2` permanecía ausente para que la descripción localizada incluida en la app fuera el fallback visible en español, inglés y francés.
  - `guest_limits_enabled` controla únicamente el límite diario de menús escritos manualmente por el invitado; no bloquea guardar una idea que la IA ya entregó. Por defecto local vale `true`.
  - `guest_ai_limits_enabled` controla el tramo gratuito propio del invitado (5 usos frente a los 10 de cada cuenta registrada); por defecto local vale `true`.
  - `rewarded_ai_enabled` controla la oferta de anuncio bonificado al agotar el tramo gratuito; por seguridad su valor por defecto local es `false` y la oferta también exige publicidad inicializada, consentimiento resuelto y un ID de bloque no vacío.
  - El respaldo colaborativo de ideas IA se controla con `ai_menu_hive_enabled`; por defecto local vale `true` y permite desactivarlo inmediatamente sin publicar una nueva versión.
  - En builds con `BuildConfig.DEBUG = true` se usa fetch inmediato para probar cambios sin esperar caché; en builds no debug se conserva intervalo mínimo de 1 hora.
- App Check:
  - MenuDado inicializa Firebase App Check al arrancar la aplicación antes de usar Analytics, Firestore o Firebase AI Logic.
  - La variante `debug` (`com.menudado.debug`, proyecto `MenuDado Debug`) usa `DebugAppCheckProviderFactory`; para probar contra Firebase hay que registrar el token debug que aparece en Logcat dentro de App Check del proyecto debug.
  - La variante `release` (`com.menudado`, proyecto `MenuDado Production`) usa `PlayIntegrityAppCheckProviderFactory`; antes de activar enforcement se deben registrar las huellas SHA-256 correctas, incluyendo Play App Signing para builds de Play Store.
  - La variante `releaseDebuggable` (`com.menudado`, proyecto `MenuDado Production`) usa `DebugAppCheckProviderFactory` para poder probar Firebase producción desde una instalación local firmada con debug; si App Check enforcement está activo, registrar en el proyecto productivo el token debug que aparece en Logcat.
  - No activar enforcement de App Check hasta instalar/probar una build con App Check, revisar que Firebase recibe tokens válidos y confirmar que AI Logic, Firestore y Auth siguen funcionando. App Check no añade coste directo, pero reduce riesgo de abuso de APIs facturables como Firebase AI Logic.
- Analítica:
  - Firebase Analytics anónimo para métricas automáticas de dispositivos/usuarios, modelo de móvil, ubicación agregada de Firebase y eventos de producto sin contenido personal del menú.
  - Implementación central: contrato `MenuDadoAnalytics`, implementación real `FirebaseMenuDadoAnalytics` y `NoOpMenuDadoAnalytics` para contextos sin Firebase.
  - Evento genérico de interacción `cta_tapped` con parámetros cerrados `screen` y `cta` para marcar botones y llamadas a la acción visibles sin enviar contenido del usuario, incluyendo navegación, onboarding, acciones de menú y CTAs de cuenta en `Mi zona` y autenticación. El onboarding conserva los IDs históricos `create_first_menu` y `explore_without_onboarding` aunque cambie el texto visible.
  - MenuDado añade al evento de apertura fabricante/modelo, versión Android, país de la configuración regional y zona horaria; no solicita GPS ni permisos de ubicación.
  - Eventos propios de activación e inventario: `first_menu_created`, `menu_inventory_changed`.
  - Eventos propios de formulario: `menu_form_started`, `menu_save_blocked`, `meal_type_selected`.
  - Eventos propios de filtros y navegación: `audience_filter_selected`, `menu_list_view_more_opened`.
  - Eventos propios de edición multimedia: `menu_edit_started`, `menu_edit_saved`, `menu_photo_updated`; la apertura del selector cámara/biblioteca desde acciones rápidas se marca como `cta_tapped` con `cta=open_photo_source`.
  - Eventos propios del dado: `dice_filter_selected`, `dice_rolled`, `dice_empty_result` y `dice_empty_recovery`; este último usa `action` cerrado (`shown`, `generate_ai`, `broaden_meal_type`, `change_filters`).
  - Eventos propios de consulta de contenido: `menu_card_opened`, `about_app_opened`.
  - Eventos propios de perfil alimentario: `dietary_profile_opened`, `dietary_profile_audience_selected`, `dietary_profile_updated`, sin enviar alérgenos, embarazo, condiciones ni texto libre.
  - Eventos propios de onboarding: `onboarding_shown` y `onboarding_completed` incluyen `onboarding_version=v7` como valor categórico alfanumérico y `exposure_type` limitado a `new_install` o `upgrade`; el segundo añade `action` limitado a `start` o `skip`.
  - Las dimensiones personalizadas prospectivas del embudo son `screen`, `cta`, `action`, `onboarding_version` y `exposure_type`. `first_menu_created` es el evento clave de activación; completar el onboarding por sí solo no se considera conversión.
  - Los favoritos y la lista de mercado conservan sus valores cerrados existentes en `cta_tapped`; no se duplican como eventos adicionales.
  - Eventos propios de actualización de app: `app_update_prompt` con parámetro `action` limitado a `shown`, `update`, `later` o `install`; no envía versión instalada, versión de tienda ni identificadores de usuario.
  - Eventos propios de IA: inicio/fin de generación y análisis, estado saludable (`health_status`), tipo de fallo (`failure_type`), límite diario local (`ai_daily_limit_reached`) y estado cerrado de la oferta bonificada (`ai_rewarded_offer`: `shown`, `unavailable`, `dismissed`, `earned` o `generation_started`), sin receta, perfil ni identificadores publicitarios.
  - Eventos propios de cuenta y zona de usuario: `my_zone_opened`, `auth_flow_started`, `auth_action` y `guest_limit_reached`, usando solo valores cerrados como modo de cuenta, acción, método, tipo de límite y contadores; no envían correo ni datos personales.
  - Eventos propios de backend: `backend_sync_retried`, `backend_sync_finished`, usando solo fuente, estado y conteos pendientes.
  - Los eventos no deben enviar nombres de menú, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs de documentos, UID de Firebase, URI de imagen, alérgenos específicos, embarazo, condiciones de salud ni texto libre del perfil.
- Publicidad:
  - Integración inicial con Google Mobile Ads SDK y User Messaging Platform para consentimiento antes de solicitar anuncios.
  - La visibilidad de publicidad se controla con Firebase Remote Config mediante la variable booleana `ads_enabled`.
  - El banner adaptativo no invasivo de Home se inserta después del formulario `Agregar menu` y antes de `Tus menus`.
  - Al agotar el tramo gratuito de IA, el dado ofrece voluntariamente un anuncio bonificado solo mientras el contador técnico permita otra llamada real. La recompensa es un único intento adicional de generación, con máximo local de 10 recompensas diarias por ámbito de identidad y límite de frecuencia de AdMob de 10 impresiones por usuario y día. Cerrar o no poder cargar el anuncio no concede crédito ni consume una llamada IA. Tras recibir la recompensa, la generación comienza al cerrarse el anuncio y reutiliza el overlay del dado 3D durante al menos 1,5 segundos antes de revelar el menú; este mínimo visual no se aplica a generaciones gratuitas ni a créditos bonificados conservados de una sesión anterior.
  - Durante desarrollo y en `releaseDebuggable` se usan App ID y bloques demo de Google para evitar tráfico inválido en AdMob. `release` usa el bloque bonificado real `ai_daily_limit_rewarded`; puede sustituirse para CI o entornos controlados con `menudadoRewardedAiAdUnitId` o `MENUDADO_REWARDED_AI_AD_UNIT_ID`.
  - La app solicita anuncios no personalizados por defecto mientras el permiso de identificador publicitario se mantiene removido.
  - Si UMP deja de permitir solicitar anuncios después de actualizar las opciones de privacidad, la app oculta inmediatamente banner y oferta bonificada, descarta cualquier anuncio bonificado precargado y solo vuelve a habilitarlos cuando el consentimiento y la inicialización estén listos.
  - Si UMP indica que las opciones de privacidad son requeridas, `Mi zona` muestra `Privacidad` solo en builds de prueba para abrir el formulario de Google; en `release` se mantiene oculta.
  - No se usan anuncios de apertura, interstitials ni rewarded interstitials; el único formato de pantalla completa es el bonificado voluntario iniciado por el usuario desde el dado.
  - El manifest mantiene removido `com.google.android.gms.permission.AD_ID` hasta completar la decisión explícita sobre anuncios personalizados y actualizar la ficha de Google Play si cambia esa estrategia.
- Configuración Firebase: `app/google-services.json`, `app/src/release/google-services.json` y `app/src/releaseDebuggable/google-services.json` apuntan a producción (`MenuDado Production`, `menudado-6a2da`, `com.menudado`); `app/src/debug/google-services.json` apunta a debug (`MenuDado Debug`, `menudado-debug`, `com.menudado.debug`).
- Nombre de proyecto Gradle: `MenuDado`.
- Package/namespace Android: `com.menudado`.
- Application ID: `com.menudado`.
- Nombre visible de la app: `MenuDado`.
- Version visible: sincronizada con `versionName` y `versionCode` del build Android.
- Variantes Android:
  - `debug`: build depurable local con `applicationId` `com.menudado.debug`, nombre visible `MenuDado Debug`, Firebase debug y App ID/bloques demo de AdMob, incluido el bonificado.
  - `release`: build productiva no depurable, usa App ID y ad unit reales de AdMob, activa R8 y reducción de recursos para publicar en tienda, y genera `app/build/outputs/mapping/release/mapping.txt` como archivo de desofuscación para Play Console; queda firmada con debug para poder instalarla desde Android Studio en desarrollo local, no como firma final de tienda.
  - `releaseDebuggable`: build depurable con configuración de release, `applicationId` `com.menudado`, Firebase producción, App Check debug y App ID/bloques demo de AdMob, incluido el bonificado, pensada para diagnosticar comportamiento productivo instalado localmente sin generar tráfico inválido con anuncios reales. Esta variante mantiene minificación y reducción de recursos desactivadas para facilitar depuración.
- Publicación Play Store:
  - La declaración de apps de salud en Play Console debe mantenerse alineada con las funciones reales de MenuDado. Dado que la app planifica menús, usa perfil alimentario, alérgenos, embarazo, condiciones de salud, calorías y análisis saludable con IA, debe declarar al menos `Nutrition and Weight Management` / `Nutrición y control del peso`.
  - La descripción pública de Play Store debe incluir el aviso de que MenuDado no es un dispositivo médico y no diagnostica, trata, cura ni previene ninguna condición médica, además de recomendar consultar con un profesional sanitario para asesoramiento, diagnóstico o tratamiento.
  - La app usa Google Play In-App Updates para detectar actualizaciones pendientes desde Play Store. No debe comparar versiones mediante scraping de la página pública ni depender de Remote Config para saber si hay una actualización disponible. Si Play informa una actualización, MenuDado muestra una invitación no bloqueante una vez por sesión y mantiene en Inicio un recordatorio compacto hasta actualizar. `Ahora no` permite seguir usando toda la app y solo cierra la invitación de esa sesión.
  - Al aceptar, MenuDado usa exclusivamente el flujo flexible oficial para descargar en segundo plano; si flexible no está permitido o el launcher falla, abre la ficha productiva en Play Store sin bloquear la app. Durante la descarga el recordatorio muestra progreso sin repetir acciones y, cuando Play informa `DOWNLOADED`, ofrece `Instalar` tanto en una invitación posponible como en el recordatorio persistente. Los fallos de consulta no bloquean el uso y se reintentan al volver a primer plano.
  - El flujo de actualización solo agrega analítica de acciones cerradas (`shown`, `update`, `later`, `install`) y no requiere migración Room ni cambios de esquema local porque no persiste nuevos datos estructurales.
  - Para Android 15/API 35, `MainActivity` habilita edge-to-edge con `androidx.activity` actualizado y fuerza `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`; las fotos de menús se decodifican con `BitmapFactory.Options.inSampleSize` para evitar cargar bitmaps completos en portadas.
  - El AAB productivo se genera en `app/build/outputs/bundle/release/app-release.aab`.
  - Si Play Console muestra la advertencia de desofuscación, subir `app/build/outputs/mapping/release/mapping.txt` en el artefacto correspondiente para mejorar el diagnóstico de crashes y ANR.
  - El build `release` configura `ndk.debugSymbolLevel = SYMBOL_TABLE`; las librerías nativas actuales vienen de dependencias AndroidX/DataStore y la tarea local `mergeReleaseNativeDebugMetadata` no genera símbolos externos porque no hay metadata nativa propia disponible.
- Recursos públicos para tienda: GitHub Pages desde `docs/`, con política de privacidad en `https://rhon1990.github.io/menuDado/privacy-policy/` y solicitud de eliminacion de datos en `https://rhon1990.github.io/menuDado/data-deletion/`.

## Principios de UX

- La primera pantalla debe ser la app usable, no una página de presentación.
- La navegación principal vive en una barra inferior flotante de superficie cálida, con verde MenuDado para el destino seleccionado y accesos a Inicio, Perfil y Mi zona. `Acerca de la app` y `Privacidad` se acceden desde `Mi zona`. Cambiar mediante las pestañas inferiores abre siempre el destino desde arriba; en cambio, entrar a `Ver más` y volver conserva la posición previa de Inicio.
- Una acción principal clara: `¿No sabes qué preparar hoy?`, con `Ayúdame a elegir` visible por defecto para la generación con IA, `Elegir un menú al azar` como secundaria y escritura manual como terciaria.
- Una acción divertida y protagonista: tocar el dado IA para crear una idea saludable; la selección aleatoria local se presenta como una acción secundaria independiente y nunca mueve el dado IA.
- La cabecera debe respetar el espacio de la barra de estado y usar colores de sistema coherentes con la marca.
- El flujo de creación debe evitar acciones duplicadas: una idea IA se revisa en modal antes de guardarse; un menú manual se guarda desde el formulario; el análisis IA precalculado solo se conserva si la idea generada no fue modificada.
- Los selectores de modo, filtros de comida y switches booleanos deben usar controles reutilizables con estilo de marca MenuDado, evitando componentes básicos sin personalización cuando formen parte de flujos principales.
- En la pantalla principal, los filtros repetidos de tipo de comida y público objetivo deben mostrarse como selectores compactos con menú para reducir la sensación de exceso de botones.
- La respuesta saludable debe ser breve y no juzgar al usuario.
- La app debe funcionar bien con pocos menús y no pedir configuración técnica al usuario final.
- Los textos visibles para usuarios finales no deben mencionar nombres técnicos internos como Firebase, Firestore, API keys, Remote Config, google-services, IDs de cliente ni nombres de variantes como `MenuDado Debug`; esos detalles quedan solo en código, logs técnicos o documentación interna.

## Foco de Validación

- Crear y persistir menús localmente.
- Editar menús guardados conservando el mismo registro local y descartando análisis IA cuando cambie la receta.
- Filtrar menús por tipo de comida.
- Ejecutar la selección aleatoria solo cuando existan menús aplicables al filtro elegido.
- Validar que `Ayúdame a elegir` es la acción visible del estado inicial de generación con IA, que lanzar el dado IA abre el modal sin guardar automáticamente, que `Guardar en mis menús` persiste análisis y calorías y que `Probar otra idea` conserva la intención sin saltarse protecciones.
- Validar que `Elegir un menú al azar` sugiere desayuno, almuerzo o cena por hora local, exige público objetivo, limita candidatos por filtros, no anima el dado IA, permite elegir otro desde el resultado y, ante ausencia real, ofrece recuperación sin mezclar públicos.
- Validar que al desactivar un público en el perfil alimentario desaparece de agregar menú y del dado.
- Validar que la animación del dado no bloquee la UI ni repita resultados por dobles taps accidentales.
- Manejar en el análisis IA: éxito, sin internet, respuesta mal formada y errores del proveedor.
- Mantener la interfaz usable en pantallas Android pequeñas.
- Validar que el marcado de analytics no envíe nombres, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs, URI de imagen ni datos sensibles del perfil alimentario; solo estados, tipos de comida, públicos objetivo, filtros, acciones cerradas y contadores agregables.
- Validar que el onboarding emita `onboarding_shown` solo cuando corresponde y `onboarding_completed` diferenciando `start`/`skip`, ambos con versión `7` y exposición `new_install`/`upgrade`.
- Validar que el contenido actual del onboarding use versión 7, vuelva a mostrarse una sola vez tras una versión anterior, conserve los IDs históricos de CTA y mantenga Inicio en modo IA sin lanzar automáticamente una petición.
- Validar que abrir `Acerca de la app` emita `about_app_opened` sin parámetros personales.
- Validar que posponer una actualización permita seguir usando MenuDado, conserve el recordatorio en Inicio durante la sesión y que el estado descargado ofrezca instalar sin bloquear navegación ni contenido.
- Validar que las reglas Firestore impiden leer o escribir datos de otro `uid`.
- Validar que el manifest final no declare permisos de ubicación, contactos ni identificador publicitario.
- Validar que crear, editar y eliminar menús sincroniza Firestore cuando hay conexión y mantiene pendientes locales cuando falla la red.

## Auditoría QA prepublicación (actualizada 2026-07-29)

- Estado de `1.3.0`: suite debug y APK debug verificados; el visto bueno final de tienda queda condicionado a prueba manual completa con Firebase producción y a generar el artefacto firmado final de Play.
- Versión objetivo actual: `1.3.0` (`versionCode` 14), orientada a Android 16 (`compileSdk=36`, `targetSdk=36`).
- Validación actual de `1.3.0`:
  - `./gradlew :app:testDebugUnitTest :app:assembleDebug`: correcto.
  - `./gradlew :app:lintDebug`: correcto; informe generado sin errores bloqueantes.
  - `app/build/outputs/apk/debug/output-metadata.json`: `applicationId=com.menudado.debug`, `versionName=1.3.0` y `versionCode=14`.
  - `./gradlew :app:installDebug`: instalado correctamente en un `SM-S921B` con Android 16.
  - Prueba manual en dispositivo: `Acerca de la app` muestra el nuevo fallback localizado, permite desplazarse hasta el final y presenta `Versión 1.3.0 (14)`.
  - La sesión del dispositivo estaba autenticada; el modal de ventajas exclusivo del modo invitado queda pendiente de revisión visual manual, aunque su lista, orden, recursos y traducciones se validaron mediante prueba unitaria y compilación.
  - Firebase CLI no tenía una sesión autenticada, por lo que no se publicó configuración remota. La clave versionada `about_description_v2` mostró correctamente el fallback nuevo y evitó que el valor heredado de `about_description` lo reemplazara.
- Evidencia histórica de `1.2.1` (`versionCode` 13):
  - `./gradlew :app:clean :app:testDebugUnitTest :app:lintRelease :app:bundleRelease`: correcto; ejecuta 305 tests, lint release y genera el AAB release.
  - Inspección del manifest dentro del AAB: `compileSdk=36`, `targetSdk=36`, `minSdk=23`, `versionName=1.2.1` y `versionCode=13`.
  - `git diff --check`: sin errores de whitespace.
  - Comparación de strings base contra `values-en` y `values-fr`: sin claves translatables faltantes.
  - Búsqueda de secretos accidentales en diff: sin coincidencias sensibles.
  - Revisión de permisos manifiesto: no se declaran permisos de ubicación/contactos y `AD_ID` se mantiene removido.
- Riesgos detectados antes de tienda:
  - El toolchain usa Android Gradle Plugin 8.10.1 y Gradle 8.11.1, con soporte oficial para `compileSdk=36`; KAPT puede seguir avisando de fallback de lenguaje y debe monitorearse.
  - La firma `release` documentada es debug para instalación local; para tienda se requiere generar artefacto firmado con la configuración final de Play.
  - La QA automatizada cubre primera instalación como invitado, onboarding, guardado local, límites de invitado, sesión persistente por email/Google a nivel de contrato, sincronización remota, hidratación remota y mezcla de menús locales al iniciar sesión.
  - Falta evidencia reciente de QA táctil completa en un dispositivo real con Firebase producción: registro/inicio con Google y correo, persistencia tras reinstalar, sincronización Firestore, Remote Config, anuncios, App Check y reglas Firestore.
