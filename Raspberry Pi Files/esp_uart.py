import gi
gi.require_version('AyatanaAppIndicator3', '0.1')
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk, AyatanaAppIndicator3 as AppIndicator3, GLib

import serial
import json
import time
import psutil
import subprocess
import os
import threading

ICON_PATH = "/home/khaja/ipower/battery/"
SERIAL_PORT = "/dev/ttyS0"
BAUD_RATE = 115200

# --- Tray Icon Manager ---
class TrayIcon:
    def __init__(self):
        self.indicator = AppIndicator3.Indicator.new(
            "battery-indicator",
            os.path.join(ICON_PATH, "low-battery.png"),
            AppIndicator3.IndicatorCategory.HARDWARE
        )
        self.indicator.set_status(AppIndicator3.IndicatorStatus.ACTIVE)

        # Add Quit menu
        menu = Gtk.Menu()
        item_quit = Gtk.MenuItem(label="Quit")
        item_quit.connect("activate", Gtk.main_quit)
        menu.append(item_quit)
        menu.show_all()
        self.indicator.set_menu(menu)

    def set_icon(self, icon_name):
        icon_path = os.path.join(ICON_PATH, icon_name)
        if os.path.exists(icon_path):
            self.indicator.set_icon_full(icon_path, "Battery Icon")
            print(f"[ICON] Updated to: {icon_path}")
        else:
            print(f"[WARN] Icon not found: {icon_path}")

# --- System Info Provider ---
def get_system_info():
    cpu = int(psutil.cpu_percent())
    ram = int(psutil.virtual_memory().percent)
    storage = int(psutil.disk_usage('/').percent)
    temp = get_temp()
    wifi, ssid = get_wifi_info()
    bluetooth = "inactive"
    uart = "active"

    return {
        "cpu": cpu,
        "temp": temp,
        "ram": ram,
        "storage": storage,
        "wifi": wifi,
        "bluetooth": bluetooth,
        "ssid": ssid,
        "uart": uart
    }

def get_temp():
    try:
        with open("/sys/class/thermal/thermal_zone0/temp") as f:
            return str(round(int(f.read()) / 1000))
    except:
        return "N/A"

def get_wifi_info():
    try:
        ssid = subprocess.check_output(["iwgetid", "-r"], stderr=subprocess.DEVNULL).decode().strip()
        return ("active", ssid) if ssid else ("inactive", "N/A")
    except:
        return "inactive", "N/A"

def run_command(command):
    if command == "shutdown":
        os.system("sudo shutdown now")
    elif command == "reboot":
        os.system("sudo reboot")

# --- UART Listener ---
class UARTHandler:
    def __init__(self, port, baud):
        self.ser = serial.Serial(port, baud, timeout=1)
        self.tray = TrayIcon()

    def listen(self):
        print("[INFO] Listening on UART...")
        while True:
            try:
                line = self.ser.readline().decode().strip()
                if not line:
                    continue

                print(f"[UART IN] {line}")
                data = json.loads(line)

                print(f"Battery: {data.get('battery')}%, Charging: {data.get('charging')}, Command: {data.get('command')}, Request: {data.get('request')}")

                self.update_tray_icon(data)
                self.handle_command(data)
                self.handle_request(data)

            except json.JSONDecodeError:
                print("[ERROR] Invalid JSON")
            except Exception as e:
                print(f"[ERROR] {e}")

    def update_tray_icon(self, data):
        battery = int(data.get("battery", 0))
        charging = int(data.get("charging", 0))

        if charging:
            icon = "low-battery-charging.png"
        else:
            if battery >= 90:
                icon = "battery-full.png"
            elif battery >= 75:
                icon = "battery-75.png"
            elif battery >= 50:
                icon = "battery-50.png"
            elif battery >= 25:
                icon = "battery-25.png"
            else:
                icon = "low-battery.png"

        GLib.idle_add(self.tray.set_icon, icon)

    def handle_command(self, data):
        command = data.get("command")
        if command in ["shutdown", "reboot"]:
            ack = {"ack": "ok"}
            self.send_json(ack)
            print(f"[INFO] Executing command: {command}")
            time.sleep(1)
            run_command(command)

    def handle_request(self, data):
        if data.get("request") == "full":
            system_info = get_system_info()
            system_info["battery"] = int(data.get("battery", 0))
            system_info["charging"] = int(data.get("charging", 0))

            response = {
                "ack": "ok",
                "power":"off"
                
            }
            self.send_json(response)

    def send_json(self, obj):
        try:
            msg = json.dumps(obj) + "\n"
            self.ser.write(msg.encode())
            print(f"[UART OUT] {msg.strip()}")
        except Exception as e:
            print(f"[ERROR] Sending JSON: {e}")

# --- Main ---
def main():
    uart = UARTHandler(SERIAL_PORT, BAUD_RATE)
    thread = threading.Thread(target=uart.listen, daemon=True)
    thread.start()
    Gtk.main()

if __name__ == "__main__":
    main()
