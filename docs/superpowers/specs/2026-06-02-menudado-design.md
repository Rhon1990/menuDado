# MenuDado - Diseño

## Objetivo

Crear una app Android nativa en Kotlin para guardar menús locales, analizar si son saludables con IA online y elegir qué comer mediante un botón de dado con filtro opcional por desayuno, almuerzo o cena.

## Identidad

La app usa el logo oficial en `assets/logo/menu-dado-logo.svg` y `assets/logo/menu-dado-logo.png`. La interfaz toma colores del logo: verde marca `#397556`, crema `#FAF6DB`, beige `#E6C696`, marrón `#7D2F10`, tomate `#D6552F`, aguacate `#8FB65C`, amarillo huevo `#F3AE32` y tinta `#263238`.

## Arquitectura

La app usa MVVM con Compose. La lógica de selección aleatoria y parsing IA vive en dominio para poder probarse sin Android. Room persiste los menús localmente. Firebase AI Logic queda preparado como proveedor remoto con Gemini, sin claves hardcodeadas.

## Flujo

La pantalla principal muestra una cabecera con marca, filtros, el botón de dado y la lista de menús. El filtro del dado es opcional: sin filtro considera todos los menús; con filtro solo usa menús de ese tipo. Al pulsar el dado se anima brevemente y después muestra "Hoy toca..." con el menú elegido.

## IA

Al crear o editar un menú se puede pedir análisis saludable. La respuesta esperada se normaliza a `saludable`, `mejorable` o `poco_saludable`, más razón y sugerencia. Si Firebase no está configurado o no hay conexión, la app muestra un error claro y conserva el menú local.

## Validación

Las pruebas cubren filtro opcional del dado, ausencia de candidatos, parsing de la respuesta IA y fallback ante respuestas mal formadas. La verificación de proyecto será compilación Gradle y tests unitarios.

