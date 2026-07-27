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
   - Inicio abre con `¿No sabes qué preparar hoy?` y el apoyo `No pasa nada. Elige para quién cocinas y la IA te ayudará con una idea saludable.`; la tarjeta de IA se presenta como `Encontremos algo rico`.
   - La ayuda de IA es el estado inicial porque es la acción con mayor adopción observada y reduce decisiones antes de recibir una idea saludable. Su CTA visible es `Ayúdame a elegir (%1$d)`, con el contador de usos disponible.
   - El tipo de comida es obligatorio y viene sugerido automaticamente segun la hora local del movil: desayuno por la mañana, almuerzo al mediodia/tarde y cena por la noche; el usuario puede cambiarlo antes de guardar o generar.
   - El público objetivo también es obligatorio y no viene seleccionado por defecto; el usuario debe elegir persona adulta, peques o bebé entre los públicos activos del perfil alimentario.
   - Si solo hay un público activo, el selector de público se muestra ya seleccionado con ese público. Si hay dos o más públicos activos, el selector queda vacío y obliga al usuario a elegir para quién es el menú. La hidratación remota de perfiles debe refrescar este estado en el `ViewModel` para no conservar una selección nula o una lista de públicos desactualizada tras sincronizar.
   - En el estado principal se muestra la selección de tipo y público, el campo `¿Qué tienes en casa? (Opcional)` con el ejemplo `Ej. tomate, arroz o pollo`, y el dado IA como acción principal. Al lanzarlo, MenuDado genera una idea saludable usando perfil alimentario, tipo de comida, público objetivo e ingredientes base.
   - Si el dado de IA está bloqueado por falta de tipo de comida, público objetivo o pausa de IA, el propio bloque del dado debe explicar el motivo fuera del botón del dado, usar un aviso cálido alineado con la paleta de MenuDado dentro del bloque amarillo, mantener el botón en un color neutral de marca claramente distinto del naranja activo y recordar que el usuario puede cambiar a `Escribir menú`. El aviso usa el naranja del dado solo como acento/título y deja el motivo en texto oscuro para mejorar legibilidad. En pausa de IA por límite, el aviso debe explicar de forma muy corta que el uso gratuito de IA es limitado y que se puede esperar un poco o seguir con `Escribir menú`.
   - Cuando la IA completa la idea, se abre un modal de detalle con nombre, descripción, notas, calorías, análisis saludable y sugerencia. Debajo de `Idea generada con IA`, el primer metadato identifica la inspiración culinaria seleccionada localmente (`Cocina mexicana`, `Cocina mediterránea`, etc.), seguido del estado saludable y las calorías en una fila responsive que puede saltar de línea. Al guardar la idea, esta inspiración se conserva como metadato opcional del menú en Room y Firebase para mostrarla después; no añade llamadas, tokens ni campos al JSON de Gemini. `Guardar en mis menús` es la acción principal; `Probar otra idea` reutiliza tipo, público e ingredientes base y respeta cuota, throttle y timeout; `Descartar` sigue disponible como acción terciaria.
   - Al tocar `Escribir mi menú` se muestran nombre del plato o menú, ingredientes o descripción y notas opcionales, además de `Guardar menú`; se puede volver a IA sin perder los selectores.
   - Si existen menús guardados, Inicio muestra `Tu último menú` con el menú de mayor `createdAt` y abre su detalle al tocarlo; no representa un borrador ni una edición pendiente.
   - Si el usuario cambia el tipo de comida o el público objetivo del formulario, se limpia el borrador actual del menú para evitar mezclar contenido de contextos distintos; editar los campos de texto conserva el resto del formulario.
   - Calorías estimadas opcionales.
   - Se guarda con una única acción principal: `Guardar menú`.
   - El análisis IA no forma parte del guardado inicial; se ejecuta después desde la tarjeta del menú guardado.
   - Tras persistir correctamente un menú nuevo en Room, la app muestra de inmediato un Snackbar breve y localizado confirmando `Menú guardado con éxito`; las validaciones bloqueadas o los límites de invitado no deben mostrar esa confirmación. La subida pendiente a Firestore continúa como tarea cancelable y no retrasa el feedback local-first cuando la red está lenta. Las mutaciones remotas del mismo menú se serializan por ID para que un guardado pendiente nunca sobrescriba una edición o eliminación posterior.
   - Desde el modo `Generar con IA` se puede generar una idea con IA según el tipo seleccionado.
   - Mientras se genera una idea con IA, la app debe mostrar un loading bloqueante con el dado 3D animado de MenuDado para evitar dobles acciones, reutilizando el estilo, la cara y el progreso de animación del dado del botón de lanzamiento para que ambos dados se vean sincronizados; la animación debe continuar hasta que termine la respuesta de IA y desacelerar gradualmente sin llegar a pararse. No debe depender del GIF plano anterior. El texto debe atribuir la preparación a la IA y no sugerir que el dado por sí solo prepara la receta.
   - Las acciones visibles de IA deben bloquearse de forma inmediata antes de lanzar la corrutina para evitar taps repetidos, doble consumo local y multiples llamadas a Gemini.
   - Antes de realizar una llamada real a Gemini, MenuDado aplica una pausa local mínima y compartida por generación, análisis individual y análisis por lote para bloquear doble tap o llamadas casi simultáneas. Si el usuario reintenta en ese instante, la app no debe hacer otra petición al proveedor ni gastar el contador diario local.
   - La pausa local de IA debe sentirse instantánea y solo proteger contra doble tap; no debe abrir el modal de cuota ni mostrarse como `IA descansando` después de una petición válida o al editar ingredientes base. Si vuelve a aparecer `IA descansando` tras escribir ingredientes o tras solo 1 o 2 ideas, revisar primero `AI_REQUEST_THROTTLE_MILLIS`, el estado `isAiRequestThrottlePause`, la visibilidad de `aiRetryAtMillis` y que no se esté confundiendo una pausa local interna con una cuota real de Firebase/Gemini.
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
   - Al tocar un item de `Tus menus`, se abre un modal de detalle con descripción, notas, análisis IA y acciones.
   - Las tarjetas y el modal de detalle ofrecen un menú de tres puntos con acciones de MenuDado sobre fondo verde de cabecera: `Foto del menú`, `Compartir`, `Editar` y `Eliminar`. `Foto del menú` abre el selector existente para tomar una foto con la cámara o elegir una imagen de la biblioteca.
   - Desde el modal de detalle se puede compartir el menú como texto usando el sistema de compartir de Android, compatible con WhatsApp si está instalado. En esta fase no se comparte la imagen local del menú para evitar permisos y compatibilidad extra.
   - Al tocar `Eliminar`, la app debe pedir confirmación antes de borrar el menú de la lista.
   - Estado vacío que invita a crear el primer menú.
   - Si un menú tiene calorías estimadas, mostrarlas como `kcal aprox.` en tarjeta y modal solo cuando el menú ya tenga análisis IA.

