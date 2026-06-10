# MenuDado - Contexto del Proyecto

## Visión

MenuDado es una app Android nativa para planificar menús de comida de forma local y elegir qué comer hoy cuando el usuario no sabe qué cocinar. La app guarda los menús en el móvil y usa IA online para evaluar si un menú es saludable.

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

1. Crear menús localmente.
   - El formulario `Agregar menu` separa dos modos iniciales: `Escribir menu` y `Generar con IA`.
   - El tipo de comida es obligatorio y no viene seleccionado por defecto; el usuario debe elegir desayuno, almuerzo o cena antes de guardar o generar.
   - En modo manual se muestran los campos de nombre del plato o menú, ingredientes o descripción y notas opcionales.
   - En modo IA se muestra primero la selección de tipo, un campo opcional de ingredientes base y la acción de generar; los campos de texto aparecen después de que la IA completa la idea para que el usuario la revise y guarde.
   - Calorías estimadas opcionales.
   - Se guarda con una única acción principal: `Guardar menú`.
   - El análisis IA no forma parte del guardado inicial; se ejecuta después desde la tarjeta del menú guardado.
   - Desde el formulario se puede generar una idea con IA según el tipo seleccionado.
   - La generación con IA puede usar uno o más ingredientes base escritos por el usuario, por ejemplo `berenjena`; si algún ingrediente contradice el perfil alimentario configurado, la app debe mostrar un aviso y no llamar a la IA.
   - Las ideas generadas por IA deben ser saludables, ricas, simples y con ingredientes comunes de supermercado.
   - Si el tipo seleccionado es cena, la idea generada por IA debe ser ligera, rapida y de baja energia para la noche: maximo 10 minutos, pocos ingredientes y preparacion similar de sencilla a un desayuno; debe evitar horno, guarniciones multiples y recetas con varios pasos.
   - La generación de idea con IA debe devolver también el análisis saludable y calorías en la misma llamada. Si el usuario guarda esa idea sin modificarla, el menú debe quedar ya analizado sin hacer una segunda llamada a Firebase IA.
   - Si el usuario modifica manualmente nombre, descripción, notas o tipo después de generar la idea, la evaluación precalculada debe descartarse para evitar guardar un análisis desactualizado y debe mostrarse el aviso: `Modificaste la receta generada. Para verla como analizada, guarda el menu y toca Analizar IA.`
   - Cada generación debe intentar diferenciarse de menús guardados del mismo tipo y de la idea actual del formulario, evitando repetir plato, base principal, proteína o preparación.

2. Ver menús guardados.
   - Filtrar por desayuno, almuerzo, cena o todos.
   - La lista `Tus menus` debe mostrarse colapsada por defecto para ahorrar espacio.
   - Al tocar una tarjeta de `Tus menus`, esa tarjeta se expande y muestra descripción, notas, análisis IA y acciones.
   - Solo una tarjeta de `Tus menus` debe permanecer expandida a la vez; al abrir otra, la anterior se colapsa.
   - Estado vacío que invita a crear el primer menú.
   - Si un menú tiene calorías estimadas, mostrarlas como `kcal aprox.` en tarjeta y modal solo cuando el menú ya tenga análisis IA.

3. Sugerencia aleatoria.
   - Botón de dado para elegir un menú guardado al azar.
   - Al tocar el dado, se muestra una animación breve de lanzamiento antes del resultado.
   - La animación debe mostrar un dado 3D con seis platos ilustrados en sus caras, bordes redondeados y acabado cálido similar al logo; no debe usar puntos, letras, icono estático ni una cara plana 2D.
   - El dado del botón de lanzamiento debe permitir ajustar su ángulo con el dedo: al mantener presionado sobre el dado y arrastrar, cambia la rotación, y al soltar conserva la posición elegida.
   - Al detenerse después de cada lanzamiento, el dado debe quedar en una orientación de reposo distinta para que se vea una cara diferente.
   - El dado mostrado en el modal de resultado debe reflejar la misma orientación final del lanzamiento.
   - El resultado debe mostrarse en un modal diseñado, con cabecera de marca, dado 3D, secciones claras y botón principal de cierre; no debe ser un diálogo de texto plano.
   - Antes de lanzar el dado, el usuario puede filtrar opcionalmente por desayuno, almuerzo o cena.
   - Los filtros del dado deben mantenerse en una sola línea horizontal.
   - Si el usuario elige un filtro, el azar solo considera menús de ese tipo.
   - Si no elige filtro, el azar considera todos los menús guardados.
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
   - Debe devolver y guardar una estimación numérica de calorías para una ración adulta.
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

5. Comportamiento local primero.
   - Los menús siguen disponibles sin conexión.
   - El análisis con IA requiere internet y muestra un error claro si no hay conexión.

6. Onboarding de primera apertura.
   - La primera vez que el usuario entra a la app, MenuDado muestra un onboarding breve en modal sin reemplazar la pantalla principal.
   - El onboarding explica cuatro pasos básicos en este orden: completar el perfil alimentario, guardar menús, usar IA y lanzar el dado.
   - El paso de perfil debe recordar que alergias y alimentos a evitar ayudan a adaptar mejor las ideas generadas.
   - El paso de IA debe comunicar en lenguaje simple que el usuario tiene hasta 20 ayudas de IA al día y que vuelven a estar disponibles cada mañana a las 9 am.
   - El usuario puede avanzar o retroceder por los pasos con swipe horizontal, empezar u omitir.
   - Al empezar u omitir, el onboarding se marca como completado en almacenamiento local y no vuelve a mostrarse en siguientes aperturas.

