# iPower – Battery-Powered Smart Monitoring & Control System

**iPower** is a modular, battery-powered system that integrates a Raspberry Pi, an ESP-8285 microcontroller, and a custom-built Android application. It enables remote monitoring and control of the Raspberry Pi's power, temperature, CPU/RAM usage, and more 
- ideal for remote or headless embedded setups.
- battery monitor with system battery icon on system tray
- no cable overheads
- clean setup
- no custom 3D printed case needed
- builtin charge and step up modules
- can be charged with the Pi usb input, no extra usb headers to charge.
- 3.5inch tft-display support
- runs on raspberry pi Desktop Os and Headless installs
- highly customizable python code and esp-firmware
- runs on 1MB Flash along with OTA

---

## 📦 Project Structure

```
iPower/
├── Android-App/          # Android application (built in Android Studio)
├── Images/               # Project screenshots, circuit diagrams, UI images
├── Firmware Files/         # Firmware for ESP8266/ESP32 microcontroller
├── Raspberry Pi/         # Python scripts for UART-based communication on Pi
├── README.md             # This project overview file
```

---

## 🚀 Features

- 🔋 **Battery monitoring** (voltage, status)
- 🧠 **Raspberry Pi system info** (CPU, RAM, disk, temperature)
- 📲 **Android app** for real-time remote control
- 🔌 **Safe shutdown / reboot** control from the mobile app
- 🔄 **Serial UART communication** between Raspberry Pi and ESP
- ⚡ Fully offline and headless system management

---

## 🛠️ Tech Stack

| Component      | Technology                |
|----------------|---------------------------|
| Microcontroller| ESP8285 / ESP8266         |
| Raspberry Pi 3 | Python 3 + pySerial       |
| Mobile App     | Android (Java)            |
| Communication  | UART Serial Protocol      |

---

## 🧰 How to Use

### 📟 1. Flash the ESP Microcontroller
- Navigate to `Firmware Files/Release_V_1.0` and open the `.ino` file in Arduino IDE or PlatformIO.
- Select the correct board and port.
- Upload the firmware to ESP8266/ESP32.

### 🍓 2. Run the Raspberry Pi Script
- Navigate to `Raspberry Pi Files/`
- Install required Python libraries:
  ```bash
  pip3 install pyserial
  ```
- Run the main UART handler:
  ```bash
  python3 esp_uart.py
  ```

### 📱 3. Build and Run the Android App
- Open `Android-App/` in Android Studio.
- Connect your Android device.
- Build and install the app on your phone.
- Pair with Raspberry Pi via Wi-Fi or Bluetooth (based on your implementation).

---

## 🖼️ Images & Diagrams

Find visuals and screenshots in the [`Images/`](./Images/) folder.

- Android app UI
- Serial monitor samples

---

## 📜 License

This project is licensed under the [MIT License](LICENSE.txt).  
You are free to use, modify, and distribute it with proper attribution.

---

## 🙌 Credits

Created by Shek Khaja Pasha
-
From firmware to frontend – an end-to-end embedded solution.

---

## 📬 Contact

For suggestions, issues, or contributions:  
[GitHub Issues](https://github.com/yourusername/iPower/issues) • your.email@example.com

---

> _Empowering embedded systems with smart, remote control._