3. Dado contextual.
   - El dado aparece dentro del bloque `Qué comer hoy` y pertenece exclusivamente a la generación de una idea saludable nueva con IA. `Elegir un menú al azar` ejecuta la selección local sobre los guardados con un indicador de progreso propio y no anima el dado IA ni consume cuota de IA.
   - Al tocar el dado, se muestra una animación breve de lanzamiento con duración constante antes del resultado.
   - La animación debe mostrar un dado 3D con seis platos ilustrados en sus caras, bordes redondeados y acabado cálido similar al logo; no debe usar puntos, letras, icono estático ni una cara plana 2D.
   - El dado del botón de lanzamiento debe permitir ajustar su ángulo con el dedo: al mantener presionado sobre el dado y arrastrar, cambia la rotación, y al soltar conserva la posición elegida.
   - Al detenerse después de cada lanzamiento, el dado debe quedar en una orientación de reposo distinta para que se vea una cara diferente.
   - Tras `Elegir un menú al azar`, el resultado abre el mismo modal de detalle que se muestra al tocar cualquier menú guardado. Solo cuando el detalle procede de esa selección muestra `Elegir otro menú`, que repite el sorteo con los mismos filtros; los detalles abiertos desde tarjetas o listas no muestran esa acción.
   - Antes de lanzar el dado, la app sugiere desayuno, almuerzo o cena segun la hora local del movil, y el usuario debe escoger persona adulta, peques o bebé; no hay opción `Todos` y el tipo de comida sugerido se puede cambiar.
   - Los selectores de tipo y público del bloque `Qué comer hoy` se usan tanto para generar con IA como para elegir menús guardados.
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
   - Los botones visibles de IA deben mostrar entre paréntesis los usos locales restantes del día solo cuando la IA esta disponible, por ejemplo `Generar idea con IA (19)` y `Analizar IA (19)`. En modo invitado con `guest_limits_enabled=true`, el contador visible debe corresponder al límite de invitado de esa acción: generación de ideas y análisis muestran sus propios restantes sobre 5, aunque la cuota técnica del proyecto siga siendo 20. Si hay una pausa de cuota activa, no deben mostrar el contador diario; deben mostrar `IA descansando` en una sola línea, recortado con puntos suspensivos si el ancho no alcanza, para evitar comunicar que los usos restantes estan disponibles en ese momento.
   - El contador diario local usa 20 usos como referencia del free tier observado en Firebase/Gemini para este proyecto. Debe decrementar solo cuando la app realiza un intento real de llamada a Gemini y debe reiniciarse con el día de cuota de Gemini, a medianoche Pacific Time. La documentación oficial indica que los límites se aplican por proyecto, que pueden variar por tier/modelo y que los RPD se reinician a medianoche Pacific Time; el valor real debe seguir monitoreándose en AI Studio/Firebase.
   - Si se agota la cuota gratuita de IA, la app debe explicar que la IA esta en pausa y evitar un contador visible que prometa una reactivacion exacta. Si el proveedor devuelve `retry in`, usar ese valor internamente; si no, usar el siguiente reset diario de Gemini API a medianoche Pacific Time.
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
   - Los productos marcados se mueven a una sección plegable `Comprados`, desde donde se pueden restaurar o limpiar.
   - Los menús antiguos ya analizados pero sin productos muestran `Crear lista de mercado con IA`; esta acción consume una llamada de IA porque no se inventan productos localmente.
   - Room versión 11 guarda las contribuciones por menú y el estado comprado. Firestore sincroniza los productos dentro del documento del menú y los estados globales en `users/{uid}/marketProducts/{productKey}`.
   - La telemetría no debe enviar nombres de productos, claves de producto ni IDs de menús.

