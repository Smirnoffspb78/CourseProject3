package com.company.smirnov.client;

import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.getLogger;
import static java.lang.System.in;
import static java.util.Objects.requireNonNull;


/**
 * Клиент.
 */
public class Client {
    private final System.Logger logger = getLogger(Client.class.getName());
    /**
     * Адрес для соединения с сервером.
     */
    private final InetSocketAddress address;
    private final Scanner scanner = new Scanner(in);

    /**
     * Коннектор для взаимодействия с сервером.
     */
    private ReceivingAndSendingMessage connectionHandler;

    List<String> fileNames=new CopyOnWriteArrayList<>();

    /**
     * Конструктор создает клиента, подключаемого к серверу.
     *
     * @param address Адрес для подключения к серверу
     */
    public Client(InetSocketAddress address) {
        this.address = requireNonNull(address);
    }

    /**
     * Создает новое соединение.
     *
     * @throws IOException Ошибка подключения к серверу
     */
    private void createConnection() throws IOException {
        connectionHandler = new ReceivingAndSendingMessage(new Socket(address.getHostName(), address.getPort()));
    }

    /**
     * Запуск клиентского соединения.
     */
    public void startClient() {
        logger.log(INFO, "Введите имя");
        String username = scanner.nextLine();
        try {
            createConnection();
            new Writer(username, connectionHandler).start();
            Reader reader = new Reader(connectionHandler);
            reader.setDaemon(true);
            reader.start();
        } catch (IOException e) {
            logger.log(WARNING, "Ошибка подключения к серверу.");
        }
    }
}
