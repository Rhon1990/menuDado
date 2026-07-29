# MenuDado 1.3.0: cuenta, Acerca de y versión

## Objetivo

Actualizar la propuesta de valor de la cuenta y la descripción de la app con
mensajes verificables, orientados a activación y retención, y publicar el
cambio visible como `1.3.0` (`versionCode` 14) en la rama
`codex/onboarding-analytics-v6`.

## Decisiones de producto

### Ventajas de la cuenta

La sección deja de presentar como ventaja el uso en otro móvil y elimina
promesas ambiguas sobre guardar menús sin límite. El mensaje se apoya en los
contratos actuales:

- invitado: 5 usos gratuitos de IA diarios cuando el límite remoto está activo;
- cuenta registrada: 10 usos gratuitos de IA diarios;
- generación y análisis comparten el contador;
- existe un máximo técnico local separado de 20 llamadas reales al proveedor;
- una cuenta permite recuperar los datos asociados después de reinstalar.

Copy español aprobado:

- Título: `Tu cuenta te da más`
- Introducción: `Crea tu cuenta gratis para proteger lo que guardas y disponer de 10 usos gratuitos de IA al día.`
- Prompt: `Crea una cuenta para recuperar tus datos si reinstalas la app`
- Ventaja 1: `Con tu cuenta tienes 10 usos gratuitos de IA al día.`
- Ventaja 2: `Recupera tus menús y favoritos si reinstalas la app.`
- Ventaja 3: `Conserva tu perfil alimentario y preferencias en tu cuenta.`
- Ventaja 4: `Mantén guardados tus menús analizados y los productos de su lista de mercado.`

Los textos se traducirán semánticamente a inglés y francés. La lista visible
pasará de seis elementos redundantes a cuatro beneficios diferenciados. El
modal permitirá desplazamiento vertical para conservar el acceso a todo el
contenido con fuente grande, francés o pantallas de poca altura.

### Acerca de la app

La descripción local debe explicar el recorrido completo del producto sin
atribuir a la cuenta capacidades que no sean exclusivas:

`MenuDado nació para resolver una pregunta cotidiana: ¿qué preparo hoy? Genera ideas con IA adaptadas a tu perfil, guarda tus menús, deja que el dado te ayude a elegir y reúne los productos en tu lista de mercado. Puedes empezar sin registrarte y crear una cuenta gratis para conservar tus datos.`

Se actualizarán también las variantes inglesa y francesa. El aviso de salud,
la política de privacidad, la autoría y el contacto no cambian.

Firebase Remote Config conserva prioridad sobre el texto local mediante
`about_description_v2`. Esta clave versionada evita que el valor heredado de
`about_description` reemplace el nuevo fallback localizado. Si se necesita
sobrescribir la descripción de `1.3.0`, deberá publicarse el nuevo texto en
`about_description_v2`; la operación se verificará por separado y no se
afirmará como realizada sin acceso confirmado al proyecto.

## Versión

- `versionName`: `1.3.0`
- `versionCode`: `14`

El incremento menor comunica el onboarding renovado, su analítica y los
cambios visibles de activación. El código se incrementa de forma monotónica
desde `1.2.1 (13)`.

## Alcance técnico

- Actualizar `app/build.gradle.kts`.
- Actualizar los recursos `values`, `values-en` y `values-fr`.
- Reducir la lista de ventajas de cuenta a los cuatro recursos vigentes.
- Actualizar las pruebas unitarias que fijan la lista y la versión.
- Actualizar `docs/project-context.md` con el nuevo copy, sus límites exactos y
  la versión objetivo.
- No modificar autenticación, contadores, Firestore, navegación ni diseño
  visual.

## Validación

- Prueba unitaria de la lista y orden de ventajas.
- Prueba unitaria del valor de `BuildConfig.VERSION_NAME` y de la etiqueta
  `1.3.0 (14)`.
- Compilación de recursos en los tres idiomas.
- Suite `:app:testDebugUnitTest`.
- Build `:app:assembleDebug`.
- Revisión del `output-metadata.json` generado.
- Revisión manual de `Mi zona` y `Acerca de la app` para comprobar ajuste,
  scroll, traducciones y contenido visible.

## Riesgos y mitigaciones

- **Remote Config puede ocultar la nueva descripción local:** verificar el
  valor remoto y reportar la publicación como estado externo independiente.
- **El texto numérico puede quedar desactualizado si cambian los límites:**
  mantener la documentación y los recursos coordinados con
  `AiDailyUsagePolicy`. No comparar el tramo de la cuenta con el del invitado,
  porque `guest_ai_limits_enabled=false` concede 10 usos a ambos.
- **Regresión de espacio en idiomas traducidos:** validar compilación y revisar
  las pantallas con contenido desplazable.
