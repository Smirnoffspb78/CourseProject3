package com.company.smirnov.server;

import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.io.File.separator;
import static java.lang.System.*;
import static java.lang.System.Logger.Level.*;
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
    private final CopyOnWriteArrayList<ReceivingAndSendingMessage> connectionHandlers = new CopyOnWriteArrayList<>();
    /**
     * Список сообщений для рассылки клиентам.
     */
    private final BlockingQueue<Message> messages = new ArrayBlockingQueue<>(1000, true);

    private final ConcurrentMap<String, String> nameFileAndDescription = new ConcurrentHashMap<>();

    /**
     * Конструктор создает сервер
     * @param port Номер порта
     * @param maxSizeFile Максимальный размер файла, доступный для загрузки
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
                .formatted(separator, separator, separator, separator, separator)))) {
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
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ReceivingAndSendingMessage connectionHandler = new ReceivingAndSendingMessage(socket);
                    connectionHandlers.add(connectionHandler);
                    new ThreadForClient(connectionHandler, connectionHandlers, messages,
                            maxSizeFile, maxLengthDescription, nameFileAndDescription).start();
                } catch (IOException e) {
                    logger.log(ERROR, "Ошибка запуска сервера");
                    return;
                }
            }
        } catch (IOException e) {
            logger.log(ERROR, "Порт % не доступен".formatted(port));
        }
    }
}