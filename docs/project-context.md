# MenuDado - Contexto del Proyecto

## Visión

MenuDado es una app Android nativa para planificar menús de comida y elegir qué comer hoy cuando el usuario no sabe qué cocinar. La app mantiene comportamiento local-first con Room para funcionar sin conexión, sincroniza los datos guardados en Firebase Firestore mediante autenticación anónima sin pantalla de login y usa IA online para evaluar si un menú es saludable.

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
- Splash screen: usa el logo completo `app/src/main/res/drawable/menu_dado_logo.png` centrado sobre fondo sólido `#337551`; en Android 12+ se oculta el icono nativo del sistema para evitar un splash previo duplicado.
- Uso recomendado: el SVG como fuente principal de marca y el PNG para previsualizaciones o exportaciones temporales.
- Estilo: ilustrado, cálido, apetitoso y cercano, con lectura clara como icono de app.

### Paleta de Colores

- Verde marca: `#2F765D` - fondo principal de marca, saludable y confiable.
- Verde cabecera: `#337551` - fondo sólido de la cabecera principal.
- Verde profundo: `#1F4F43` - cabecera, barra de estado y contrastes fuertes.
- Crema cálido: `#FFF7E7` - dado, fondos claros y contraste suave.
- Crema del dado de platos: `#FBF2DC` - fondo específico de las caras del dado ilustrado.
- Fondo app: `#FFF9EC` - fondo general cálido y limpio.
- Superficie: `#FFFCF4` - tarjetas principales con lectura clara.
- Superficie formulario: `#FFEBC7` - bloque de creación de menú.
- Arena suave: `#F2DFC1` - estados neutros o sin analizar.
- Marrón contorno: `#9A6A45` - bordes y detalles secundarios.
- Marrón borde dado: `#C49460` - trazo fino de las caras del dado 3D.
- Tomate: `#E35D3E` - llamada principal del dado y estados poco saludables.
- Aguacate: `#79A85B` - selección y estados saludables.
- Amarillo huevo: `#F4B43E` - desayuno, energía y estados intermedios.
- Tinta: `#263238` - texto principal.
- Tinta secundaria: `#5F6B66` - textos de apoyo.

## Funcionalidades Principales

### Idioma de la app

- La UI debe usar recursos Android y seguir el idioma del móvil cuando esté en español, inglés o francés; cualquier otro idioma debe caer en español.
- Las etiquetas visibles de público en español son `Persona adulta`, `Peques` y `Bebé`.
- La generación y el análisis con IA deben pedir nombre, descripción, notas, razón y sugerencia en el mismo idioma visible de la app para evitar mezclar idiomas en menús creados por IA.

