# Conservar la última selección del campo Para

## Objetivo

Mantener en el selector `Para` el último público elegido por el usuario cuando
existan dos o más perfiles alimentarios activos, incluso después de guardar o
descartar una idea.

## Comportamiento

- Elegir un público activo actualiza el formulario y la selección local
  persistida existente.
- Guardar un menú limpia los datos propios de la receta, pero conserva el
  público actual si continúa activo.
- Descartar una idea conserva igualmente el público actual.
- Al volver a abrir la app se restaura la última selección persistida únicamente
  si el perfil continúa activo.
- Si el público seleccionado se desactiva:
  - con un único perfil activo, se selecciona automáticamente ese perfil;
  - con varios perfiles activos, el selector queda vacío para no decidir por el
    usuario;
  - sin perfiles activos, el selector queda vacío.

## Enfoque técnico

Se reutilizarán `FormAudienceSelectionStore` y
`selectedOrSingleDefault`. El cambio se limitará al reinicio del formulario en
`MenuDadoViewModel`: en vez de resolver la selección desde `null`, resolverá
desde el público que ya contiene el estado.

No se crearán nuevos stores, componentes UI, eventos Analytics, campos de
Firebase ni migraciones.

## Validación

- Prueba de regresión: después de guardar con varios perfiles activos, el
  selector conserva el último público seleccionado.
- Mantener las pruebas existentes para:
  - restauración entre instancias del `ViewModel`;
  - selección automática cuando solo queda un perfil;
  - invalidación de una selección desactivada.
- Ejecutar la suite unitaria debug y el ensamblado debug.

## Riesgos y mitigación

- **Selección inválida tras cambios de perfil:** se mantiene la validación
  contra `enabledAudiences`.
- **Conservar contenido de una receta anterior:** solo se conserva el público;
  nombre, descripción, ingredientes, análisis y metadatos generados continúan
  limpiándose.
- **Refactor innecesario:** el cambio queda localizado en el reinicio del
  formulario y su prueba.
