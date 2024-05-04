package com.company.smirnov.client;

import java.net.InetSocketAddress;

public class AppClient {
    public static void main(String[] args) {
        new Client(new InetSocketAddress("127.0.0.1", 2220)).startClient();
    }
}
