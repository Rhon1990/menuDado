# Rotación local de menús de colmena

## Objetivo

Evitar que una misma identidad reciba de nuevo un menú de colmena antes de
recorrer los candidatos compatibles disponibles, sin añadir servicios,
lecturas adicionales ni escrituras en Firebase.

## Decisiones

- El historial se guarda únicamente en el teléfono.
- Se separa por identidad usando los scopes existentes:
  - `guest`
  - `account:<uid>`
- Solo se guardan `semanticHash`; nunca nombres, recetas, ingredientes,
  perfiles, imágenes ni otro contenido del menú.
- Cada identidad conserva como máximo 24 hashes y el último hash mostrado.
- El almacenamiento debe permanecer por debajo de unos pocos KB por identidad.
- La preferencia local se excluye de Android Backup y de transferencias entre
  dispositivos. Desinstalar o borrar los datos reinicia la rotación.
- No se modifica Firestore, sus reglas, Remote Config ni Firebase AI Logic.

## Comportamiento

1. La colmena consulta como máximo los 12 candidatos compatibles actuales.
2. Se descartan candidatos inseguros para el perfil, como hasta ahora.
3. Entre los candidatos seguros se priorizan los que no estén en el historial
   de la identidad activa.
4. El candidato mostrado se añade al historial local.
5. Si todos los candidatos seguros de la consulta ya fueron vistos, la
   rotación se considera completada:
   - se inicia un nuevo ciclo;
   - si existen al menos dos candidatos, se evita elegir inmediatamente el
     último menú mostrado;
   - si solo existe un candidato compatible, se permite repetirlo porque su
     ciclo contiene un único elemento.
6. Cuando el historial supera 24 hashes, se eliminan primero los más antiguos.
   Esto garantiza una ventana amplia de no repetición sin crecimiento
   indefinido.

Los menús semánticamente equivalentes comparten `semanticHash`, por lo que
variantes como “pasta con tomate” y “espaguetis en salsa de tomate” se tratan
como el mismo menú cuando la IA proporciona la misma clave canónica.

## Componentes

### `HiveRotationStore`

Contrato local responsable de:

- leer el snapshot de una identidad;
- registrar un hash mostrado;
- reiniciar el ciclo cuando el repositorio indique que los candidatos
  disponibles ya fueron recorridos;
- limitar el historial a 24 elementos.

La implementación de producción usa `SharedPreferences`. Los tests usan una
implementación en memoria.

### `AiMenuHiveRepository`

Recibe los hashes vistos y el último hash mostrado. Selecciona primero un
candidato no visto. Cuando no quedan candidatos seguros sin ver, devuelve un
candidato de nuevo ciclo evitando el último si hay alternativas, e informa que
el ciclo debe reiniciarse.

### `MenuDadoViewModel`

Resuelve el scope de identidad ya usado por las cuotas, obtiene el snapshot
antes de buscar en la colmena y registra el resultado solo después de haberlo
mostrado correctamente. Cambiar entre invitado y cuentas cambia también el
historial activo.

## Errores y degradación

- Si el historial local no puede leerse, la colmena sigue funcionando con un
  snapshot vacío.
- Si no puede escribirse, el menú se muestra igualmente; la garantía de
  rotación se recuperará en posteriores escrituras válidas.
- Un fallo de almacenamiento local nunca debe bloquear la generación ni el
  fallback de colmena.
- Se conserva el fallback de caché cuando Firestore no responde.

## Validación

- Una identidad recorre todos los candidatos seguros antes de repetir.
- El nuevo ciclo evita repetir consecutivamente si hay más de una alternativa.
- Un único candidato puede repetirse tras completar su ciclo.
- Invitado, cuenta A y cuenta B conservan historiales independientes.
- El historial sobrevive a recrear el ViewModel y reiniciar la app.
- Nunca se guardan más de 24 hashes por identidad.
- Los archivos de preferencias quedan excluidos de backup y transferencia.
- Las suites unitarias, lint, `release` y `releaseDebuggable` siguen pasando.

## Fuera de alcance

- Sincronizar la rotación entre teléfonos.
- Descargar toda la colección de colmena.
- Aumentar el límite de 12 lecturas por consulta.
- Guardar historial remoto por usuario.
