# Diseño: mejora de calidad del prompt de generación IA

## Objetivo

Elevar la probabilidad de que cada generación produzca un menú específico, ejecutable y coherente con las preferencias del usuario, sin aumentar el número de llamadas al modelo ni cambiar el contrato de datos existente.

La mejora se limita a reorganizar y precisar `MenuGenerationPrompt`. Una acción de generación seguirá ejecutando una única llamada a `generateContent` y continuará usando el parser actual.

## Fuera de alcance

- Cambiar el proveedor o el modelo de IA.
- Añadir reintentos, una segunda generación, autocorrección o validación mediante otra llamada al modelo.
- Modificar el JSON, `GeneratedMenuParser`, las entidades de dominio o la UI.
- Prometer una exactitud garantizada o precisión médica.
- Añadir nuevas llamadas para imágenes, analítica o enriquecimiento de contenido.

## Arquitectura preservada

El flujo se mantiene sin nuevas capas ni solicitudes:

`MenuDadoViewModel -> Repository -> FirebaseHealthAnalyzer -> generateContent (1 llamada) -> GeneratedMenuParser`

La lista de ideas que deben evitarse conserva el máximo actual de ocho elementos.

## Contrato del prompt

El prompt se organizará en bloques breves y priorizados para reducir ambigüedad y evitar instrucciones repetidas.

### 1. Objetivo y contexto

Se solicitará una sola propuesta de comida apropiada para el tipo de comida, audiencia, idioma, ingredientes disponibles y restricciones recibidas.

### 2. Restricciones obligatorias y prioridad

Si dos instrucciones entran en tensión, se aplicará este orden:

1. Seguridad alimentaria y restricciones dietéticas o alergias.
2. Audiencia, edad y condiciones especiales como embarazo o alimentación infantil.
3. Tipo de comida solicitado.
4. Uso razonable de los ingredientes base.
5. Practicidad, variedad y atractivo de la propuesta.

Las condiciones actuales de cena se conservan: máximo diez minutos, hasta cinco ingredientes principales y sin horno, múltiples acompañamientos ni procesos largos. Las reglas existentes para desayuno, almuerzo, adultos, niños, bebés, embarazo, veganismo y alergias también se mantienen.

### 3. Utilidad de la respuesta

La respuesta deberá cumplir estos criterios dentro de los campos actuales:

- `name`: nombre concreto y reconocible; no una categoría genérica ni una repetición literal de los ingredientes.
- `description`: ingredientes comunes con cantidades aproximadas para una porción adecuada a la audiencia y una preparación breve y accionable.
- `notes`: tiempo estimado de preparación y un consejo útil de sustitución, conservación o servicio; no debe repetir la descripción.
- `calories`: estimación realista para una porción adecuada a la audiencia.
- `health_status`, `health_reason` y `health_suggestion`: evaluación breve, comprensible y no alarmista, sin presentar consejo médico.

La preparación debe ser compacta para no inflar la salida: de dos a cuatro indicaciones breves dentro del `description`, sin ampliar el esquema.

### 4. Variedad real

Cuando existan ideas previas, la nueva propuesta no podrá limitarse a cambiar el nombre. Debe variar al menos una dimensión relevante, como ingrediente base, proteína, técnica o estilo del plato, manteniendo las restricciones obligatorias.

Se seguirá enviando un máximo de ocho ideas previas, tal como hace el flujo actual.

### 5. Salida estricta

El modelo devolverá exclusivamente un objeto JSON válido, sin Markdown, comentarios, texto adicional ni campos nuevos. Se mantienen exactamente estos campos:

```json
{
  "name": "",
  "description": "",
  "notes": "",
  "calories": 0,
  "health_status": "",
  "health_reason": "",
  "health_suggestion": ""
}
```

Todo el texto generado deberá usar el idioma solicitado: español, inglés o francés.

## Presupuesto de llamadas y tokens

- Una interacción del usuario produce una sola llamada a `generateContent`.
- No se añade validación, reintento ni crítica mediante IA.
- El prompt elimina redundancias y usa bloques compactos con prioridades explícitas.
- No incluye ejemplos extensos de recetas.
- El JSON y la lista máxima de ocho ideas previas permanecen acotados.

Esto puede aumentar moderadamente los tokens de instrucción por llamada, pero no el número de llamadas. El incremento se controla sustituyendo reglas repetidas por un contrato único y conciso.

## Errores y analítica

El manejo actual de timeout, cuota, errores de red y parsing no cambia. Tampoco se añaden ni modifican eventos analíticos como parte de este alcance.

Los porcentajes comentados anteriormente son estimaciones técnicas, no resultados medidos. El impacto real deberá observarse con la relación entre `save_generated_menu` y los eventos `try_another_generated_menu` y `discard_generated_menu`, sin generar tráfico artificial al modelo.

## Estrategia QA

La implementación seguirá TDD:

1. Añadir pruebas fallidas en `MenuGenerationPromptTest` para el nuevo contrato: nombre específico, cantidades aproximadas, preparación accionable, tiempo de preparación, consejo útil, prioridad de restricciones, variedad real, idioma y JSON estricto.
2. Modificar únicamente `MenuGenerationPrompt` hasta satisfacer esas pruebas.
3. Ejecutar las pruebas existentes del prompt para comprobar que siguen presentes las reglas de cena, restricciones, ingredientes, audiencia, embarazo, salud, variedad e idioma.
4. Ejecutar las pruebas del ViewModel que verifican el número de generaciones y confirmar que una acción sigue produciendo una sola llamada al analizador.
5. Ejecutar tests, compilación y ensamblado de la variante aplicable; comparar lint con la línea base conocida si aparecen incidencias preexistentes.

## Criterios de aceptación

- El prompt exige una receta concreta, cantidades aproximadas, preparación breve y tiempo estimado.
- Las restricciones de seguridad tienen prioridad explícita sobre variedad o atractivo.
- La respuesta conserva exactamente el esquema JSON que consume el parser actual.
- Todo el contenido se solicita en el idioma configurado.
- Una acción de generación continúa realizando exactamente una llamada al modelo.
- No se añaden reintentos, nuevas dependencias ni cambios en UI, dominio o persistencia.
- Las pruebas existentes y las nuevas pruebas del contrato quedan en verde.

## Riesgos y mitigación

- **Prompt demasiado largo:** se mitiga agrupando reglas y eliminando repeticiones y ejemplos extensos.
- **Instrucciones contradictorias:** se mitiga con un orden de prioridad explícito.
- **Salida más verbosa:** se limita la preparación a indicaciones breves y se conserva el mismo esquema.
- **Mejora no cuantificada:** se evita presentar estimaciones como métricas reales y se propone medir aceptación y regeneración en producción.

## Justificación técnica

Este enfoque tiene impacto mínimo porque mejora el contrato en el único punto que construye la solicitud, reutiliza el parser y el flujo actuales y no altera arquitectura ni costes por número de llamadas. Las pruebas convierten los criterios de calidad en requisitos revisables y reducen el riesgo de perder restricciones ya soportadas.
