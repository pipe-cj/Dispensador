# Plan de pruebas – Modo Control y diseño
Proyecto: Dispensador de comida IoT (TI3042 U2) · Responsable: Jorge
Equipo de pruebas: 2 teléfonos Android (A = Control, B = Dispensador), misma cuenta, código DISP-01, con internet.

| # | Módulo | Prueba | Pasos | Resultado esperado | Resultado real | OK |
|---|--------|--------|-------|--------------------|----------------|----|
| C1 | Control | Dispensar porción mediana | En A elegir Mediana y tocar "Dispensar ahora" | B muestra "Dispensando..." en menos de 2 s; aparece mensaje "Orden enviada" en A | | [ ] |
| C2 | Control | Cada porción | Repetir C1 con Chica y Grande | El nivel baja 5 %, 10 % y 20 % respectivamente en A y B | | [ ] |
| C3 | Control | Nivel en vivo | Dispensar y mirar A | El círculo y el % se actualizan sin recargar | | [ ] |
| C4 | Control | Estado en línea | Cerrar la app en B | A muestra "Sin conexión" y el botón Dispensar queda desactivado | | [ ] |
| C5 | Control | Nivel bajo | Dejar el nivel en menos de 20 % | Banner rojo y mensaje emergente en A (una vez, no en bucle) | | [ ] |
| C6 | Control | Nivel en 0 | Vaciar el dispensador | Botón Dispensar desactivado | | [ ] |
| C7 | Control | Recarga quita aviso | Tocar "Recargar" en B | A vuelve a 100 % y el banner desaparece | | [ ] |
| C8 | Control | Sin internet en A | Modo avión en A y tocar Dispensar | No se cierra la app; no se pierde el estado al volver la red | | [ ] |
| H1 | Horarios | Agregar | Horarios → + → hora 1 min adelante → porción | Aparece en la lista ordenada por hora | | [ ] |
| H2 | Horarios | Ejecución | Esperar la hora programada | B dispensa solo y queda en el historial como "por horario" | | [ ] |
| H3 | Horarios | Desactivar | Apagar el interruptor de un horario | No se ejecuta a su hora | | [ ] |
| H4 | Horarios | Borrar | Tocar el basurero y confirmar | Desaparece de A y de la consola Firebase | | [ ] |
| H5 | Horarios | Lista vacía | Borrar todos | Aparece el mensaje "Aún no hay horarios" | | [ ] |
| I1 | Historial | Registro | Dispensar desde A | Aparece arriba en el historial con fecha, hora y porción | | [ ] |
| I2 | Historial | Orden | Hacer 3 raciones seguidas | Más reciente primero | | [ ] |
| I3 | Historial | Vacío | Usar un dispensador nuevo | Aparece el mensaje "Aún no hay raciones" | | [ ] |
| N1 | Navegación | Barra inferior | Cambiar entre las 3 pestañas | Sin cierres; cada pantalla carga datos en vivo | | [ ] |
| N2 | Navegación | Rotación | Girar el teléfono | Los datos se mantienen | | [ ] |
| S1 | Seguridad | Sin sesión | Abrir Control sin iniciar sesión | La app no muestra pantallas de Control | | [ ] |
| S2 | Seguridad | Dispensador ajeno | Con otro usuario, intentar leer DISP-01 | Firebase rechaza (permission denied) | | [ ] |
| S3 | Seguridad | Porción inválida | Escribir porcion="xxl" en la consola | Las reglas lo rechazan | | [ ] |
| U1 | Diseño | Pantallas pequeñas | Probar en pantalla de 5" | Nada queda cortado; botón visible | | [ ] |
| U2 | Diseño | Textos | Revisar ortografía y mensajes de error | Mensajes claros, en español | | [ ] |

Registro de errores: anotar fecha, caso, qué pasó y quién lo corrige (Control: Jorge · Dispensador: Luis · Login/reglas: Tomás · Integración: Pipe).
Calendario: pruebas parciales jue 19 y vie 20; prueba completa en 2 teléfonos dom 22.
