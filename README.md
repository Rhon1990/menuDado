# MenuDado

MenuDado es una app Android nativa en Kotlin para guardar menús locales, analizar si son saludables con IA y elegir qué comer con un dado animado.

## Recursos públicos

- [Política de Privacidad](docs/privacy-policy.md)
- GitHub Pages: `https://rhon1990.github.io/menuDado/privacy-policy/`

## Qué incluye

- App Android nativa con Jetpack Compose.
- Persistencia local con Room.
- Formulario para crear menús de desayuno, almuerzo y cena.
- Botón de dado con animación.
- Filtro obligatorio del dado por tipo de comida.
- Resultado aleatorio con "Hoy toca...".
- Análisis saludable con Firebase AI Logic y Gemini, preparado para configurar Firebase.

## Abrir en Android Studio

Abre esta carpeta como proyecto en Android Studio. El nombre Gradle y el nombre de Android Studio están configurados como `MenuDado`.

El proyecto incluye Gradle wrapper. Si Android Studio pide sincronizar, acepta la sincronización.

## Configurar IA con Firebase

1. Crea un proyecto en Firebase.
2. Registra una app Android con el package `com.menudado`.
3. Descarga `google-services.json`.
4. Coloca el archivo en `app/google-services.json`.
5. En Firebase, habilita Firebase AI Logic con Gemini Developer API.
6. Recompila la app.

Sin `google-services.json`, la app permite crear menús locales y usar el dado, pero el análisis IA mostrará error de configuración o conexión.

## Verificación local

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