1. Crear menús.
   - El formulario `Agregar menu` separa dos modos iniciales: `Escribir menu` y `Generar con IA`.
   - El tipo de comida es obligatorio y viene sugerido automaticamente segun la hora local del movil: desayuno por la mañana, almuerzo al mediodia/tarde y cena por la noche; el usuario puede cambiarlo antes de guardar o generar.
   - El público objetivo también es obligatorio y no viene seleccionado por defecto; el usuario debe elegir persona adulta, peques o bebé entre los públicos activos del perfil alimentario.
   - Si solo hay un público activo, el selector de público se muestra ya seleccionado con ese público. Si hay dos o más públicos activos, el selector queda vacío y obliga al usuario a elegir para quién es el menú.
   - En modo manual se muestran los campos de nombre del plato o menú, ingredientes o descripción y notas opcionales.
   - En modo IA se muestra primero la selección de tipo, un campo opcional de ingredientes base y la acción de generar; los campos de texto aparecen después de que la IA completa la idea para que el usuario la revise y guarde.
   - Si el usuario cambia el tipo de comida o el público objetivo del formulario, se limpia el borrador actual del menú para evitar mezclar contenido de contextos distintos; editar los campos de texto conserva el resto del formulario.
   - Calorías estimadas opcionales.
   - Se guarda con una única acción principal: `Guardar menú`.
   - El análisis IA no forma parte del guardado inicial; se ejecuta después desde la tarjeta del menú guardado.
   - Desde el formulario se puede generar una idea con IA según el tipo seleccionado.
   - Mientras se genera una idea con IA, la app debe mostrar un loading bloqueante con el dado de carga de MenuDado para evitar dobles acciones.
   - Las acciones visibles de IA deben bloquearse de forma inmediata antes de lanzar la corrutina para evitar taps repetidos, doble consumo local y multiples llamadas a Gemini.
   - Antes de realizar una llamada real a Gemini, MenuDado aplica una pausa local corta y compartida por generación, análisis individual y análisis por lote. Si hubo un intento de IA hace pocos segundos, la app muestra el aviso de espera sin gastar el contador diario local ni hacer otra petición al proveedor.
   - La generación con IA puede usar uno o más ingredientes base escritos por el usuario, por ejemplo `berenjena`; si algún ingrediente contradice el perfil alimentario configurado, la app debe mostrar un aviso y no llamar a la IA.
   - Las ideas generadas por IA deben ser saludables, ricas, simples y con ingredientes comunes de supermercado.
   - Si el tipo seleccionado es cena, la idea generada por IA debe ser ligera, rapida y de baja energia para la noche: maximo 10 minutos, pocos ingredientes y preparacion similar de sencilla a un desayuno; debe evitar horno, guarniciones multiples y recetas con varios pasos.
   - La generación de idea con IA debe devolver también el análisis saludable y calorías en la misma llamada. Si el usuario guarda esa idea sin modificarla, el menú debe quedar ya analizado sin hacer una segunda llamada a Firebase IA.
   - La app no debe generar imágenes con IA para evitar costes y APIs adicionales. La foto del menú es opcional, debe venir de la cámara del móvil o de una imagen elegida desde la biblioteca, y solo se puede agregar o cambiar después de crear el menú desde `Tus menus` o `Editar menu`.
   - Si el usuario modifica manualmente nombre, descripción, notas o tipo después de generar la idea, la evaluación precalculada debe descartarse para evitar guardar un análisis desactualizado y debe mostrarse el aviso: `Modificaste la receta generada. Para verla como analizada, guarda el menu y toca Analizar IA.`
   - Si el usuario edita solo la foto de un menú guardado, el análisis IA y las calorías existentes deben conservarse porque la receta no cambió.
   - Cada generación debe intentar diferenciarse de menús guardados del mismo tipo y del mismo público objetivo, además de la idea actual del formulario, evitando repetir plato, base principal, proteína o preparación.
   - Durante el mismo día, la app recuerda las últimas ideas generadas con IA por tipo de comida y público objetivo para enviarlas como ideas a evitar en siguientes generaciones, incluso si el usuario no las guarda.
   - El prompt de generación debe promover variedad saludable con opciones como cremas o sopas ligeras, ensaladas completas, ensalada César saludable, bowls, salteados simples, tortillas, legumbres, wraps, tostas, pasta integral y arroz integral.
   - Los menús ya guardados se pueden editar desde un diálogo `Editar menu` abierto desde la propia tarjeta para evitar que el usuario pierda el contexto.
   - Si al editar un menú se cambia nombre, tipo, público objetivo, descripción o notas, el análisis IA y las calorías asociadas se descartan para evitar mostrar una evaluación desactualizada; el usuario puede volver a tocar `Analizar IA`.

2. Ver menús guardados.
   - Filtrar por desayuno, almuerzo, cena o todos.
   - La sección `Tus menus` se organiza por público objetivo: persona adulta, peques y bebé, mostrando solo públicos activos en el perfil alimentario que además tengan menús guardados.
   - Cada público se presenta como carrusel horizontal con tarjetas compactas y visuales, mostrando por defecto los 10 menús más recientes de ese público.
   - Las tarjetas muestran una portada: si el menú tiene `imageUri`, carga esa imagen local; si no, muestra un placeholder/skeleton visual coherente con la marca y el tipo de comida.
   - La acción `Ver mas` aparece en cada sección con menús y abre una pantalla interna manteniendo el header de la app. Esa pantalla muestra todos los menús de ese público en grilla, sin scroll horizontal, separados por desayuno, almuerzo y cena.
   - Al tocar un item de `Tus menus`, se abre un modal de detalle con descripción, notas, análisis IA y acciones.
   - Al tocar `Eliminar`, la app debe pedir confirmación antes de borrar el menú de la lista.
   - Estado vacío que invita a crear el primer menú.
   - Si un menú tiene calorías estimadas, mostrarlas como `kcal aprox.` en tarjeta y modal solo cuando el menú ya tenga análisis IA.

