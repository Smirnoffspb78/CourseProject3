package com.company.smirnov.server;

public class AppServer {
    public static void main(String[] args) {
        new Server(2220, 20, 250).startServer();
    }
}
