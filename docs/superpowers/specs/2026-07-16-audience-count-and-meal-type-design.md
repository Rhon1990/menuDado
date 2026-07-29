# Diseño: contador por perfil y tipo de comida en tarjetas

## Objetivo

Facilitar el escaneo de `Tus menús` mostrando cuántos menús tiene cada perfil y si cada tarjeta corresponde a desayuno, almuerzo o cena.

## Diseño aprobado

- Cada cabecera de perfil (`Adulto`, `Peques` o `Bebé`) muestra junto al título el mismo badge compacto usado por `Favoritos`.
- El badge usa el total real de menús del perfil, aunque el carrusel de Inicio solo muestre los 10 más recientes.
- Se reutiliza el plural existente `%d menú/menús` en español, inglés y francés.
- Cada tarjeta de perfil muestra siempre el tipo de comida debajo del nombre y antes del estado saludable y las calorías.
- En pantallas donde la tarjeta necesita identificar también el público, el metadato conserva el formato `Tipo · Público`.
- No cambian orden, navegación, acciones, datos ni sincronización.

## Accesibilidad y UX

- El contador combina texto y número; no depende únicamente del color.
- El tipo de comida usa texto secundario legible y localizado.
- La nueva línea no reduce los objetivos táctiles del corazón ni del menú de tres puntos.

## Validación

- Prueba unitaria del total por perfil, incluyendo listas mayores que el límite visible del carrusel.
- Prueba unitaria del recurso de tipo usado por la tarjeta.
- Suite unitaria debug, `assembleDebug`, `git diff --check` y revisión visual con perfiles y tipos distintos.
