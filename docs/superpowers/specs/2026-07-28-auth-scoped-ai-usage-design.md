# Cuotas de IA separadas por identidad

## Objetivo

Evitar que los usos gratuitos o recompensados consumidos como invitado reduzcan
las cuotas disponibles después de iniciar sesión, y viceversa.

## Comportamiento

- El modo invitado dispone cada día de 5 usos gratuitos de IA y puede obtener
  hasta 10 generaciones adicionales mediante anuncios bonificados.
- Cada cuenta autenticada dispone cada día de 10 usos gratuitos de IA y puede
  obtener hasta 10 generaciones adicionales mediante anuncios bonificados.
- Cambiar entre invitado y cuenta autenticada recupera el saldo anterior de ese
  ámbito; no copia, reinicia ni descuenta el saldo del otro.
- El invitado usa un ámbito local estable del dispositivo. Una cuenta usa un
  ámbito derivado de su UID, por lo que dos cuentas no comparten saldo.
- El límite técnico de 20 solicitudes diarias a Firebase AI Logic permanece
  compartido en el dispositivo para controlar consumo y costes.

## Modelo de datos

Se separan dos conceptos que actualmente están mezclados:

1. **Consumo de producto**: usos gratuitos y libro de créditos recompensados,
   almacenados por fecha y ámbito de identidad.
2. **Consumo del proveedor**: número de solicitudes reales realizadas a
   Firebase AI Logic durante la fecha actual, almacenado localmente y sin
   depender del ámbito.

El ámbito de invitado es estable aunque cambie la sesión anónima de Firebase.
El ámbito autenticado incluye el UID. Los créditos recompensados conservan su
contrato actual de `earnedCount` y `consumedCount`, pero cada ámbito mantiene su
propio libro.

Los datos antiguos sin ámbito se migran una sola vez cuando la identidad inicial
ya está resuelta. Se atribuyen al ámbito activo en ese momento y se registra la
migración para no volver a importar el mismo consumo.

## Flujo de generación

1. MenuDado consulta la cuota gratuita o recompensada del ámbito activo.
2. Si existe derecho a generar, consume únicamente el saldo de ese ámbito.
3. Si quedan solicitudes del proveedor, intenta primero la generación en vivo
   con Firebase y registra esa solicitud en el contador técnico compartido.
4. Si Firebase falla, se mantiene el fallback actual a la colmena.
5. Si el límite técnico compartido ya está agotado, Firebase se considera no
   disponible y se consulta directamente la colmena, evitando una llamada que
   se sabe que no puede realizarse.
6. Si no existe una coincidencia segura en la colmena, se conserva el error
   normal del flujo; nunca se relajan perfil, alergias ni restricciones.

El análisis de un menú no tiene equivalente en la colmena. Por tanto, requiere
cuota gratuita del ámbito y disponibilidad en el contador técnico del proveedor.

## Sincronización y seguridad

- El consumo de invitado no se sube como consumo de una cuenta autenticada.
- La cuota autenticada continúa asociada al UID correspondiente.
- Cambiar repetidamente de sesión no reinicia el invitado ni una cuenta.
- Un anuncio solo concede un crédito al recibir el callback de recompensa.
- El máximo de 10 recompensas se aplica de forma independiente a cada ámbito,
  sin superar las 20 solicitudes reales diarias al proveedor.

## Validación

- Gastar los 5 usos de invitado e iniciar sesión muestra 10 usos gratuitos.
- Gastar o acumular recompensas como invitado no reduce las 10 recompensas de
  la cuenta.
- Cerrar sesión recupera exactamente el saldo anterior del invitado.
- Volver a iniciar sesión recupera exactamente el saldo anterior de la cuenta.
- Dos UID distintos no comparten usos gratuitos ni recompensas.
- Al alcanzar 20 solicitudes reales, generar no llama a Firebase y usa la
  colmena; analizar queda bloqueado.
- La migración de un contador antiguo ocurre una sola vez y no duplica usos.
- Los límites diarios se reinician al cambiar la fecha de referencia.

## Alcance

El cambio se limita a contadores, selección de ámbito, migración local y
decisión Firebase/colmena. No modifica el diseño visual, los bloques de AdMob,
la recompensa `1 Idea IA`, el contenido de los prompts ni las reglas de
seguridad alimentaria.
