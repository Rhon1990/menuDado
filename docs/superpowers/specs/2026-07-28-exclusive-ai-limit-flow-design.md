# Flujo exclusivo de límites de IA y anuncios

## Objetivo

Evitar que MenuDado muestre al mismo tiempo el dado de carga, el detalle de una
idea y un aviso de pausa o límite. El flujo debe entregar valor antes de mostrar
un error y conservar las alternativas manuales cuando ya no sea posible generar
más ideas con IA.

## Contrato de producto

### Mientras quedan usos gratuitos

- Tocar el dado inicia una única generación.
- Durante la petición solo se muestra el dado de carga.
- El resultado abre un único detalle de menú.

### Al agotar los usos gratuitos

- Si hay anuncios bonificados disponibles, el dado ofrece voluntariamente ver
  un anuncio para obtener una idea adicional.
- Completar el anuncio concede exactamente un intento.
- La generación comienza al cerrar el anuncio y conserva la transición vigente:
  el dado gira durante al menos 1,5 segundos antes de revelar el resultado.
- Cerrar el anuncio sin recompensa o no poder mostrarlo no consume crédito ni
  inicia una llamada de IA. Se muestra un único aviso y el usuario permanece en
  Inicio.

### Si el proveedor de IA entra en pausa

- La pausa se registra internamente para impedir nuevas llamadas innecesarias.
- Mientras se busca una alternativa compatible en la colmena, solo permanece
  visible el dado de carga.
- Si existe una alternativa segura, se abre directamente el detalle del menú y
  no se muestra el aviso de pausa.
- Si no existe una alternativa, termina la carga y aparece un único aviso de
  pausa.
- Durante la pausa no se ofrecen anuncios que no puedan convertirse de forma
  fiable en una nueva idea.

### Al agotar los anuncios o los intentos adicionales

- Cada ámbito de identidad conserva el máximo actual de 10 recompensas diarias.
- Cuando se consumen todas, el dado deja de ofrecer anuncios y muestra el máximo
  diario alcanzado sin abrir automáticamente otro modal.
- Si temporalmente no hay inventario de anuncios, no se concede ni se consume
  un intento; el CTA puede volver a estar disponible cuando se cargue un anuncio.
- Si la función de anuncios está desactivada o no puede utilizarse, la app trata
  la generación como limitada y no promete una recompensa.

### Al agotar todos los límites de generación IA

- Se desactiva únicamente la generación de nuevas ideas con IA hasta el reinicio
  diario vigente.
- Inicio sigue operativo: el usuario puede escribir y guardar un menú manual o
  dejar que el dado elija entre sus menús guardados.
- Los menús existentes, Mercado, Perfil y Mi zona continúan disponibles.
- El límite no debe provocar bucles de modales ni bloquear la navegación.

## Diseño técnico

### Aviso de fallo diferido

`MenuDadoViewModel` separará el registro interno de una pausa de IA de su
presentación visible. El estado de reintento y el retroceso de cuota se aplicarán
una sola vez antes de consultar la colmena, pero el mensaje se revelará solo si
la búsqueda alternativa termina sin resultado.

Un resultado de colmena limpiará cualquier mensaje transitorio y conservará el
estado interno necesario para no volver a llamar al proveedor durante la pausa.

### Presentación exclusiva

`MenuDadoScreen` derivará una única superficie principal del flujo de IA con
esta prioridad:

1. Dado de carga mientras la generación o el fallback estén activos.
2. Detalle del menú cuando exista un resultado listo.
3. Aviso de error, pausa o límite cuando no exista resultado.
4. Ninguna superficie modal en el resto de casos.

La prioridad se expresará mediante una función pura y probada. Los avisos
generales no se renderizarán mientras el dado de carga o el detalle generado
sean la superficie activa.

## Analítica

Se mantienen los eventos actuales de oferta, recompensa, cierre, indisponibilidad
y comienzo de generación. El cambio no enviará contenido del menú ni datos
publicitarios nuevos.

## Pruebas

- Un fallo de cuota con fallback exitoso no presenta un aviso y abre el menú.
- Un fallo de cuota sin fallback presenta un único aviso al terminar la carga.
- Carga, detalle generado y aviso son mutuamente excluyentes.
- La generación bonificada conserva el mínimo visual de 1,5 segundos.
- Cerrar o fallar un anuncio no consume crédito ni inicia generación.
- Agotar 10 recompensas cambia el dado a máximo diario sin ofrecer otro anuncio.
- Los flujos gratuitos y la selección de menús guardados no cambian.

## Riesgos y límites

- El cambio no modifica IDs de AdMob, Remote Config, Firebase AI Logic, prompts,
  contadores diarios ni el número máximo de recompensas.
- Se evita una máquina de estados global para no ampliar el riesgo de esta
  versión; la exclusividad se limita a las superficies del flujo IA.
- La disponibilidad real de anuncios y el comportamiento visual deben validarse
  además en un dispositivo con el bloque demo de `releaseDebuggable`.
