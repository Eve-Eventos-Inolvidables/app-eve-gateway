package com.example.appeveapigateway;

public record GatewayError(boolean success, String message) {

    public static GatewayError of(String message) {
        return new GatewayError(false, message);
    }
}
