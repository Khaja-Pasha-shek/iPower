package com.example.pipower;

import android.net.nsd.NsdServiceInfo;


public class NsdService {
    NsdServiceInfo nsdServiceInfo;
    private String name;
    private String host;
    private int port;

    public NsdService(NsdServiceInfo nsdServiceInfo) {
        this.nsdServiceInfo = nsdServiceInfo;
    }

    public void AddService(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
