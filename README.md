# Dispensador de comida IoT – TI3042 Unidad 2

App Android (Kotlin) que controla y monitorea un dispensador de comida para mascotas.
Dos teléfonos Android se comunican por WiFi a través de Firebase, y el teléfono "Dispensador"
mueve un servomotor conectado a un ESP32 por Bluetooth.

```
Teléfono A (Modo Control) ⇄ WiFi ⇄ Firebase (Auth + Realtime DB) ⇄ WiFi ⇄ Teléfono B (Modo Dispensador) → Bluetooth → ESP32 + servo
```

## Funcionalidades

| Pantalla | Qué hace |
|---|---|
| Login / Registro | Correo y contraseña con Firebase Authentication, validaciones, recuperar contraseña |
| Elegir modo | Código del dispensador (`DISP-01`) y modo Control o Dispensador |
| Control | Dispensar porción chica, mediana o grande; nivel de comida y estado en línea en vivo; aviso de nivel bajo |
| Horarios | Agregar, activar/desactivar y borrar horarios de comida |
| Historial | Raciones con fecha, hora, porción y origen (app u horario) |
| Dispensador | Recibe órdenes, las envía al ESP32, descuenta el nivel, ejecuta horarios, botón Recargar |

## Herramientas y justificación

| Decisión | Elección | Motivo |
|---|---|---|
| Lenguaje / IDE | Kotlin + Android Studio | Estándar oficial de Android |
| Comunicación entre teléfonos | WiFi vía Firebase Realtime Database | Funciona a cualquier distancia, tiempo real, cifrado TLS incluido |
| Comunicación con el aparato | Bluetooth Classic (SPP) con ESP32 | Corta distancia, bajo consumo, no necesita red |
| Autenticación | Firebase Authentication | Usuarios compartidos entre teléfonos; contraseñas con hash en la nube |
| Almacenamiento | Firebase Realtime Database | Misma plataforma, consulta en tiempo real, sin servidor propio |
| Interfaz | Material Design 3, ViewBinding, RecyclerView | Guías de diseño de Android |

## Seguridad (ISO/IEC 27400)

- **Autenticación obligatoria:** todas las pantallas heredan de `SesionActivity`; sin sesión vuelven al login.
- **Control de acceso:** `database.rules.json` solo deja leer y escribir el dispensador vinculado al usuario.
- **Cifrado:** HTTPS/TLS en todo el tráfico (`usesCleartextTraffic="false"`); contraseñas nunca guardadas en el teléfono.
- **Validación de entradas:** en la app (`Validaciones.kt`), en las reglas de Firebase y en el ESP32 (solo acepta C, M, G, P).
- **Mensajes seguros:** el login no revela si un correo existe.
- **Historial inmodificable:** las reglas solo permiten agregar eventos, no editarlos ni borrarlos.
- **Mínimos permisos y sin respaldo:** solo Internet y Bluetooth; `allowBackup="false"`.
- **Limitación conocida:** cualquiera que conozca el código del dispensador puede vincularse. Mejora futura: lista de miembros aprobados por el dueño.

## Cómo ejecutar

1. Abrir el proyecto en Android Studio y poner `google-services.json` en `app/`.
2. En Firebase: activar *Correo/contraseña* y publicar `database.rules.json`.
3. Subir `esp32_dispensador/esp32_dispensador.ino` al ESP32 (placa *ESP32 Dev Module*, librería *ESP32Servo*).
4. Vincular el teléfono B con `DISP-01` en Ajustes > Bluetooth.
5. Teléfono A → Modo Control. Teléfono B → Modo Dispensador.

## Circuito

| Servo SG90 | ESP32 |
|---|---|
| Café / negro | GND |
| Rojo | VIN (5 V) |
| Naranja / amarillo | GPIO 13 |

## Equipo

- Felipe Carvajal (Pipe): base del proyecto, integración, Firebase y ESP32
- Tomás: login y seguridad
- Jorge: modo Control, diseño y plan de pruebas
- Luis: modo Dispensador

Documentos: `docs/mockup_pantallas.html` (mockup) y `docs/plan_de_pruebas.md` (pruebas).
