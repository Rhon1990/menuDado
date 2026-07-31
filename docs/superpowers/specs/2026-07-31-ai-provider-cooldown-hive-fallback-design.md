# Generación durante la recuperación de Gemini

## Objetivo

Evitar que la generación de ideas quede bloqueada con `IA descansando` después de que Firebase AI Logic responda `RESOURCE_EXHAUSTED`. MenuDado debe seguir ofreciendo una idea compatible mediante la colmena mientras el proveedor se recupera y volver a probar Gemini cuando finalice el plazo indicado.

## Comportamiento

- Una petición válida intenta Gemini cuando no existe una recuperación activa del proveedor.
- Si Gemini falla, se conserva el flujo actual: validar el fallo, registrar su recuperación y buscar una alternativa compatible en la colmena.
- Mientras exista una recuperación activa, el dado permanece habilitado y la siguiente petición se dirige directamente a la colmena, sin realizar llamadas inútiles a Gemini.
- Cuando vence la recuperación, el siguiente intento vuelve a usar Gemini.
- La pausa local de un segundo no se muestra ni bloquea una nueva generación que comienza después de finalizar la anterior.
- Mientras una generación o un anuncio bonificado estén activos, los toques repetidos continúan bloqueados.
- Si la colmena no devuelve una idea compatible, se muestra un único aviso localizado y no se revela información interna sobre Firebase, cuotas ni la colmena.

## Alcance técnico

El cambio se limita a la generación de ideas en `MenuDadoViewModel`. Los flujos de análisis individual y por lote conservan su protección compartida actual porque no disponen de un fallback equivalente en la colmena.

La generación seguirá consumiendo el uso o crédito que corresponda cuando se inicia una petición válida, tanto si el resultado procede de Gemini como de la colmena. La protección diaria total y la oferta de anuncio bonificado no cambian.

## Flujo de datos

1. Validar tipo de comida, público, perfil e ingredientes.
2. Resolver el acceso diario: gratuito, crédito bonificado, oferta de anuncio o límite total.
3. Si el proveedor puede utilizarse y no está recuperándose, iniciar Gemini.
4. Si Gemini falla, registrar la recuperación aplicable y buscar en la colmena.
5. Si el proveedor ya está recuperándose, omitir Gemini y buscar directamente en la colmena.
6. Revalidar perfil y público antes de mostrar cualquier candidato.

## Validación

- Una segunda generación inmediata después de una respuesta correcta vuelve a llamar a Gemini.
- Una recuperación activa no muestra `IA descansando` ni deshabilita el dado de generación.
- Una recuperación activa evita una nueva llamada a Gemini y consulta la colmena.
- Al vencer la recuperación, la generación vuelve a llamar a Gemini.
- Un fallo real de Gemini continúa consultando la colmena.
- Los dobles toques durante una generación siguen produciendo una única petición.
- Los análisis IA conservan su comportamiento actual.

## Riesgos y límites

- La colmena puede no contener una alternativa compatible; en ese caso se conserva el aviso de indisponibilidad existente.
- El resultado durante la recuperación puede proceder de la colmena, pero la UI no expone ese detalle técnico.
- No se modifican Remote Config, AdMob, Firebase Console ni las cuotas configuradas externamente.
