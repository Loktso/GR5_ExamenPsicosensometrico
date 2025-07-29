/* ====================================================
 * PROYECTO: GR5_ExamenPsicosensometrico
 * VERSIÓN: 2.1
 * AUTOR: [Grupo 5: Peña Erick, Pinos Abrahan]
 * DESCRIPCIÓN: Sistema de prueba psicosensometrico con 
 *              configuración dinámica desde Java
 * ====================================================
 */

// ==================== CONSTANTES ====================
// Parte de Erick Peña
const int GR5_PIN_LED = 8;          // Pin digital para el LED
const int GR5_PIN_BUZZER = 9;       // Pin digital para el buzzer (PWM)
const int GR5_PIN_BOTON = 7;        // Pin digital para el botón (INPUT_PULLUP)
const int GR5_PRIMEROS_ESTIMULOS = 5; // Número de estímulos iniciales controlados

// ==================== ESTRUCTURAS ====================
/**
 * Estructura de configuración del sistema
 * @param intensidadCorrectaLed Intensidad requerida para LED (1-3)
 * @param intensidadCorrectaBuzzer Intensidad requerida para buzzer (1-3)
 * @param requiereLed Si el LED debe estar activo en combinación correcta
 * @param requiereBuzzer Si el buzzer debe estar activo en combinación correcta
 * @param velocidad Nivel de velocidad (1: lenta, 2: media, 3: rápida)
 */
struct GR5_Configuracion {
  int intensidadCorrectaLed;
  int intensidadCorrectaBuzzer;
  bool requiereLed;
  bool requiereBuzzer;
  int velocidad;
};

// ==================== VARIABLES GLOBALES ====================
GR5_Configuracion GR5_configActual = {
  2,    // intensidadCorrectaLed (MEDIA)
  2,    // intensidadCorrectaBuzzer (MEDIA)
  true, // requiereLed
  true, // requiereBuzzer
  2     // velocidad (MEDIA)
};

bool GR5_modoActivo = false;                // Estado del sistema
int GR5_contadorEstimulos = 0;              // Contador de estímulos generados
bool GR5_estimuloActualEsCorrecto = false;  // Estado del estímulo actual

// Tiempos de duración según velocidad (ms)
const int GR5_TIEMPOS_VELOCIDAD[3] = {3000, 2000, 1000}; // Lento, Medio, Rápido

//Parte de Abraham Pinos
// ==================== CONFIGURACIÓN INICIAL ====================
void setup() {
  // Inicializar comunicación serial
  Serial.begin(9600);
  while (!Serial) {
    ; // Esperar a que se inicialice el puerto serial
  }

  // Configurar pines
  pinMode(GR5_PIN_LED, OUTPUT);
  pinMode(GR5_PIN_BUZZER, OUTPUT);
  pinMode(GR5_PIN_BOTON, INPUT_PULLUP);

  // Inicializar generador de números aleatorios
  randomSeed(analogRead(0));

  // Mensaje de inicio
  Serial.println("GR5_SISTEMA_INICIADO");
  GR5_ImprimirConfiguracionActual();
}

// ==================== BUCLE PRINCIPAL ====================
void loop() {
  GR5_ProcesarComandosSerial();
  
  if (!GR5_modoActivo) {
    delay(100);
    return;
  }

  GR5_GenerarEstimuloCombinado();
}

// ==================== MÉTODOS PRINCIPALES ====================

/**
 * Procesa los comandos recibidos por el puerto serial
 */
void GR5_ProcesarComandosSerial() {
  if (Serial.available()) {
    String GR5_comando = Serial.readStringUntil('\n');
    GR5_comando.trim();

    // Comando INICIAR
    if (GR5_comando.equalsIgnoreCase("INICIAR")) {
      GR5_modoActivo = true;
      GR5_contadorEstimulos = 0; // Reiniciar contador
      Serial.println("GR5_MODO_ACTIVO");
    } 
    // Comando DETENER
    else if (GR5_comando.equalsIgnoreCase("DETENER")) {
      GR5_modoActivo = false;
      GR5_ApagarEstimulos();
      Serial.println("GR5_MODO_DETENIDO");
    }
    // Comando CONFIG
    else if (GR5_comando.startsWith("CONFIG:")) {
      GR5_ProcesarConfiguracion(GR5_comando.substring(7));
    }
  }
}

