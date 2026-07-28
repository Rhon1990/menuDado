# Transición del dado tras un anuncio bonificado

## Objetivo

Después de cerrar correctamente un anuncio bonificado, MenuDado debe mostrar el dado 3D girando durante al menos 1,5 segundos antes de revelar el menú generado con IA. El ajuste solo se aplica a la generación desbloqueada por ese anuncio.

## Alcance

- La recompensa se conserva cuando Google Mobile Ads la entrega.
- La generación no comienza visualmente hasta que se cierra el anuncio a pantalla completa.
- Al volver a MenuDado, la generación y el tiempo mínimo de animación comienzan en paralelo.
- El modal se muestra cuando la IA ha terminado y han transcurrido al menos 1,5 segundos.
- Si la IA tarda más, el dado continúa girando hasta recibir el resultado.
- Los flujos de generación gratuita, generación mediante un crédito bonificado ya disponible, reintento, fallback de colmena y selección de menús guardados mantienen su comportamiento actual.
- Cerrar el anuncio sin recompensa o no poder mostrarlo no inicia la animación ni la generación.

## Diseño técnico

### Cierre del anuncio

`MenuDadoRewardedAd` separará la recepción de la recompensa del cierre visual. Mantendrá un indicador interno cuando se reciba la recompensa y notificará `onRewardEarned` una sola vez al cerrarse el anuncio. Si se cierra sin recompensa, conservará `onDismissedWithoutReward`.

### Presentación bonificada

`MenuDadoViewModel` iniciará la petición validada con una duración mínima de presentación únicamente desde `onRewardedGenerationEarned()`. La petición de IA y el temporizador de 1,5 segundos se ejecutarán en paralelo.

El estado seguirá usando la fase activa de generación existente, por lo que Compose reutilizará `AiGenerationLoadingOverlay` y el dado 3D actual. Si el resultado llega antes del mínimo, el borrador permanecerá oculto hasta completar el tiempo. Si llega después, se mostrará en cuanto finalice la petición.

La duración mínima será una constante interna con nombre específico del flujo bonificado. El camino normal usará una duración mínima de cero y no cambiará.

## Errores y cancelación

- Un fallo de IA conserva el tratamiento y el fallback existentes; el mínimo visual no convierte un error en éxito.
- Si el anuncio falla al mostrarse, se mantiene el aviso de indisponibilidad actual.
- Si el anuncio se cierra sin recompensa, se limpia la solicitud pendiente sin consumir crédito.
- Los callbacks del anuncio continuarán siendo idempotentes para evitar doble generación.

## Pruebas

- La recompensa no reanuda la generación antes del cierre del anuncio.
- El cierre con recompensa notifica una única recompensa.
- El cierre sin recompensa conserva el comportamiento actual.
- Una generación bonificada rápida mantiene el estado de carga durante 1,5 segundos y después muestra el detalle.
- Una generación normal rápida muestra el detalle sin aplicar el mínimo bonificado.
- Una generación bonificada lenta no añade una espera adicional después de terminar la IA.

## Riesgos de regresión

- Doble callback entre la recompensa y el cierre del anuncio.
- Aplicar accidentalmente el retraso a generaciones no bonificadas.
- Mostrar simultáneamente el overlay y el modal.

Estos riesgos se limitan con estado explícito en el controlador, un parámetro de presentación exclusivo del camino bonificado y pruebas con reloj virtual.