6. Comportamiento local primero con backend.
   - Los menús siguen disponibles sin conexión.
   - Los menús, perfil alimentario, uso diario de IA y onboarding se guardan localmente primero y se sincronizan en Firestore bajo el usuario Firebase actual.
   - En una instalación sin sesión resuelta, MenuDado entra por defecto como invitado y conserva el comportamiento anterior con usuario anónimo; no muestra una pantalla de acceso antes de Inicio.
   - En modo invitado, MenuDado permite usar el dado sin límite, editar y borrar menús, y marcar favoritos. Para incentivar la cuenta sin bloquear el uso básico, limita por día local a 5 menús guardados, 5 ideas con IA y 5 análisis con IA; al alcanzar un límite muestra un aviso simple invitando a crear cuenta. Estos límites solo aplican al usuario invitado y se pueden desactivar remotamente con `guest_limits_enabled=false`.
   - La barra inferior incluye `Mi zona`, una sección de cuenta inspirada en el patrón de zona de usuario: saludo, franja/botón de ventajas que abre un modal con beneficios ampliados y acciones `Registrarme gratis` e `Iniciar sesión` cuando el usuario sigue como invitado.
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

7. Onboarding de primera apertura.
   - La primera vez que el usuario entra a la app, MenuDado muestra un onboarding breve en modal sin reemplazar la pantalla principal.
   - El onboarding concentra la activación en una única propuesta: resolver qué comer en menos de un minuto generando una idea con IA o dejando que el dado elija entre los menús guardados.
   - La acción principal del onboarding lleva a crear el primer menú; la secundaria permite explorar sin registro y sin bloquear el uso básico.
   - Al empezar u omitir, el onboarding se marca como completado en almacenamiento local y no vuelve a mostrarse en siguientes aperturas hasta que exista una nueva versión de contenido relevante. La versión vigente del contenido de onboarding es 5.

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
   - Permite escribir alimentos a evitar o condiciones de salud relevantes, por ejemplo `diabético`, `hipertenso` o `sin picante`.
   - La generación de ideas con IA debe respetar el perfil del público objetivo seleccionado, incluyendo rango de edad, restricciones y condiciones de salud escritas por el usuario, sin cambiar la creación manual de menús.