3. Sugerencia aleatoria.
   - Botón de dado para elegir un menú guardado al azar.
   - Al tocar el dado, se muestra una animación breve de lanzamiento antes del resultado.
   - La animación debe mostrar un dado 3D con seis platos ilustrados en sus caras, bordes redondeados y acabado cálido similar al logo; no debe usar puntos, letras, icono estático ni una cara plana 2D.
   - El dado del botón de lanzamiento debe permitir ajustar su ángulo con el dedo: al mantener presionado sobre el dado y arrastrar, cambia la rotación, y al soltar conserva la posición elegida.
   - Al detenerse después de cada lanzamiento, el dado debe quedar en una orientación de reposo distinta para que se vea una cara diferente.
   - Tras la animación, el resultado debe abrir el mismo modal de detalle que se muestra al tocar cualquier menú guardado.
   - Antes de lanzar el dado, la app sugiere desayuno, almuerzo o cena segun la hora local del movil, y el usuario debe escoger persona adulta, peques o bebé; no hay opción `Todos` y el tipo de comida sugerido se puede cambiar.
   - Cada grupo de filtros del dado debe mantenerse como selector horizontal de lectura rápida.
   - Los filtros de público del dado solo muestran públicos activos en el perfil alimentario.
   - Si solo hay un público activo, el filtro de público del dado se muestra ya seleccionado con ese público. Si hay dos o más públicos activos, queda vacío y obliga al usuario a elegir.
   - Si el usuario elige filtros, el azar solo considera menús de ese tipo y público objetivo.
   - Si no elige filtros y toca el dado, la app muestra un aviso pidiendo escoger desayuno, almuerzo o cena y persona adulta, peques o bebé antes de lanzar.
   - El azar no debe repetir durante el mismo día menús que ya hayan salido mientras queden candidatos nuevos para el filtro activo.
   - Si todos los menús del filtro activo ya salieron hoy, la memoria de ese filtro se reinicia automáticamente y el dado vuelve a elegir entre todos sus menús sin mostrar aviso.
   - La memoria de menús elegidos también se reinicia automáticamente al cambiar el día.
   - El resultado responde: "Qué comer hoy".
   - Si no hay menús para el filtro elegido, la app guía al usuario para crear uno o quitar el filtro.

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
   - Cuando existan menús pendientes de análisis, la lista debe ofrecer una acción general `Analizar pendientes con IA` para evaluar un lote pequeño de menús en una sola llamada a Gemini y reducir consumo de requests. El análisis individual por tarjeta debe mantenerse para cuando el usuario quiera evaluar un único menú.
   - El análisis por lote debe enviar menús con `id` y guardar solo los resultados válidos devueltos para cada `id`, conservando los menús que ya estaban analizados.
   - Los botones visibles de IA deben mostrar entre paréntesis los usos locales restantes del día solo cuando la IA esta disponible, por ejemplo `Generar idea saludable con IA (19)` y `Analizar IA (19)`. Si hay una pausa de cuota activa, no deben mostrar el contador diario; deben mostrar `IA descansando` en una sola línea, recortado con puntos suspensivos si el ancho no alcanza, para evitar comunicar que los usos restantes estan disponibles en ese momento.
   - El contador diario local usa 20 usos como referencia del free tier observado en Firebase/Gemini para este proyecto. Debe decrementar solo cuando la app realiza un intento real de llamada a Gemini y debe reiniciarse con el día de cuota de Gemini, a medianoche Pacific Time. La documentación oficial indica que los límites se aplican por proyecto, que pueden variar por tier/modelo y que los RPD se reinician a medianoche Pacific Time; el valor real debe seguir monitoreándose en AI Studio/Firebase.
   - Si se agota la cuota gratuita de IA, la app debe explicar que la IA esta en pausa y evitar un contador visible que prometa una reactivacion exacta. Si el proveedor devuelve `retry in`, usar ese valor internamente; si no, usar el siguiente reset diario de Gemini API a medianoche Pacific Time.
   - El mensaje de cuota debe diferenciar el tipo de límite cuando Firebase/Gemini lo expone: demasiadas solicitudes seguidas, demasiados tokens/contexto, límite diario gratuito o límite temporal genérico.
   - Los mensajes visibles de cuota deben usar lenguaje simple para público general, sin mencionar métricas técnicas como RPM, TPM, `retry in`, tokens o nombres internos de error.
   - Mientras haya una espera de cuota activa, la app no debe hacer nuevas llamadas a Gemini; debe reutilizar el aviso local hasta que venza la espera interna, incluso si la pantalla o la app se recrea. Cuando la espera llegue a cero, el aviso debe decir que ya se puede reintentar con IA.
   - Si Gemini vuelve a responder `RESOURCE_EXHAUSTED` tras vencer la espera recomendada, la app debe aplicar retroceso exponencial local persistente para reducir reintentos fallidos: primera cuota respeta el proveedor, segunda cuota consecutiva espera al menos 2 minutos, luego 4, 8, 16 y hasta un máximo de 30 minutos. Un éxito de IA reinicia ese control.

