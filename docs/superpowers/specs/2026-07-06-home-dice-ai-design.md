# Home Dice IA Design

## Objetivo

Reducir la confusion actual del Home haciendo que el dado responda al modelo mental real de los usuarios: en modo `Generar con IA`, lanzar el dado crea una idea saludable nueva; en modo `Escribir menu`, lanzar el dado elige entre menus guardados.

## Contexto Confirmado

- `docs/project-context.md` describe el dado actual como selector aleatorio de menus guardados.
- El codigo actual muestra `DiceSection` antes de `MenuForm` en `MenuDadoScreen.kt`, por lo que la primera accion fuerte del Home es lanzar el dado.
- `generateMenuIdea()` ya genera un borrador con nombre, descripcion, notas, calorias y analisis saludable en una sola llamada IA.
- `saveMenu()` ya guarda el borrador actual con el analisis precalculado cuando no se modifica.

## Diseno UX

El Home tendra un bloque principal `Que comer hoy` con selector de modo:

- `Generar con IA`, seleccionado por defecto.
- `Escribir menu`.

En modo `Generar con IA`:

- El usuario selecciona tipo de comida y publico.
- Puede escribir ingredientes base opcionales.
- El dado es la accion principal.
- Al lanzar el dado se ejecuta generacion IA usando perfil alimentario, tipo de comida, publico e ingredientes base.
- Al terminar, se abre un modal de detalle de idea generada.
- El modal muestra nombre, descripcion, notas si existen, analisis saludable, calorias y sugerencia.
- Acciones del modal: `Guardar menu` y `Descartar`.

En modo `Escribir menu`:

- Se mantiene el formulario manual actual.
- El dado tambien aparece, pero el texto indica que elige entre menus guardados.
- Al lanzar el dado se mantiene el comportamiento actual: seleccion aleatoria filtrada por tipo y publico y apertura del modal de detalle del menu guardado.

## Contratos De Comportamiento

- El modo inicial debe ser `Generar con IA`.
- Cambiar tipo de comida o publico limpia borradores generados como hoy.
- Guardar una idea generada desde el modal debe reutilizar `saveMenu()` para conservar limites de invitado, analitica, analisis precalculado y sincronizacion.
- Descartar una idea generada debe limpiar el borrador IA sin guardar.
- El dado de IA debe respetar las mismas protecciones actuales: audiencia obligatoria, perfil alimentario, conflictos de ingredientes, limites de invitado, cuota diaria, throttle y errores IA.
- El dado de menus guardados debe conservar la memoria diaria y mensajes actuales.

## Validacion QA

- Unit test: el modo inicial del Home es IA.
- Unit test: generar IA deja una idea pendiente para modal sin guardarla automaticamente.
- Unit test: descartar limpia la idea pendiente y el borrador.
- Unit test: guardar la idea pendiente persiste el menu con analisis y calorias.
- Build/compile: `./gradlew :app:testDebugUnitTest` o verificacion equivalente enfocada.

## Riesgos

- `MenuDadoScreen.kt` es grande; el cambio debe ser puntual y reutilizar componentes existentes.
- El texto debe evitar volver a mezclar conceptos: IA crea ideas nuevas, el dado en modo manual elige guardados.
- El loading de IA no debe decir que el dado prepara la propuesta si eso refuerza una atribucion incorrecta; debe hablar de IA/MenuDado.