/**
 * Procesa la cadena de configuración recibida
 * @param configStr Cadena con formato "LI2,BI1,LT,BF,V2"
 */
void GR5_ProcesarConfiguracion(String configStr) {
  // Buscar componentes en la cadena
  int liIndex = configStr.indexOf("LI");
  int biIndex = configStr.indexOf("BI");
  int ltIndex = configStr.indexOf("LT");
  int bfIndex = configStr.indexOf("BF");
  int vIndex = configStr.indexOf("V");
  
  // Procesar intensidad LED
  if (liIndex != -1) {
    GR5_configActual.intensidadCorrectaLed = configStr.substring(liIndex+2, liIndex+3).toInt();
  }
  
  // Procesar intensidad Buzzer
  if (biIndex != -1) {
    GR5_configActual.intensidadCorrectaBuzzer = configStr.substring(biIndex+2, biIndex+3).toInt();
  }
  
  // Procesar requerimiento LED
  GR5_configActual.requiereLed = (ltIndex != -1);
  
  // Procesar requerimiento Buzzer
  GR5_configActual.requiereBuzzer = (bfIndex == -1);
  
  // Procesar velocidad
  if (vIndex != -1) {
    int velocidad = configStr.substring(vIndex+1, vIndex+2).toInt();
    if (velocidad >= 1 && velocidad <= 3) {
      GR5_configActual.velocidad = velocidad;
    }
  }
  
  Serial.println("GR5_CONFIG_ACTUALIZADA");
  GR5_ImprimirConfiguracionActual();
}

/**
 * Imprime la configuración actual por el puerto serial
 */
void GR5_ImprimirConfiguracionActual() {
  Serial.print("GR5_CONFIG_ACTUAL: LI");
  Serial.print(GR5_configActual.intensidadCorrectaLed);
  Serial.print(", BI");
  Serial.print(GR5_configActual.intensidadCorrectaBuzzer);
  Serial.print(", LED:");
  Serial.print(GR5_configActual.requiereLed ? "SI" : "NO");
  Serial.print(", BZ:");
  Serial.print(GR5_configActual.requiereBuzzer ? "SI" : "NO");
  Serial.print(", VEL:");
  Serial.println(GR5_configActual.velocidad);
}
//Parte de Erick Peña
/**
 * Genera un estímulo combinado aleatorio, asegurando que en los primeros
 * 5 estímulos aparezca al menos una vez la combinación correcta
 */
