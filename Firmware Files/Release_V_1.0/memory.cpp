#include "memory.h"

Memory::Memory(/* args */) {
}

Memory::~Memory() {
}

void Memory::start() {
  EEPROM.begin(this->_size);
  mem = true;
}

esp_mem_t Memory::clear(void) {
  if (this->mem) {
    for (size_t i = 0; i < (this->_size + 1); i++) {
      EEPROM.write(i, 0x00);
    }
    EEPROM.commit();
    return SUCCESS;
  }
  return ERR;
}

esp_mem_t Memory::clear_devicename(void) {
  if (this->mem) {
    for (size_t i = 0; i < 20; i++) {
      EEPROM.write(i, 0x00);
    }
    EEPROM.commit();
    return SUCCESS;
  }
  return ERR;
}

void Memory::set_devicename(String _t) {
  this->clear_devicename();
  if (this->mem) {
    for (size_t i = 0; i < _t.length(); i++) {
      EEPROM.write(i, _t[i]);  // 0-20
    }
    EEPROM.commit();
  }
}

String Memory::get_devicename(void) {
  if (this->mem) {
    uint8_t b = EEPROM.read(1);

    if (b == 0 || b == 255) {
      return "ERR";
    }
    String _m = "";
    for (size_t i = 0; i < 20; i++)  // 0-20
    {
      b = EEPROM.read(i);
      if (b > 0 && b != 255) {
        _m += (char)b;
      }
    }
    return _m;
  }
  return "ERR";
}

esp_mem_t Memory::clear_ssid(void) {
  if (this->mem) {
    for (size_t i = 0; i < 30; i++) {
      EEPROM.write(i + 21, 0x00);  // 21-51
    }

    EEPROM.commit();
    return SUCCESS;
  }
  return ERR;
}

void Memory::set_ssid(String _t) {
  this->clear_ssid();
  if (this->mem) {
    for (size_t i = 0; i < _t.length(); i++) {
      EEPROM.write(i + 21, _t[i]);
    }
    EEPROM.commit();
  }
}

String Memory::get_ssid(void) {
  if (this->mem) {
    uint8_t b = EEPROM.read(21);

    if (b == 0 || b == 255) {
      return "ERR";
    }

    String _m = "";
    for (size_t i = 0; i < 30; i++) {
      b = EEPROM.read(i + 21);
      if (b > 0 && b != 255) {
        _m += (char)b;
      }
    }
    return _m;
  }
  return "ERR";
}

esp_mem_t Memory::clear_pass(void) {
  if (this->mem) {
    for (size_t i = 0; i < 30; i++) {
      EEPROM.write(i + 52, 0x00);
    }
    EEPROM.commit();
    return SUCCESS;
  }
  return ERR;
}

void Memory::set_pass(String _t) {
  this->clear_pass();
  if (this->mem) {
    for (size_t i = 0; i < _t.length(); i++) {
      EEPROM.write(i + 52, _t[i]);
    }
    EEPROM.commit();
  }
}

String Memory::get_pass(void) {
  if (this->mem) {
    uint8_t b = EEPROM.read(21);

    if (b == 0 || b == 255) {
      return "ERR";
    }
    String _m = "";
    for (size_t i = 0; i < 30; i++) {
      b = EEPROM.read(i + 52);
      if (b > 0 && b != 255) {
        _m += (char)b;
      }
    }
    return _m;
  }
  return "ERR";
}
