package com.company.smirnov.server;

import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.io.File.separator;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.nio.file.Files.newBufferedReader;

/**
 * Сервер.
 */
public class Server {
    private final System.Logger logger = getLogger(Server.class.getName());

    /**
     * Максимальный размер файла для загрузки, [мБ].
     */
    private final double maxSizeFile;
    /**
     * Максимальная длина имени файла.
     */
    private final int maxLengthDescription;

    /**
     * Порт для соединения с сервером.
     */
    private final int port;

    /**
     * Список активных соединений с сервером.
     */
    private final List<ReceivingAndSendingMessage> connectionHandlers = new CopyOnWriteArrayList<>();

    private final Map<String, String> nameFileAndDescription = new ConcurrentHashMap<>();

    /**
     * Конструктор создает сервер
     *
     * @param port                 Номер порта
     * @param maxSizeFile          Максимальный размер файла, доступный для загрузки
     * @param maxLengthDescription Максимальное количество символов для описания файла
     */
    public Server(int port, long maxSizeFile, int maxLengthDescription) {
        if (maxSizeFile < 0 || maxLengthDescription <= 0 || port <= 0) {
            throw new IllegalArgumentException(" maxSizeFile<=0; lengthName<=0");
        }
        this.port = port;
        this.maxSizeFile = maxSizeFile;
        this.maxLengthDescription = maxLengthDescription;
        try (BufferedReader readFilesAndDescription = newBufferedReader(Path.of(".%sserver%ssrc%sfiles%sdescription%sdescription.csv"
                .replace("%s", separator)))) {
            readFilesAndDescription
                    .lines()
                    .map(nameAndDescription -> nameAndDescription.split(";"))
                    .forEach(nameAndDescription -> nameFileAndDescription.put(nameAndDescription[0], nameAndDescription[1]));
        } catch (IOException e) {
            logger.log(ERROR, "Не удалось загрузить список файлов или файлы на сервере отсутствуют");
        }
    }

    /**
     * Запускает сервер.
     */
    public void startServer() {
        boolean executeServer = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (executeServer) {
                try {
                    Socket socket = serverSocket.accept();
                    ReceivingAndSendingMessage connectionHandler = new ReceivingAndSendingMessage(socket);
                    connectionHandlers.add(connectionHandler);
                    new ThreadForClient(connectionHandler, connectionHandlers,
                            maxSizeFile, maxLengthDescription, nameFileAndDescription).start();
                } catch (IOException e) {
                    logger.log(ERROR, "Ошибка приема/передачи данных через канал.");
                    executeServer = false;
                    connectionHandlers.forEach(connectionHandler -> {
                        try {
                            connectionHandler.close();
                        } catch (Exception ex) {
                            logger.log(ERROR, "Ошибка ввода/вывода данных");
                        }
                    });
                    serverSocket.close();
                }
            }
        } catch (IOException e) {
            logger.log(ERROR, "Порт %s не доступен".formatted(port));
        }
    }
}