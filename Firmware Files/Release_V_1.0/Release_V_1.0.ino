#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WebSocketsServer.h>
#include <Hash.h>
#include <ArduinoJson.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include "memory.h"

#define CHARGE_PIN 14
#define PI_POWER 5

WebSocketsServer webSocket = WebSocketsServer(81);
ESP8266WebServer server(80);
Memory m;

const char* host = "ipower";
bool isRunning = false;
const char* serverIndex = "<form method='POST' action='/update' enctype='multipart/form-data'><input type='file' name='update'><input type='submit' value='Update'></form>";

const float ADC_REF_VOLTAGE = 1;
const int ADC_MAX = 1000;
const float DIVIDER_RATIO = 4.45;

const float BATTERY_VOLTAGE_EMPTY = 3.2;
const float BATTERY_VOLTAGE_FULL = 4.2;

const int SAMPLES = 10;  // Number of samples for moving average
int percent_buffer[SAMPLES];
int buffer_index = 0;
bool buffer_filled = false;
bool wifiChange = false;

bool _status = false;
int charge_status = 0;
int battery_percent = 0;

unsigned long previousMillis = 0;
const long interval = 5000;

String pi_response = "";

bool connectToWiFi(String _ssid, String _pass) {

  WiFi.disconnect(true);

  int n = WiFi.scanNetworks();
  delay(10);
  if (n == 0) {
    Serial.println("No networks found");
    return false;
  }

  for (int i = 0; i < n; ++i) {
    if (_ssid == WiFi.SSID(i)) {
      // Serial.print(WiFi.RSSI(i));
      // Serial.println((WiFi.encryptionType(i) == ENC_TYPE_NONE) ? " " : "*");
      WiFi.begin(_ssid, _pass);
      while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        // Serial.print(".");
      }
      // Serial.println( WiFi.localIP());
      if (!MDNS.begin(host)) {
        // Serial.println("SetUp: Error setting up MDNS responder!");
        while (1) {
          delay(1000);
        }
      }
      // Serial.println("mDNS responder started");
      MDNS.addService("http", "tcp", 80);
      break;
    }
  }
  return true;
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t* payload, size_t length) {

  switch (type) {
    case WStype_DISCONNECTED:
      // Serial.printf("[%u] Disconnected!\n", num);
      _status = false;
      break;
    case WStype_CONNECTED:
      {
        IPAddress ip = webSocket.remoteIP(num);
        _status = true;
        // Serial.printf("[%u] Connected from %d.%d.%d.%d url: %s\n", num, ip[0], ip[1], ip[2], ip[3], payload);
        if (pi_response.length() > 0) {
          sendMessageToServer(_status, pi_response);
        } else {
          
        }
        // send message to client
        //        webSocket.sendTXT(num, WiFi.localIP().toString());
      }
      break;
    case WStype_TEXT:
      // Serial.printf("[%u] get Text: %s\n", num, payload);
      // send message to client
      // webSocket.sendTXT(num, "message here");

      // send data to all connected clients
      // webSocket.broadcastTXT("message here");

      receiveMessageFromServer((char*)payload);
      break;
    case WStype_BIN:
      // Serial.printf("[%u] get binary length: %u\n", num, length);
      // hexdump(payload, length);

      // send message to client
      // webSocket.sendBIN(num, payload, length);
      break;
  }
}

int batteryPercent(float voltage) {
  if (voltage <= BATTERY_VOLTAGE_EMPTY)
    return 0;
  else if (voltage >= BATTERY_VOLTAGE_FULL)
    return 100;
  else {
    float percent = (voltage - BATTERY_VOLTAGE_EMPTY) * 100.0 / (BATTERY_VOLTAGE_FULL - BATTERY_VOLTAGE_EMPTY);
    return (int)(percent + 0.5);  // Round to nearest
  }
}

