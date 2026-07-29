# Orden estable de menús y prioridad del último favorito

## Objetivo

Mantener cada carrusel de perfil ordenado del menú más reciente al más antiguo sin que una acción de favorito cambie la posición de sus tarjetas. En la colección Favoritos, mostrar primero el menú marcado como favorito más recientemente.

## Enfoque elegido

Se separan los dos conceptos de orden:

- Los carruseles de Adulto, Peques y Bebé usan exclusivamente `createdAt` descendente.
- Cuando cambia el primer ID por la llegada de un menú nuevo, el carrusel de ese perfil vuelve al índice 0 para mostrarlo.
- Favoritos usa un nuevo campo opcional `favoritedAt` descendente.
- Al activar el corazón se guarda la hora actual en `favoritedAt`; al desactivarlo se elimina esa marca.
- Los favoritos antiguos sin esta marca usan `createdAt` como respaldo para conservar un orden determinista y compatible.

Se descarta ordenar Favoritos por `createdAt`, porque no representa la última selección del usuario. También se descarta mantener el orden solo en memoria, porque se perdería al reiniciar la aplicación o al sincronizar otro dispositivo.

## Persistencia y sincronización

`favoritedAt` se incorpora al modelo de dominio, a Room y al documento Firestore. Room suma una migración no destructiva; los favoritos existentes reciben `createdAt` como valor inicial. Firestore acepta documentos antiguos sin el campo mediante el mismo respaldo.

## Comportamiento y compatibilidad

Marcar o desmarcar un menú no altera `createdAt`, por lo que su tarjeta permanece en la misma posición dentro del perfil. Si se vuelve a marcar un favorito, recibe una nueva fecha, pasa al primer lugar de Favoritos y esa colección vuelve al índice 0 para hacerlo visible. La visibilidad por perfiles activos se mantiene sin cambios.

Después de persistir un menú nuevo en Room, la pantalla muestra una confirmación breve mediante Snackbar mientras la sincronización Firestore continúa de forma cancelable. Las mutaciones remotas se serializan por menú para preservar su orden. El evento se emite solo tras un guardado local real y se consume una vez para evitar repeticiones por recomposición o rotación.

## Validación

- Prueba de orden estable en carruseles de perfil al cambiar `isFavorite`.
- Prueba de cambio del primer ID al agregar un menú nuevo en un perfil.
- Prueba del evento de confirmación tras guardar y de su ausencia cuando el guardado está bloqueado.
- Prueba de que el guardado devuelve tras Room aunque la sincronización remota siga suspendida.
- Prueba de que una eliminación espera el `upsert` pendiente del mismo menú y conserva el orden remoto.
- Prueba de prioridad por `favoritedAt` en Favoritos y respaldo por `createdAt`.
- Prueba del ciclo activar/desactivar para asignar y limpiar `favoritedAt`.
- Pruebas de conversión Room y serialización Firestore.
- Suite unitaria completa, ensamblado debug y prueba física de ambos carruseles.