5. Comportamiento local primero con backend.
   - Los menús siguen disponibles sin conexión.
   - Los menús, perfil alimentario, uso diario de IA y onboarding se guardan localmente primero y se sincronizan en Firestore bajo el usuario anónimo del dispositivo.
   - Si una operación remota falla, la app mantiene estado pendiente local para reintentar la sincronización de menús, perfil alimentario, uso diario de IA y onboarding en un siguiente arranque.
   - El análisis con IA requiere internet y muestra un error claro si no hay conexión.

6. Onboarding de primera apertura.
   - La primera vez que el usuario entra a la app, MenuDado muestra un onboarding breve en modal sin reemplazar la pantalla principal.
   - El onboarding explica cuatro pasos básicos en este orden: completar el perfil alimentario, guardar menús, usar IA y lanzar el dado.
   - El paso de perfil debe recordar que el usuario puede activar persona adulta, peques o bebé e indicar embarazo, alergias y condiciones de salud para adaptar mejor las ideas generadas.
   - El paso de IA debe comunicar en lenguaje simple que el usuario tiene hasta 20 ayudas de IA al día y que vuelven a estar disponibles cada mañana a las 9 am.
   - El usuario puede avanzar o retroceder por los pasos con swipe horizontal, empezar u omitir.
   - Al empezar u omitir, el onboarding se marca como completado en almacenamiento local y no vuelve a mostrarse en siguientes aperturas hasta que exista una nueva versión de contenido relevante.

7. Perfil alimentario.
   - La app ofrece un menú hamburguesa con acceso a `Perfil alimentario`.
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

8. Acerca de la app.
   - El menú hamburguesa ofrece una sección `Acerca de la app`.
   - La sección explica que MenuDado se hizo para ayudar cuando el usuario no sabe qué comer, permitiendo guardar menús, elegir con el dado y apoyarse con IA.
   - Muestra como creador a `Rhonal A. Delgado Padilla`.
   - Muestra el contacto `rhonal.delgado@gmail.com`.
   - Muestra al final la versión visible de la app desde `BuildConfig.VERSION_NAME` en texto pequeño.

## Dirección Técnica