int readBattery() {
  long adc_sum = 0;
  const int adc_samples = 10;

  for (int i = 0; i < adc_samples; i++) {
    adc_sum += analogRead(A0);
    delay(5);
  }

  int adc_raw = adc_sum / adc_samples;
  float adc_voltage = (adc_raw / (float)ADC_MAX) * ADC_REF_VOLTAGE;
  float battery_voltage = adc_voltage * DIVIDER_RATIO;

  int current_percent = batteryPercent(battery_voltage);

  // Update moving average buffer
  percent_buffer[buffer_index++] = current_percent;
  if (buffer_index >= SAMPLES) {
    buffer_index = 0;
    buffer_filled = true;
  }

  // Calculate average
  int average_percent = 0;
  int count = buffer_filled ? SAMPLES : buffer_index;
  for (int i = 0; i < count; i++) {
    average_percent += percent_buffer[i];
  }
  average_percent /= count;

  // Serial.print("Battery: ");
  // Serial.print(average_percent);
  // Serial.println("%");
  return average_percent;
}

void setup() {
  Serial.begin(115200);

  m.start();
  // m.clear();
  Serial.print("\n");
  Serial.flush();
  WiFi.disconnect(true);

  pinMode(CHARGE_PIN, INPUT_PULLUP);
  pinMode(PI_POWER, OUTPUT);

  // Initialize buffer
  for (int i = 0; i < SAMPLES; i++) {
    percent_buffer[i] = 0;
  }

  WiFi.mode(WIFI_AP_STA);

  WiFi.softAP("ipower", "password");
  delay(10);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  server.on("/", HTTP_GET, []() {
    server.sendHeader("Connection", "close");
    server.send(200, "text/html", serverIndex);
  });
  server.on(
    "/update", HTTP_POST, []() {
      server.sendHeader("Connection", "close");
      server.send(200, "text/plain", (Update.hasError()) ? "FAIL" : "OK");
      ESP.restart();
    },
    []() {
      HTTPUpload& upload = server.upload();
      if (upload.status == UPLOAD_FILE_START) {
        // Serial.setDebugOutput(true);
        WiFiUDP::stopAll();
        // Serial.printf("Update: %s\n", upload.filename.c_str());
        uint32_t maxSketchSpace = (ESP.getFreeSketchSpace() - 0x1000) & 0xFFFFF000;
        if (!Update.begin(maxSketchSpace)) {  // start with max available size
          // Update.printError(Serial);
        }
      } else if (upload.status == UPLOAD_FILE_WRITE) {
        if (Update.write(upload.buf, upload.currentSize) != upload.currentSize) {
          // Update.printError(Serial);
        }
      } else if (upload.status == UPLOAD_FILE_END) {
        if (Update.end(true)) {  // true to set the size to the current progress
          // Serial.printf("Update Success: %u\nRebooting...\n", upload.totalSize);
        } else {
          // Update.printError(Serial);
        }
      }
      yield();
    });

  server.begin();

  if (m.get_ssid() != "ERR") {

    // Serial.println("ssid: " + m.get_ssid());
    // Serial.println("pass: " + m.get_pass());
    connectToWiFi(m.get_ssid(), m.get_pass());
  } else {
    connectToWiFi("JioFiber_4G", "Qualcomm");
  }
}

int prev_battery = 0;

