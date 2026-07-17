# Orden estable de menús y prioridad del último favorito

## Objetivo

Mantener cada carrusel de perfil ordenado del menú más reciente al más antiguo sin que una acción de favorito cambie la posición de sus tarjetas. En la colección Favoritos, mostrar primero el menú marcado como favorito más recientemente.

## Enfoque elegido

Se separan los dos conceptos de orden:

- Los carruseles de Adulto, Peques y Bebé usan exclusivamente `createdAt` descendente.
- Favoritos usa un nuevo campo opcional `favoritedAt` descendente.
- Al activar el corazón se guarda la hora actual en `favoritedAt`; al desactivarlo se elimina esa marca.
- Los favoritos antiguos sin esta marca usan `createdAt` como respaldo para conservar un orden determinista y compatible.

Se descarta ordenar Favoritos por `createdAt`, porque no representa la última selección del usuario. También se descarta mantener el orden solo en memoria, porque se perdería al reiniciar la aplicación o al sincronizar otro dispositivo.

## Persistencia y sincronización

`favoritedAt` se incorpora al modelo de dominio, a Room y al documento Firestore. Room suma una migración no destructiva; los favoritos existentes reciben `createdAt` como valor inicial. Firestore acepta documentos antiguos sin el campo mediante el mismo respaldo.

## Comportamiento y compatibilidad

Marcar o desmarcar un menú no altera `createdAt`, por lo que su tarjeta permanece en la misma posición dentro del perfil. Si se vuelve a marcar un favorito, recibe una nueva fecha y pasa al primer lugar de Favoritos. La visibilidad por perfiles activos se mantiene sin cambios.

## Validación

- Prueba de orden estable en carruseles de perfil al cambiar `isFavorite`.
- Prueba de prioridad por `favoritedAt` en Favoritos y respaldo por `createdAt`.
- Prueba del ciclo activar/desactivar para asignar y limpiar `favoritedAt`.
- Pruebas de conversión Room y serialización Firestore.
- Suite unitaria completa, ensamblado debug y prueba física de ambos carruseles.
