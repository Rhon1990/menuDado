# Diseño de microcopy humano para MenuDado

## Objetivo

Reescribir y auditar todos los textos visibles de MenuDado para conectar primero con la necesidad cotidiana de la persona y presentar después el valor de la IA. La app debe sentirse cercana, práctica y tranquilizadora, sin sonar robótica, autoritaria, infantil ni excesivamente promocional.

## Principio narrativo

Cada flujo seguirá, cuando el espacio lo permita, esta jerarquía:

1. **Necesidad humana:** reconocer qué intenta resolver la persona.
2. **Acción sencilla:** explicar qué puede hacer ahora.
3. **Valor de la IA:** contar cómo la IA crea, analiza o personaliza el resultado.

Ejemplo principal:

- Necesidad: `¿No sabes qué cocinar?`
- Acción: `Lanza el dado`
- Valor: `Recibe una idea saludable creada con IA`

La IA no se oculta ni se minimiza. Deja de ser una orden técnica como `Lanzar con IA` y pasa a explicar un beneficio concreto.

## Voz y tono

- Cercano, breve y cotidiano.
- Trato de `tú`, con español neutro y natural.
- Sin emojis, exclamaciones innecesarias ni promesas exageradas.
- Sin términos internos como `timeout`, `throttle`, `tokens`, `JSON` o nombres de errores.
- Verbos de ayuda: `encontrar`, `inspirar`, `revisar`, `guardar`, `recuperar`, `probar`.
- Preguntas solo cuando reconocen una necesidad real; no convertir cada pantalla en un interrogatorio.
- Los botones comienzan con una acción clara y evitan frases largas.
- Los estados de espera cuentan qué está ocurriendo y reducen incertidumbre.
- Los errores explican el problema en lenguaje simple y ofrecen un siguiente paso.

## Alcance

Se auditará el 100 % del contenido visible y accesible:

- Inicio, dado, generación y selección aleatoria.
- Creación manual, edición y detalle de menú.
- Generación, análisis individual y análisis pendiente con IA.
- Lista de mercado.
- Favoritos, carruseles, filtros y estados vacíos.
- Perfil alimentario.
- Registro, inicio de sesión, modo invitado y `Mi zona`.
- Onboarding, información de la app y actualización disponible.
- Cámara, biblioteca, compartir y confirmaciones destructivas.
- Cargas, cuotas, errores, recuperación y mensajes de éxito.
- Etiquetas, ayudas, placeholders, botones y descripciones de accesibilidad.

No se modificarán nombres de producto, recetas ni textos generados dinámicamente por la IA. Tampoco se renombrarán eventos analíticos, rutas, claves internas o contratos remotos.

## Criterio para textos cortos

Auditar todos los textos no significa alargarlos todos. Etiquetas funcionales ya naturales como `Guardar`, `Editar`, `Eliminar`, `Inicio` o `Favoritos` pueden mantenerse si son la opción más clara. El objetivo es cambiar el tono de la experiencia completa sin degradar:

- Escaneabilidad.
- Jerarquía visual.
- Accesibilidad.
- Comprensión de acciones destructivas.
- Precisión legal o sanitaria.

## Aplicación por superficie

### Inicio y dado

- La cabecera parte de `¿No sabes qué cocinar?`.
- La acción principal usa `Lanza el dado`.
- El texto de apoyo explica que la IA crea una idea saludable adaptada a las selecciones.
- La cantidad de usos disponibles se conserva, pero no domina el mensaje.
- La alternativa de menús guardados se presenta como una forma rápida de redescubrir algo que la persona ya guardó.

### Generación con IA

- El loading evita `Generando idea` como frase técnica y cuenta que se está buscando algo rico y adecuado.
- El resultado se presenta como una idea pensada para la persona.
- Reintentar invita a descubrir otra opción, sin ocultar que la IA la crea.
- Los bloqueos por selección incompleta indican qué falta y cómo continuar.

### Análisis con IA

- `Analizar IA` cambia a una acción comprensible como `Revisar con IA`.
- El resultado explica qué observa la IA y cómo podría mejorarse el menú.
- La revisión pendiente se plantea como ayuda para completar menús guardados, no como una tarea obligatoria.

### Mercado

- La pantalla se presenta como una lista que reúne lo necesario para cocinar los menús elegidos.
- Los estados vacíos explican cómo se llena la lista.
- Comprar, restaurar, limpiar e incluir menús conservan verbos directos y consecuencias claras.

### Cuenta y perfil

- El registro se explica desde el beneficio: recuperar menús, favoritos, perfil y lista de mercado.
- El modo invitado se comunica sin presión.
- Los ajustes alimentarios explican que ayudan a recibir ideas más adecuadas.

### Errores, cuota y espera

- Cada mensaje responde: qué ocurrió, qué puede hacer la persona y cuándo puede reintentar si aplica.
- No se culpa a la conexión cuando la causa real puede ser demora o cuota.
- La disponibilidad gratuita de IA se explica con transparencia, sin lenguaje punitivo.

### Confirmaciones, salud y privacidad

- Las confirmaciones destructivas siguen siendo explícitas sobre qué se elimina.
- Los avisos de salud y privacidad conservan todo su significado.
- El tono puede ser más natural, pero nunca ambiguo, persuasivo ni informal en exceso.

## Localización

- Español es la fuente de intención.
- Inglés y francés serán transcreaciones naturales, no traducciones palabra por palabra.
- Se conservarán las claves, placeholders y cantidades de cada recurso.
- Cada idioma debe mantener la misma intención, nivel de cercanía y jerarquía entre necesidad, acción e IA.
- Cualquier idioma no soportado seguirá usando español.

## Restricciones de implementación

- Priorizar cambios en recursos `strings.xml`.
- Mover a recursos cualquier texto visible o accesible que siga hardcodeado, salvo símbolos puramente visuales.
- No cambiar lógica, navegación, persistencia, Firebase, prompts de negocio ni analítica.
- No introducir una capa nueva de copy ni duplicar recursos existentes sin necesidad.
- Ajustar layout únicamente si una traducción natural se corta con la configuración actual.
- No hacer push, crear PR ni modificar servicios remotos.

## Validación

### Automatizada

- Paridad de claves entre español, inglés y francés.
- Paridad de placeholders y plurales.
- Compilación de recursos Android.
- Suite unitaria completa.
- Tests existentes que fijan recursos o textos visibles.
- Escaneo de textos visibles hardcodeados en Compose.

### Visual y manual

- Inicio con acción de IA disponible, bloqueada y en espera.
- Resultado generado y detalle guardado.
- Análisis individual y pendiente.
- Mercado vacío, con pendientes y con comprados.
- Registro, inicio de sesión, invitado y `Mi zona`.
- Perfil, onboarding, errores y confirmación de eliminación.
- Revisión en español, inglés y francés.
- Revisión con tamaño de fuente normal y ampliado.

## Criterios de aceptación

- La primera acción de Inicio conecta con `no saber qué cocinar`.
- Ningún CTA principal presenta la IA como una obligación técnica.
- La IA sigue identificada en generación, análisis, personalización y disponibilidad.
- Todos los textos visibles fueron auditados.
- Los textos modificados son breves, claros, naturales y coherentes entre pantallas.
- Errores y esperas ofrecen un siguiente paso.
- Los avisos legales, de salud y de eliminación mantienen precisión.
- Español, inglés y francés compilan con claves y placeholders equivalentes.
- No hay cambios remotos.
