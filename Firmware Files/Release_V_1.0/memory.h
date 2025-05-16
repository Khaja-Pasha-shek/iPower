#ifndef MEMORY_H
#define MEMORY_H

#include "Arduino.h"
#include <EEPROM.h>
#include "mem.h"

typedef uint8_t u8;

class Memory {
private:
  bool mem = true;
  int _size = 164;

public:
  void start();

  esp_mem_t clear();
  esp_mem_t clear_ssid(void);
  esp_mem_t clear_pass(void);
  esp_mem_t clear_devicename(void);

  void set_ssid(String _t);
  void set_pass(String _t);
  void set_devicename(String _t);

  String get_devicename(void);
  String get_ssid(void);
  String get_pass(void);

  Memory();
  ~Memory();
};

#endif