- Plataforma: Android nativo.
- Lenguaje: Kotlin.
- UI: Jetpack Compose.
- Almacenamiento local: Room.
- Backend: Firebase Auth anónimo y Firebase Firestore por usuario bajo `users/{uid}`; no hay login visible.
- Arquitectura: MVVM con repositorios.
- Estado y asincronía: Kotlin coroutines y Flow.
- Integración IA: Firebase AI Logic con Gemini 2.5 Flash-Lite para análisis saludable y lote de pendientes.
- Generación de ideas IA: Firebase AI Logic con Gemini 2.5 Flash-Lite para texto, análisis saludable y calorías. No se usan modelos de imagen IA; las fotos de menú son opcionales, tomadas con cámara o seleccionadas por el usuario desde biblioteca, y gestionadas después de crear el menú guardado.
- Todos los build variants usan IA real con Firebase AI Logic; `debug`, `develop` y `releaseDebuggable` usan el proyecto Firebase separado `menudado-debug` con `applicationId` `com.menudado.debug`, mientras `release` usa el proyecto productivo `menudado-6a2da` con `applicationId` `com.menudado`.
- Perfil alimentario: configuración local por público objetivo en SharedPreferences usada como restricciones del prompt de generación IA y sincronizada en Firestore.
- Onboarding: estado local en SharedPreferences para mostrar la guía solo en primera apertura y sincronizado en Firestore.
- Metadatos backend: al abrir la app se sincronizan país de la configuración regional, zona horaria, fabricante/modelo de dispositivo, versión Android, `versionName` y `versionCode`; no se solicita GPS, contactos ni identificador publicitario.
- Reglas Firestore: `firestore.rules` restringe lectura/escritura a `users/{request.auth.uid}/**`.
- Analítica:
  - Firebase Analytics anónimo para métricas automáticas de dispositivos/usuarios, modelo de móvil, ubicación agregada de Firebase y eventos de producto sin contenido personal del menú.
  - Implementación central: contrato `MenuDadoAnalytics`, implementación real `FirebaseMenuDadoAnalytics` y `NoOpMenuDadoAnalytics` para contextos sin Firebase.
  - Evento genérico de interacción `cta_tapped` con parámetros cerrados `screen` y `cta` para marcar botones y llamadas a la acción visibles sin enviar contenido del usuario.
  - MenuDado añade al evento de apertura fabricante/modelo, versión Android, país de la configuración regional y zona horaria; no solicita GPS ni permisos de ubicación.
  - Eventos propios de activación e inventario: `first_menu_created`, `menu_inventory_changed`.
  - Eventos propios de formulario: `menu_form_started`, `menu_save_blocked`, `meal_type_selected`.
  - Eventos propios de filtros y navegación: `audience_filter_selected`, `menu_list_view_more_opened`.
  - Eventos propios de edición multimedia: `menu_edit_started`, `menu_edit_saved`, `menu_photo_updated`.
  - Eventos propios del dado: `dice_filter_selected`, `dice_rolled`, `dice_empty_result`.
  - Eventos propios de consulta de contenido: `menu_card_opened`, `about_app_opened`.
  - Eventos propios de perfil alimentario: `dietary_profile_opened`, `dietary_profile_audience_selected`, `dietary_profile_updated`, sin enviar alérgenos, embarazo, condiciones ni texto libre.
  - Eventos propios de onboarding: `onboarding_shown`, `onboarding_completed` con parámetro `action` limitado a `start` o `skip`.
  - Eventos propios de IA: inicio/fin de generación y análisis, estado saludable (`health_status`), tipo de fallo (`failure_type`) y límite diario local (`ai_daily_limit_reached`).
  - Eventos propios de backend: `backend_sync_retried`, `backend_sync_finished`, usando solo fuente, estado y conteos pendientes.
  - Los eventos no deben enviar nombres de menú, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs de documentos, UID de Firebase, URI de imagen, alérgenos específicos, embarazo, condiciones de salud ni texto libre del perfil.
- Publicidad:
  - Integración inicial con Google Mobile Ads SDK y User Messaging Platform para consentimiento antes de solicitar anuncios.
  - El primer formato monetizable es un banner adaptativo no invasivo en Home, insertado en el contenido después del formulario `Agregar menu` y antes de `Tus menus`.
  - Durante desarrollo usa el App ID y ad unit ID demo de Google para evitar tráfico inválido en AdMob.
  - La app solicita anuncios no personalizados por defecto mientras el permiso de identificador publicitario se mantiene removido.
  - Si UMP indica que las opciones de privacidad son requeridas, el menú lateral muestra `Opciones de privacidad` para abrir el formulario de Google.
  - No se usan anuncios de apertura, interstitials ni rewarded interstitials en esta fase para no interrumpir el dado, el guardado ni la generación con IA.
  - El manifest mantiene removido `com.google.android.gms.permission.AD_ID` hasta completar la decisión explícita sobre anuncios personalizados y actualizar la ficha de Google Play si cambia esa estrategia.
