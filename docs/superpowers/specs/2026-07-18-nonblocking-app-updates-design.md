# Diseño: actualizaciones persistentes sin bloquear MenuDado

**Fecha:** 2026-07-18  
**Estado:** aprobado para planificación  
**Alcance:** experiencia de actualización desde Google Play

## Objetivo

Mejorar la adopción de nuevas versiones sin impedir que el usuario consulte menús, cocine o use cualquier función de MenuDado. Una actualización disponible debe ser visible y fácil de iniciar, pero nunca debe convertir la aplicación en una pantalla bloqueada.

## Decisión

MenuDado conservará Google Play In-App Updates como única fuente para conocer actualizaciones. Se usará actualización flexible para descargar en segundo plano mientras el usuario continúa utilizando la app. La actualización inmediata no se iniciará como primera opción porque interrumpe el uso.

No se añadirá una versión mínima en Firebase Remote Config ni un control propio de backend. Así se evita una configuración remota capaz de bloquear accidentalmente a todos los usuarios y se mantiene un único origen de verdad: Google Play.

## Experiencia de usuario

### Actualización disponible

Al confirmar Google Play que existe una actualización permitida:

- se muestra una invitación inicial con el nombre `Actualización disponible`;
- la acción principal es `Actualizar ahora`;
- la acción secundaria `Ahora no` cierra la invitación durante la sesión actual;
- cerrar la invitación no bloquea Inicio ni ninguna otra pantalla;
- después de cerrarla, Inicio mantiene un recordatorio compacto y persistente para que la actualización siga siendo descubrible;
- el recordatorio ofrece `Actualizar` y no ocupa el lugar del CTA principal de generación de menús.

La invitación inicial solo aparece una vez por sesión. Al volver a abrir MenuDado en una sesión posterior, se vuelve a mostrar si Play continúa indicando que la actualización está disponible.

### Descarga flexible

Al tocar `Actualizar ahora` o `Actualizar`:

- se inicia el flujo flexible oficial de Google Play si está permitido;
- el usuario puede continuar usando MenuDado durante la descarga;
- el recordatorio persistente cambia a un estado breve de descarga cuando Play informa progreso o instalación pendiente;
- no se muestran diálogos repetidos durante la misma descarga.

### Actualización descargada

Cuando Play informa `DOWNLOADED`:

- se muestra una invitación `Actualización lista` con la acción principal `Instalar y reiniciar`;
- el usuario puede elegir `Más tarde` y continuar utilizando la app;
- Inicio conserva un recordatorio persistente `Actualización lista para instalar` con la acción `Instalar`;
- al instalar, se usa `completeUpdate()` y Google Play reinicia la aplicación.

### Fallback y errores

- Si Play confirma la actualización pero el flujo flexible no puede iniciarse, se abre la ficha productiva de MenuDado en Play Store.
- Si la consulta de Play falla o no hay conexión, MenuDado sigue funcionando y vuelve a consultar al regresar a primer plano.
- Si Play no informa una actualización, no se reserva espacio en Inicio ni se muestra ningún aviso.
- Si la actualización deja de estar disponible, todos los recordatorios se limpian.
- Builds instalados fuera de Play Store no deben quedar bloqueados ni mostrar falsos positivos.

## Arquitectura y estados

`MainActivity` seguirá siendo responsable del contrato con `AppUpdateManager`. Se reemplazarán los booleanos dispersos por un estado de presentación pequeño y explícito:

- `NotAvailable`: no se muestra UI de actualización.
- `Available`: Play ofrece una actualización.
- `Downloading`: el flujo flexible está en curso.
- `Downloaded`: Play terminó la descarga y permite instalar.

El estado se actualizará desde:

1. `appUpdateInfo` al arrancar y al volver a `ON_RESUME`;
2. un `InstallStateUpdatedListener` durante la actualización flexible;
3. el resultado del launcher oficial de Google Play.

La UI tendrá dos niveles reutilizando el mismo estado:

- una invitación modal no bloqueante, visible una vez por sesión para `Available` y nuevamente cuando cambie a `Downloaded`;
- un recordatorio compacto dentro de Inicio mientras el estado sea `Available`, `Downloading` o `Downloaded`.

El recordatorio llegará a `MenuDadoScreen` mediante parámetros de presentación y callbacks, sin introducir dependencias de Google Play en el `ViewModel` ni en los componentes de dominio. Esto mantiene la integración Android en `MainActivity` y la UI reusable/testeable.

## Prioridad visual

El recordatorio usará la paleta existente de MenuDado:

- fondo verde de selección o superficie cálida;
- texto principal en tinta;
- botón verde de marca;
- iconografía simple de descarga/actualización si ya existe un recurso reutilizable.

Se ubicará al comienzo del contenido de Inicio, debajo de la cabecera y antes de `Qué comer hoy`. Será compacto y no competirá con `Generar con IA`, que continúa siendo el CTA principal del producto.

## Analítica

Se conserva `app_update_prompt` y sus acciones cerradas:

- `shown`: invitación inicial mostrada;
- `update`: usuario inicia la actualización;
- `later`: usuario pospone la invitación o instalación;
- `install`: usuario instala una descarga terminada.

El recordatorio persistente reutiliza `update` o `install` cuando se toca. No se enviarán versiones, identificadores ni texto libre.

## Idiomas

Todos los textos nuevos o ajustados se definirán en español, inglés y francés mediante recursos Android. No se introducirán cadenas visibles directamente en Kotlin.

## Pruebas

La implementación debe cubrir mediante TDD:

- traducción de disponibilidad y estado de instalación a `NotAvailable`, `Available`, `Downloading` y `Downloaded`;
- uso exclusivo de `FLEXIBLE`; si no está permitido, la acción abre la ficha de Play Store sin bloquear la app;
- invitación inicial visible una sola vez por sesión;
- posponer cierra la invitación, pero conserva el recordatorio en Inicio;
- estado descargado ofrece instalación y puede posponerse sin bloquear;
- fallo de consulta permite usar la app;
- recordatorio ausente cuando no hay actualización;
- acciones analíticas cerradas y URLs productivas sin cambios.

La verificación final incluirá tests unitarios, compilación `debug` y `releaseDebuggable`, y una prueba manual guiada con el flujo de actualización disponible mediante Play o un entorno de prueba compatible. Si Play no ofrece una actualización a una instalación local, se documentará esa limitación y se validarán los estados con pruebas automatizadas.

## Fuera de alcance

- bloquear el uso de MenuDado;
- Firebase Remote Config para una versión mínima;
- cambios en Room, Firestore o datos del usuario;
- descargar APKs directamente;
- comparar versiones mediante scraping de la página pública de Play Store;
- cambiar versionCode o versionName como parte de esta funcionalidad.