9. Acerca de la app.
   - `Acerca de la app` se abre desde `Mi zona`.
   - La descripción, el creador y el contacto visibles vienen de Firebase Remote Config mediante las variables string `about_description`, `about_created_by` y `about_contact`; si no existen o están vacías, la app usa textos locales de respaldo.
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
- Remote Config:
  - La visibilidad de publicidad se controla con la variable booleana `ads_enabled`; solo si vale `true` se solicita consentimiento, se inicializa AdMob y se muestra el banner. El valor por defecto local es `false`.
  - El contenido de `Acerca de la app` se controla con las variables string `about_description`, `about_created_by` y `about_contact`; sus valores por defecto locales conservan la descripción actual, el creador `Rhonal A. Delgado Padilla` y el contacto `rhonal.delgado@gmail.com`.
  - Los límites de uso para invitado se controlan con la variable booleana `guest_limits_enabled`; por defecto local vale `true`. Si se cambia a `false`, el invitado puede guardar menús y usar IA sin las restricciones locales de invitado, manteniendo igualmente las cuotas técnicas del proveedor de IA.
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
  - Evento genérico de interacción `cta_tapped` con parámetros cerrados `screen` y `cta` para marcar botones y llamadas a la acción visibles sin enviar contenido del usuario, incluyendo navegación, onboarding, acciones de menú y CTAs de cuenta en `Mi zona` y autenticación.
  - MenuDado añade al evento de apertura fabricante/modelo, versión Android, país de la configuración regional y zona horaria; no solicita GPS ni permisos de ubicación.
  - Eventos propios de activación e inventario: `first_menu_created`, `menu_inventory_changed`.
  - Eventos propios de formulario: `menu_form_started`, `menu_save_blocked`, `meal_type_selected`.
  - Eventos propios de filtros y navegación: `audience_filter_selected`, `menu_list_view_more_opened`.
  - Eventos propios de edición multimedia: `menu_edit_started`, `menu_edit_saved`, `menu_photo_updated`; la apertura del selector cámara/biblioteca desde acciones rápidas se marca como `cta_tapped` con `cta=open_photo_source`.
  - Eventos propios del dado: `dice_filter_selected`, `dice_rolled`, `dice_empty_result` y `dice_empty_recovery`; este último usa `action` cerrado (`shown`, `generate_ai`, `broaden_meal_type`, `change_filters`).
  - Eventos propios de consulta de contenido: `menu_card_opened`, `about_app_opened`.
  - Eventos propios de perfil alimentario: `dietary_profile_opened`, `dietary_profile_audience_selected`, `dietary_profile_updated`, sin enviar alérgenos, embarazo, condiciones ni texto libre.
  - Eventos propios de onboarding: `onboarding_shown`, `onboarding_completed` con parámetro `action` limitado a `start` o `skip`.
  - Eventos propios de actualización de app: `app_update_prompt` con parámetro `action` limitado a `shown`, `update`, `later` o `install`; no envía versión instalada, versión de tienda ni identificadores de usuario.
  - Eventos propios de IA: inicio/fin de generación y análisis, estado saludable (`health_status`), tipo de fallo (`failure_type`) y límite diario local (`ai_daily_limit_reached`).
  - Eventos propios de cuenta y zona de usuario: `my_zone_opened`, `auth_flow_started`, `auth_action` y `guest_limit_reached`, usando solo valores cerrados como modo de cuenta, acción, método, tipo de límite y contadores; no envían correo ni datos personales.
  - Eventos propios de backend: `backend_sync_retried`, `backend_sync_finished`, usando solo fuente, estado y conteos pendientes.
  - Los eventos no deben enviar nombres de menú, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs de documentos, UID de Firebase, URI de imagen, alérgenos específicos, embarazo, condiciones de salud ni texto libre del perfil.
- Publicidad:
  - Integración inicial con Google Mobile Ads SDK y User Messaging Platform para consentimiento antes de solicitar anuncios.
  - La visibilidad de publicidad se controla con Firebase Remote Config mediante la variable booleana `ads_enabled`.
  - El primer formato monetizable es un banner adaptativo no invasivo en Home, insertado en el contenido después del formulario `Agregar menu` y antes de `Tus menus`.
  - Durante desarrollo usa el App ID y ad unit ID demo de Google para evitar tráfico inválido en AdMob.
  - La app solicita anuncios no personalizados por defecto mientras el permiso de identificador publicitario se mantiene removido.
  - Si UMP indica que las opciones de privacidad son requeridas, `Mi zona` muestra `Privacidad` solo en builds de prueba para abrir el formulario de Google; en `release` se mantiene oculta.
  - No se usan anuncios de apertura, interstitials ni rewarded interstitials en esta fase para no interrumpir el dado, el guardado ni la generación con IA.
  - El manifest mantiene removido `com.google.android.gms.permission.AD_ID` hasta completar la decisión explícita sobre anuncios personalizados y actualizar la ficha de Google Play si cambia esa estrategia.
