package com.example.pipower;

//{"ack": "ok", "data": {"cpu": "4.3", "temp": "54", "ram": "54.3", "wifi": "active", "bluetooth": "inactive", "ssid": "JioFiber_4G", "uart": "active"}}

public class JsonResponse {
    private String ack;
    private Data data;
    private String power; // optional fallback field

    public String getAck() {
        return ack;
    }

    public Data getData() {
        return data;
    }

    public String getPower() {
        return power;
    }

    public static class Data {
        private int cpu;
        private int temp;
        private int ram;
        private int battery;
        private int charging;
        private String wifi;
        private String ssid;
        private String uart;
        private String bluetooth;
        private int diskUsed;
        private int diskFree;

        public int getCpu() { return cpu; }
        public int getTemp() { return temp; }
        public int getRam() { return ram; }
        public int getBattery() { return battery; }
        public int getCharging() { return charging; }
        public String getWifi() { return wifi; }
        public String getSsid() { return ssid; }
        public String getUart() { return uart; }
        public String getBluetooth() { return bluetooth; }
        public int getDiskUsed() { return diskUsed; }
        public int getDiskFree() { return diskFree; }
    }
}
