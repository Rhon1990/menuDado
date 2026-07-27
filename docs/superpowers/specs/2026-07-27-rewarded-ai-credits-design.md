# Diseño: créditos IA mediante anuncios recompensados

**Fecha:** 2026-07-27  
**Estado:** aprobado para especificación; pendiente de revisión del documento  
**Alcance:** límites diarios de IA, anuncio recompensado opcional y configuración de AdMob.

## Objetivo

Mantener un límite de coste previsible para Gemini y permitir que la persona usuaria obtenga más ideas de menú tras alcanzar su cuota gratuita, mediante un anuncio recompensado voluntario. La recompensa conserva el mismo flujo de generación actual: Gemini primero y colmena compatible solo cuando Gemini no responde.

## Límites y créditos

| Perfil | Usos IA gratuitos por día | Créditos por anuncios por día | Máximo de solicitudes Gemini por día |
| --- | ---: | ---: | ---: |
| Cuenta | 10 | 10 | 20 |
| Invitado | 5 | 10 | 15 |

- Un uso gratuito cuenta cualquier solicitud real a Gemini: generar, analizar un menú o análisis por lote.
- Un crédito recompensado permite exclusivamente una generación de idea de menú, nunca un análisis.
- Cada crédito se consume al iniciar la solicitud a Gemini, aunque Gemini falle después. Es necesario porque ya se ha consumido una solicitud del proveedor.
- Ningún flujo supera el techo técnico de 20 solicitudes por día. La fecha de reinicio conserva el criterio actual de medianoche del Pacífico.
- Los límites de IA de invitado se separan de los límites existentes de guardar menús para no restringir accidentalmente el guardado al activar esta función.

## Experiencia de usuario

1. Antes del límite, el dado muestra su CTA habitual y conserva la cuenta de usos restantes.
2. Cuando una generación ya no puede usar cuota gratuita, pero quedan recompensas diarias, el bloque muestra `Límite gratuito de hoy alcanzado` y el botón activo `Ver anuncio · desbloquea 1 idea IA`.
3. Al tocarlo, la app carga o muestra un anuncio Rewarded. El usuario puede cerrar o no completar el anuncio; en ese caso no recibe crédito ni se ejecuta una solicitud IA.
4. Al recibir `onUserEarnedReward`, la app concede un crédito y reanuda automáticamente la misma petición ya validada: tipo de comida, público, perfil e ingredientes base.
5. La petición recompensada invoca Gemini. Si Gemini devuelve una propuesta válida se muestra el modal actual. Si falla por proveedor, red o timeout, se aplica el respaldo de colmena ya implementado y solo se acepta una receta compatible.
6. Si se agotaron los diez anuncios recompensados o se alcanzó el techo técnico, el bloque explica que se alcanzó el máximo de ideas IA de hoy y muestra la hora de reinicio. En ese punto no se muestra un CTA de anuncio que no pueda conceder valor.

El texto visible no afirma que ver un anuncio apoye a MenuDado ni incentiva el clic por ese motivo; solo describe la recompensa. El usuario elige siempre si desea ver el anuncio.

## Arquitectura

### Contabilidad de uso

Se añade un estado diario local específico para recompensas: créditos obtenidos y créditos consumidos. El contador existente de solicitudes reales sigue siendo la fuente de verdad para el techo técnico.

La elegibilidad para generar queda así:

- Si aún hay cuota gratuita global, se genera normalmente.
- Si la cuota gratuita se agotó y hay un crédito recompensado no consumido, se permite una única generación y se marca ese crédito como consumido.
- Si no hay crédito, la interfaz ofrece el anuncio siempre que queden recompensas diarias y la publicidad esté disponible.
- Los análisis no leen ni consumen créditos recompensados.

El almacenamiento local es suficiente para evitar infraestructura nueva. No ofrece garantía antifraude entre dispositivos o tras borrar datos; el techo de Gemini limita el consumo técnico. La verificación de servidor de anuncios queda fuera de alcance porque exige un endpoint propio y mantenimiento adicional.

### Publicidad

Se amplía el paquete `ads` actual con un controlador de Rewarded que reutiliza el consentimiento UMP y la inicialización de Mobile Ads existentes.

- Debug y `releaseDebuggable` usan el ad unit Rewarded de prueba oficial de Google.
- Release usa un nuevo ad unit Rewarded de la aplicación MenuDado creado en AdMob.
- La unidad se configura con recompensa `1 Idea IA` y frecuencia máxima de 10 impresiones recompensadas por usuario/día. El límite local conserva la misma barrera aunque AdMob no entregue una impresión.
- Un parámetro de Remote Config `rewarded_ai_enabled` permite activar o detener la oferta sin nueva versión. Su valor local por defecto es `false`.
- Un parámetro independiente `guest_ai_limits_enabled` controla la cuota IA de invitados sin cambiar límites de guardado.

Los eventos de analítica solo registran el estado agregado de la oferta (`shown`, `unavailable`, `dismissed`, `earned`, `generation_started`) y el saldo de recompensas. No incluyen recetas, ingredientes, identificadores publicitarios, perfiles ni texto introducido.

## Errores y seguridad

- No hay anuncio cargado, fallo de carga o consentimiento que no permite anuncios: se explica que el vídeo no está disponible y no se concede crédito.
- El usuario cierra el vídeo antes de obtener recompensa: no se concede crédito ni se ejecuta IA.
- Gemini falla tras obtener crédito: se ejecuta la colmena exactamente con sus reglas actuales; si tampoco hay candidato compatible se muestra el error seguro existente.
- No se relajan restricciones alimentarias, alergias, embarazo, veganismo, idioma, público ni ingredientes evitados.
- La configuración de AdMob se realiza en la aplicación de Producción existente. No se añaden mediación, compras, Cloud Functions, embeddings ni llamadas Gemini adicionales fuera de la generación recompensada autorizada.

## Validación

- Pruebas unitarias de cálculo de cuotas: cuenta, invitado, créditos obtenidos/consumidos, análisis sin créditos y techo de 20.
- Pruebas de ViewModel: CTA de recompensa, anuncio no disponible, recompensa completada, cancelación, reintento Gemini y fallback a colmena.
- Pruebas del controlador de anuncios con IDs demo en Debug; nunca usar el ID real durante QA local.
- Prueba manual con Ad Inspector y dispositivo de prueba: consentimiento, carga, cierre, recompensa y reutilización del dado.
- Verificación de Remote Config en Debug y Producción, reglas Firestore sin cambios funcionales y build/lint Release.

## Fuera de alcance

- Dar recompensas a cambio de análisis IA.
- Recompensar una visualización incompleta.
- Migrar a verificación server-side de anuncios.
- Prometer disponibilidad de anuncios o una receta cuando no exista conexión, anuncio o candidato de colmena compatible.