7. Perfil alimentario.
   - La app ofrece un menú hamburguesa con acceso a `Perfil alimentario`.
   - El perfil se guarda localmente en el móvil.
   - Permite indicar si el usuario es vegano.
   - Permite indicar si tiene alergias y seleccionar alérgenos comunes: gluten, lactosa/lácteos, huevo, frutos secos, cacahuete, soja, pescado, marisco y sésamo.
   - Permite escribir otros alimentos a evitar.
   - La generación de ideas con IA debe respetar el perfil cuando esté configurado, sin cambiar la creación manual de menús.

8. Acerca de la app.
   - El menú hamburguesa ofrece una sección `Acerca de la app`.
   - La sección explica que MenuDado se hizo para ayudar cuando el usuario no sabe qué comer, permitiendo guardar menús, elegir con el dado y apoyarse con IA.
   - Muestra como creador a `Rhonal A. Delgado Padilla`.
   - Muestra el contacto `rhonal.delgado@gmail.com`.
   - Muestra al final la versión visible `Version 1.0.0` en texto pequeño.

## Dirección Técnica

- Plataforma: Android nativo.
- Lenguaje: Kotlin.
- UI: Jetpack Compose.
- Almacenamiento local: Room.
- Arquitectura: MVVM con repositorios.
- Estado y asincronía: Kotlin coroutines y Flow.
- Integración IA: Firebase AI Logic con Gemini 2.5 Flash-Lite.
- Perfil alimentario: configuración local en SharedPreferences usada como restricciones del prompt de generación IA.
- Onboarding: estado local en SharedPreferences para mostrar la guía solo en primera apertura.
- Analítica:
  - Firebase Analytics anónimo para métricas automáticas de dispositivos/usuarios, modelo de móvil, ubicación agregada de Firebase y eventos de producto sin contenido personal del menú.
  - Implementación central: contrato `MenuDadoAnalytics`, implementación real `FirebaseMenuDadoAnalytics` y `NoOpMenuDadoAnalytics` para contextos sin Firebase.
  - MenuDado añade al evento de apertura fabricante/modelo, versión Android, país de la configuración regional y zona horaria; no solicita GPS ni permisos de ubicación.
  - Eventos propios de activación e inventario: `first_menu_created`, `menu_inventory_changed`.
  - Eventos propios de formulario: `menu_form_started`, `menu_save_blocked`, `meal_type_selected`.
  - Eventos propios del dado: `dice_filter_selected`, `dice_rolled`, `dice_empty_result`.
  - Eventos propios de consulta de contenido: `menu_card_opened`, `about_app_opened`.
  - Eventos propios de onboarding: `onboarding_shown`, `onboarding_completed` con parámetro `action` limitado a `start` o `skip`.
  - Eventos propios de IA: inicio/fin de generación y análisis, estado saludable (`health_status`), tipo de fallo (`failure_type`) y límite diario local (`ai_daily_limit_reached`).
  - Los eventos no deben enviar nombres de menú, ingredientes, notas, recetas, correo de contacto ni nombre del creador.
- Configuración Firebase: `app/google-services.json` para el proyecto Firebase asociado a `com.menudado`.
- Nombre de proyecto Gradle: `MenuDado`.
- Package/namespace Android: `com.menudado`.
- Application ID: `com.menudado`.
- Nombre visible de la app: `MenuDado`.
- Version visible: `1.0.0`.
- Recursos públicos para tienda: GitHub Pages desde `docs/`, con política de privacidad en `https://rhon1990.github.io/menuDado/privacy-policy/`.

## Principios de UX

- La primera pantalla debe ser la app usable, no una página de presentación.
- Una acción principal clara: agregar menú.
- Una acción divertida y protagonista: tocar el dado, ver una animación corta y recibir una sugerencia.
- La cabecera debe respetar el espacio de la barra de estado y usar colores de sistema coherentes con la marca.
- El formulario de creación debe evitar acciones duplicadas: guardar primero, analizar con IA después si el usuario lo decide.
- Los selectores de modo, filtros de comida y switches booleanos deben usar controles reutilizables con estilo de marca MenuDado, evitando componentes básicos sin personalización cuando formen parte de flujos principales.
- La respuesta saludable debe ser breve y no juzgar al usuario.
- La app debe funcionar bien con pocos menús y no requerir configuración compleja más allá de Firebase/API.

## Foco de Validación

- Crear y persistir menús localmente.
- Filtrar menús por tipo de comida.
- Ejecutar la selección aleatoria solo cuando existan menús aplicables al filtro elegido.
- Validar que el filtro del dado sea opcional y que limite correctamente los candidatos cuando se selecciona desayuno, almuerzo o cena.
- Validar que la animación del dado no bloquee la UI ni repita resultados por dobles taps accidentales.
- Manejar en el análisis IA: éxito, sin internet, respuesta mal formada y errores del proveedor.
- Mantener la interfaz usable en pantallas Android pequeñas.
- Validar que el marcado de analytics no envíe nombres, ingredientes, notas, recetas, correo de contacto ni nombre del creador; solo estados, tipos de comida, filtros, acciones cerradas y contadores agregables.
- Validar que el onboarding emita `onboarding_shown` solo cuando corresponde y `onboarding_completed` diferenciando `start`/`skip`.
- Validar que abrir `Acerca de la app` emita `about_app_opened` sin parámetros personales.