- Configuración Firebase: `app/google-services.json` y `app/src/release/google-services.json` apuntan a producción (`menudado-6a2da`, `com.menudado`); `app/src/debug/google-services.json`, `app/src/develop/google-services.json` y `app/src/releaseDebuggable/google-services.json` apuntan a debug (`menudado-debug`, `com.menudado.debug`).
- Nombre de proyecto Gradle: `MenuDado`.
- Package/namespace Android: `com.menudado`.
- Application ID: `com.menudado`.
- Nombre visible de la app: `MenuDado`.
- Version visible: sincronizada con `versionName` del build Android.
- Variantes Android:
  - `debug`: build depurable local con `applicationId` `com.menudado.debug`, nombre visible `MenuDado Debug`, Firebase debug y App ID/ad unit demo de AdMob.
  - `develop`: build depurable para desarrollo local con `applicationId` `com.menudado.debug`, Firebase debug y App ID/ad unit demo de AdMob para evitar tráfico inválido.
  - `release`: build productiva no depurable, usa App ID y ad unit reales de AdMob; queda firmada con debug para poder instalarla desde Android Studio en desarrollo local, no como firma final de tienda.
  - `releaseDebuggable`: build depurable con configuración de release, `applicationId` `com.menudado.debug`, Firebase debug y App ID/ad unit demo de AdMob, pensada para diagnosticar comportamiento casi productivo sin generar tráfico inválido con anuncios reales ni mezclar datos/IA con producción.
- Recursos públicos para tienda: GitHub Pages desde `docs/`, con política de privacidad en `https://rhon1990.github.io/menuDado/privacy-policy/` y solicitud de eliminacion de datos en `https://rhon1990.github.io/menuDado/data-deletion/`.

## Principios de UX

- La primera pantalla debe ser la app usable, no una página de presentación.
- Una acción principal clara: agregar menú.
- Una acción divertida y protagonista: tocar el dado, ver una animación corta y recibir una sugerencia.
- La cabecera debe respetar el espacio de la barra de estado y usar colores de sistema coherentes con la marca.
- El formulario de creación debe evitar acciones duplicadas: guardar primero, analizar con IA después si el usuario lo decide.
- Los selectores de modo, filtros de comida y switches booleanos deben usar controles reutilizables con estilo de marca MenuDado, evitando componentes básicos sin personalización cuando formen parte de flujos principales.
- En la pantalla principal, los filtros repetidos de tipo de comida y público objetivo deben mostrarse como selectores compactos con menú para reducir la sensación de exceso de botones.
- La respuesta saludable debe ser breve y no juzgar al usuario.
- La app debe funcionar bien con pocos menús y no requerir configuración compleja más allá de Firebase/API.

## Foco de Validación

- Crear y persistir menús localmente.
- Editar menús guardados conservando el mismo registro local y descartando análisis IA cuando cambie la receta.
- Filtrar menús por tipo de comida.
- Ejecutar la selección aleatoria solo cuando existan menús aplicables al filtro elegido.
- Validar que el dado sugiere desayuno, almuerzo o cena por hora local, exige público objetivo antes de lanzar, mantiene la validación interna si falta el tipo de comida y limita correctamente los candidatos por filtros.
- Validar que al desactivar un público en el perfil alimentario desaparece de agregar menú y del dado.
- Validar que la animación del dado no bloquee la UI ni repita resultados por dobles taps accidentales.
- Manejar en el análisis IA: éxito, sin internet, respuesta mal formada y errores del proveedor.
- Mantener la interfaz usable en pantallas Android pequeñas.
- Validar que el marcado de analytics no envíe nombres, ingredientes, notas, recetas, correo de contacto, nombre del creador, IDs, URI de imagen ni datos sensibles del perfil alimentario; solo estados, tipos de comida, públicos objetivo, filtros, acciones cerradas y contadores agregables.
- Validar que el onboarding emita `onboarding_shown` solo cuando corresponde y `onboarding_completed` diferenciando `start`/`skip`.
- Validar que abrir `Acerca de la app` emita `about_app_opened` sin parámetros personales.
- Validar que las reglas Firestore impiden leer o escribir datos de otro `uid`.
- Validar que el manifest final no declare permisos de ubicación, contactos ni identificador publicitario.
- Validar que crear, editar y eliminar menús sincroniza Firestore cuando hay conexión y mantiene pendientes locales cuando falla la red.