void GR5_GenerarEstimuloCombinado() {
  bool GR5_activarLed;
  bool GR5_activarBuzzer;
  int GR5_intensidadLed;
  int GR5_intensidadBuzzer;

  // Lógica para garantizar combinación correcta en primeros estímulos
  if (GR5_contadorEstimulos < GR5_PRIMEROS_ESTIMULOS) {
    if (GR5_contadorEstimulos == 2) { // Forzar combinación correcta en 3er estímulo
      GR5_activarLed = GR5_configActual.requiereLed;
      GR5_activarBuzzer = GR5_configActual.requiereBuzzer;
      GR5_intensidadLed = GR5_configActual.intensidadCorrectaLed;
      GR5_intensidadBuzzer = GR5_configActual.intensidadCorrectaBuzzer;
      GR5_estimuloActualEsCorrecto = true;
    } else {
      // Generar estímulo aleatorio normal
      GR5_GenerarEstimuloAleatorio(
        &GR5_activarLed, 
        &GR5_activarBuzzer, 
        &GR5_intensidadLed, 
        &GR5_intensidadBuzzer
      );
    }
    GR5_contadorEstimulos++;
  } else {
    // Comportamiento normal después de los primeros estímulos
    GR5_GenerarEstimuloAleatorio(
      &GR5_activarLed, 
      &GR5_activarBuzzer, 
      &GR5_intensidadLed, 
      &GR5_intensidadBuzzer
    );
  }

  // Activar estímulos físicos
  GR5_ActivarEstimulos(GR5_activarLed, GR5_activarBuzzer, GR5_intensidadLed, GR5_intensidadBuzzer);

  // Enviar información del estímulo
  Serial.print("GR5_ESTIMULO: LED");
  Serial.print(GR5_activarLed ? String(GR5_intensidadLed) : "OFF");
  Serial.print(", BZ");
  Serial.print(GR5_activarBuzzer ? String(GR5_intensidadBuzzer) : "OFF");
  Serial.println(GR5_estimuloActualEsCorrecto ? ", CORRECTO" : ", INCORRECTO");

  // Esperar respuesta del usuario
  bool GR5_presionado = GR5_EsperarRespuesta(GR5_TIEMPOS_VELOCIDAD[GR5_configActual.velocidad - 1]);

  // Evaluar respuesta
  if (GR5_presionado) {
    Serial.println("GR5_RESPUESTA: " + String(GR5_estimuloActualEsCorrecto ? "ACIERTO" : "FALLO"));
  } else {
    Serial.println("GR5_RESPUESTA: " + String(GR5_estimuloActualEsCorrecto ? "FALLO" : "ACIERTO"));
  }

  // Pausa entre estímulos
  delay(GR5_TIEMPOS_VELOCIDAD[GR5_configActual.velocidad - 1] / 2);
  GR5_ApagarEstimulos();
  delay(200);
}

//Parte de Wilman Perugachi
// ==================== MÉTODOS AUXILIARES ====================

/**
 * Genera un estímulo aleatorio según la configuración actual
 */
void GR5_GenerarEstimuloAleatorio(bool* activarLed, bool* activarBuzzer, int* intensidadLed, int* intensidadBuzzer) {
  *activarLed = random(0, 2);
  *activarBuzzer = random(0, 2);
  *intensidadLed = random(1, 4);
  *intensidadBuzzer = random(1, 4);
  
  // Evaluar si es correcto según configuración
  GR5_estimuloActualEsCorrecto = true;
  if (GR5_configActual.requiereLed && !*activarLed) GR5_estimuloActualEsCorrecto = false;
  if (GR5_configActual.requiereBuzzer && !*activarBuzzer) GR5_estimuloActualEsCorrecto = false;
  if (*activarLed && GR5_configActual.intensidadCorrectaLed != *intensidadLed) GR5_estimuloActualEsCorrecto = false;
  if (*activarBuzzer && GR5_configActual.intensidadCorrectaBuzzer != *intensidadBuzzer) GR5_estimuloActualEsCorrecto = false;
}

/**
 * Activa los estímulos físicos según los parámetros
 */
void GR5_ActivarEstimulos(bool activarLed, bool activarBuzzer, int intensidadLed, int intensidadBuzzer) {
  if (activarLed) {
    analogWrite(GR5_PIN_LED, map(intensidadLed, 1, 3, 85, 255));
  } else {
    digitalWrite(GR5_PIN_LED, LOW);
  }
  
  if (activarBuzzer) {
    analogWrite(GR5_PIN_BUZZER, map(intensidadBuzzer, 1, 3, 85, 255));
  } else {
    digitalWrite(GR5_PIN_BUZZER, LOW);
  }
}

//Parte de Erick Peña
/**
 * Espera la respuesta del usuario durante el tiempo especificado
 * @param duracion Tiempo máximo de espera en ms
 * @return true si se presionó el botón, false en caso contrario
 */
bool GR5_EsperarRespuesta(int duracion) {
  unsigned long GR5_inicio = millis();
  while (millis() - GR5_inicio < duracion) {
    if (digitalRead(GR5_PIN_BOTON) == LOW) return true;
    if (Serial.available()) GR5_ProcesarComandosSerial();
  }
  return false;
}

/**
 * Apaga todos los estímulos físicos
 */
void GR5_ApagarEstimulos() {
  digitalWrite(GR5_PIN_LED, LOW);
  digitalWrite(GR5_PIN_BUZZER, LOW);
}