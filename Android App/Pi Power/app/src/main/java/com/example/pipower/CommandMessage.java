package com.example.pipower;

public class CommandMessage {
    private int type;
    private String message;

    public CommandMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }
}