- Configuración Firebase: `app/google-services.json`, `app/src/release/google-services.json` y `app/src/releaseDebuggable/google-services.json` apuntan a producción (`MenuDado Production`, `menudado-6a2da`, `com.menudado`); `app/src/debug/google-services.json` apunta a debug (`MenuDado Debug`, `menudado-debug`, `com.menudado.debug`).
- Nombre de proyecto Gradle: `MenuDado`.
- Package/namespace Android: `com.menudado`.
- Application ID: `com.menudado`.
- Nombre visible de la app: `MenuDado`.
- Version visible: sincronizada con `versionName` y `versionCode` del build Android.
- Variantes Android:
  - `debug`: build depurable local con `applicationId` `com.menudado.debug`, nombre visible `MenuDado Debug`, Firebase debug y App ID/ad unit demo de AdMob.
  - `release`: build productiva no depurable, usa App ID y ad unit reales de AdMob, activa R8 y reducción de recursos para publicar en tienda, y genera `app/build/outputs/mapping/release/mapping.txt` como archivo de desofuscación para Play Console; queda firmada con debug para poder instalarla desde Android Studio en desarrollo local, no como firma final de tienda.
  - `releaseDebuggable`: build depurable con configuración de release, `applicationId` `com.menudado`, Firebase producción, App Check debug y App ID/ad unit demo de AdMob, pensada para diagnosticar comportamiento productivo instalado localmente sin generar tráfico inválido con anuncios reales. Esta variante mantiene minificación y reducción de recursos desactivadas para facilitar depuración.
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
- Una acción principal clara: `Qué comer hoy`, con `Generar con IA` visible por defecto, `Elegir un menú al azar` como secundaria y escritura manual como terciaria.
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
- Validar que `Generar con IA` es el estado inicial, que lanzar el dado IA abre el modal sin guardar automáticamente, que `Guardar en mis menús` persiste análisis y calorías y que `Probar otra idea` conserva la intención sin saltarse protecciones.
- Validar que `Elegir un menú al azar` sugiere desayuno, almuerzo o cena por hora local, exige público objetivo, limita candidatos por filtros, no anima el dado IA, permite elegir otro desde el resultado y, ante ausencia real, ofrece recuperación sin mezclar públicos.
- Validar que al desactivar un público en el perfil alimentario desaparece de agregar menú y del dado.
- Validar que la animación del dado no bloquee la UI ni repita resultados por dobles taps accidentales.
- Manejar en el análisis IA: éxito, sin internet, respuesta mal formada y errores del proveedor.
- Mantener la interfaz usable en pantallas Android pequeñas.
- Validar que el marcado de analytics no envíe nombres, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs, URI de imagen ni datos sensibles del perfil alimentario; solo estados, tipos de comida, públicos objetivo, filtros, acciones cerradas y contadores agregables.
- Validar que el onboarding emita `onboarding_shown` solo cuando corresponde y `onboarding_completed` diferenciando `start`/`skip`.
- Validar que el contenido actual del onboarding use versión 5 para volver a mostrarse una vez tras la actualización de textos de IA gratuita limitada y dado contextual.
- Validar que abrir `Acerca de la app` emita `about_app_opened` sin parámetros personales.
- Validar que posponer una actualización permita seguir usando MenuDado, conserve el recordatorio en Inicio durante la sesión y que el estado descargado ofrezca instalar sin bloquear navegación ni contenido.
- Validar que las reglas Firestore impiden leer o escribir datos de otro `uid`.
- Validar que el manifest final no declare permisos de ubicación, contactos ni identificador publicitario.
- Validar que crear, editar y eliminar menús sincroniza Firestore cuando hay conexión y mantiene pendientes locales cuando falla la red.

## Auditoría QA prepublicación (actualizada 2026-07-26)

- Veredicto actual: visto bueno técnico automatizado para preparar candidato de publicación. El visto bueno final de tienda queda condicionado a prueba manual en dispositivo real con Firebase producción y a generar el artefacto firmado final de Play.
- Versión objetivo actual: `1.2.1` (`versionCode` 13), orientada a Android 16 (`compileSdk=36`, `targetSdk=36`).
- Validación automatizada ejecutada:
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
  - Falta evidencia reciente de QA táctil completa en un dispositivo real con Firebase producción: registro/inicio con Google y correo, persistencia tras reinstalar, sincronización Firestore, Remote Config, anuncios, App Check y reglas Firestore. En la última auditoría ADB no detectó dispositivos conectados.
