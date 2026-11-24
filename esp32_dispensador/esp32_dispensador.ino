/*
  Dispensador de comida IoT - TI3042 U2
  ESP32 + servomotor SG90, controlado por Bluetooth Classic desde el telefono "Dispensador".

  Conexiones:
    Servo cafe/negro   -> GND del ESP32
    Servo rojo         -> VIN (5V del USB) del ESP32
    Servo naranja/amar -> GPIO 13
    (Opcional) condensador 470-1000 uF entre VIN y GND si el ESP32 se reinicia al mover el servo.

  Comandos (un caracter, por Bluetooth o por el Monitor Serie a 115200):
    C = porcion chica    M = porcion mediana    G = porcion grande
    P = ping (responde PONG, sirve para saber si esta conectado)
  Cualquier otro caracter se rechaza (validacion de entrada, ISO/IEC 27400).

  Librerias: placa "esp32" de Espressif (Gestor de tarjetas) y "ESP32Servo" de Kevin Harrington.
  Placa: "ESP32 Dev Module". Bluetooth Classic solo existe en el ESP32 normal (WROOM-32),
  NO en ESP32-C3, C6 ni S3.
*/

#include <BluetoothSerial.h>
#include <ESP32Servo.h>

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error "Bluetooth no esta habilitado. Elige la placa 'ESP32 Dev Module'."
#endif

const char* NOMBRE_BT = "DISP-01";   // mismo codigo que usa la app
const int PIN_SERVO = 13;
const int PIN_LED = 2;               // LED azul de la placa

const int ANGULO_CERRADO = 0;
const int ANGULO_ABIERTO = 90;

// Tiempo que la compuerta queda abierta para cada porcion (ajustar probando con comida real)
const int MS_CHICA = 300;
const int MS_MEDIANA = 600;
const int MS_GRANDE = 1000;

BluetoothSerial bt;
Servo servo;
bool ocupado = false;

void responder(const String& msg) {
  Serial.println(msg);
  if (bt.hasClient()) bt.println(msg);
}

void dispensar(char porcion) {
  int ms;
  const char* nombre;
  switch (porcion) {
    case 'C': ms = MS_CHICA;   nombre = "chica";   break;
    case 'M': ms = MS_MEDIANA; nombre = "mediana"; break;
    case 'G': ms = MS_GRANDE;  nombre = "grande";  break;
    default: return;
  }
  if (ocupado) { responder("ERR:OCUPADO"); return; }
  ocupado = true;

  responder(String("DISPENSANDO:") + nombre);
  digitalWrite(PIN_LED, HIGH);
  servo.write(ANGULO_ABIERTO);
  delay(ms);
  servo.write(ANGULO_CERRADO);
  delay(300);                       // deja que el servo vuelva antes de aceptar otra orden
  digitalWrite(PIN_LED, LOW);

  responder(String("OK:") + porcion);
  ocupado = false;
}

void procesar(char c) {
  if (c == '\r' || c == '\n' || c == ' ') return;
  c = toupper(c);
  if (c == 'P') { responder("PONG"); return; }
  if (c == 'C' || c == 'M' || c == 'G') { dispensar(c); return; }
  responder(String("ERR:COMANDO_INVALIDO:") + c);
}

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);

  servo.setPeriodHertz(50);               // SG90 trabaja a 50 Hz
  servo.attach(PIN_SERVO, 500, 2400);     // pulso minimo y maximo del SG90 en microsegundos
  servo.write(ANGULO_CERRADO);

  bt.begin(NOMBRE_BT);
  Serial.println("Listo. Bluetooth visible como DISP-01. Envia C, M, G o P.");
}

void loop() {
  while (bt.available()) procesar((char)bt.read());
  while (Serial.available()) procesar((char)Serial.read());
  delay(10);
}