void loop() {
  unsigned long currentMillis = millis();

  server.handleClient();
  webSocket.loop();
  MDNS.update();

  charge_status = digitalRead(CHARGE_PIN) ? 1 : 0;
  battery_percent = readBattery();

  if (battery_percent != prev_battery) {
    prev_battery = battery_percent;
    String datatosend = sendToPi(" ");
    // webSocket.broadcastTXT("sent:" + datatosend);
    Serial.print(datatosend);
  }

  pi_response = Serial.readStringUntil('\n');

  // {"ack": "ok","data":{"cpu": "50%","temp": 46,"ram": "50%","wifi": "active","ssid": "Khaja","uart": "active"} }
  if (pi_response.length() > 0) {
    // Serial.println(pi_response);
    // Serial.println(pi_response.length());

    pi_response.trim();  // Remove any extra spaces or newline characters

    JsonDocument doc;

    DeserializationError error = deserializeJson(doc, pi_response);

    if (error) {
      // Serial.print("deserializeJson() failed: ");
      // Serial.println(error.c_str());
      return;
    }

    String ack = doc["ack"];  // "ok"

    if (ack == "ok") {
      if (doc.containsKey("power") && !doc["power"].isNull()) {
        String power = doc["power"].as<String>();
        if (power == "off") {
          digitalWrite(PI_POWER, HIGH);
          JsonDocument _doc;
          // { "ack": "ok", "power": "off" }
          _doc["ack"] = "ok";
          _doc["power"] = "off";
          _doc["status"] = digitalRead(PI_POWER);

          String output;

          serializeJson(_doc, output);
          sendMessageToServer(_status, output);
        }else{
          sendMessageToServer(_status, pi_response);
        }
      } else {

        sendMessageToServer(_status, pi_response);
      }
    }
  }
  //{"ack": "ok","power":"on","data":{"cpu": "50","temp": 46,"ram": "50","battery":25,"charging":0,"wifi": "active","ssid": "Khaja","uart": "active","bluetooth":"inactive","diskUsed":50,"diskFree":50} }

  /*
{
  "charging":0,
  "battery": 50,
  "command": "shutdown",
  "request": "full"
}
*/

  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    // String datatosend = sendToPi(" ");
    // webSocket.broadcastTXT("sent:" + datatosend);

    if (WiFi.status() == WL_CONNECTED) {
      if (MDNS.isRunning() == false) {
        if (!MDNS.begin(host)) {
          // Serial.println("Loop: Error setting up MDNS responder!");
          while (1) {
            delay(1000);
          }
        }
        // Serial.println("mDNS responder started");
        MDNS.addService("http", "tcp", 80);
      }
    }
    if (WiFi.status() != WL_CONNECTED) {
      connectToWiFi("JioFiber_4G", "Qualcomm");
    }
  }

  if (wifiChange) {
    WiFi.begin(m.get_ssid(), m.get_pass());
    // Serial.println("Connecting to: " + m.get_ssid());
    while (WiFi.status() != WL_CONNECTED) {
      delay(100);
    }
    // Serial.println(WiFi.localIP());
    wifiChange = false;
  }
}

//PI code

String sendToPi(String command) {
  String _data;
  JsonDocument doc;

  doc["charging"] = !charge_status;
  doc["battery"] = battery_percent;
  doc["command"] = command;
  doc["request"] = "full";

  doc.shrinkToFit();  // optional

  serializeJson(doc, _data);
  Serial.println(_data);
  return _data;
}

bool sendMessageToServer(bool server, String _data) {
  if (server) {
    webSocket.broadcastTXT(_data);
    return true;
  }
  return false;
}

void receiveMessageFromServer(const char* input) {
  String _data = input;
  JsonDocument doc;

  DeserializationError error = deserializeJson(doc, input);

  if (error) {
    // Serial.print("deserializeJson() failed: ");
    // Serial.println(error.c_str());
    return;
  }

  if (doc.containsKey("type")) {
    int type = doc["type"];

    if (type == 0) {
      if (doc.containsKey("ssid") && !doc["ssid"].isNull()) {
        String ssid = doc["ssid"].as<String>();
        String pass = doc["pass"];
        if (ssid.length() != 0 || ssid != "null") {
          // Serial.println(ssid);
          // Serial.println(pass);
          m.set_ssid(ssid);
          m.set_pass(pass);
          wifiChange = true;
        }
      } else {
        parseError("missing ssid or password");
      }

    } else if (type == 1) {
      if (doc.containsKey("message") && !doc["message"].isNull()) {
        String message = doc["message"];  // "off"

        if (message == "on") {
          String output;
          digitalWrite(PI_POWER, LOW);
          JsonDocument doc;
          doc["message"] = "ok";
          doc["status"] = digitalRead(PI_POWER);
          serializeJson(doc, output);
          sendMessageToServer(_status, output);
        } else if (message == "off") {
          String command = doc["command"];  // "shutdown"
          sendToPi(command);
        } else {
          parseError("invalid message");
        }
      } else {
        parseError("missing message");
      }

    } else {
      parseError("type invalid");
    }
  } else {
    parseError("type missing");
  }
}

void parseError(const String& message) {
  String output = message;
  JsonDocument _doc;

  _doc["error"] = 1;
  _doc["message"] = message;

  _doc.shrinkToFit();
  serializeJson(_doc, output);
  sendMessageToServer(_status, output);
}