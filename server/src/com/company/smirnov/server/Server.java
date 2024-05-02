package com.company.smirnov.server;

import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.*;
import static java.lang.System.Logger.Level.*;

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
    private final int lengthName;

    /**
     * Порт для соединения с сервером.
     */
    private final int port;

    /**
     * Список активных соединений с сервером.
     */
    private final List<ReceivingAndSendingMessage> connectionHandlers = new CopyOnWriteArrayList<>();
    /**
     * Список сообщений для рассылки клиентам.
     */
    private final BlockingQueue<Message> messages = new ArrayBlockingQueue<>(1000, true);

    public Server(int port, long maxSizeFile, int lengthName) {
        if (maxSizeFile <= 0 || lengthName <= 0 || port <= 0) {
            throw new IllegalArgumentException(" maxSizeFile<=0; lengthName<=0");
        }
        this.port = port;
        this.maxSizeFile = maxSizeFile;
        this.lengthName = lengthName;
        /*Arrays.stream(RequestsServer.values()).forEach(requestServer -> serverMap.put(requestServer.getRequestName(), requestServer));*/
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ReceivingAndSendingMessage connectionHandler = new ReceivingAndSendingMessage(socket);
                    connectionHandlers.add(connectionHandler);
                    out.println(connectionHandler.getIdConnection());
                    new ThreadForClient(connectionHandler, connectionHandlers, messages,
                            maxSizeFile, lengthName).start();
                } catch (IOException e) {
                    logger.log(ERROR, "Ошибка запуска сервера");
                    return;
                }
            }
        } catch (IOException e) {
            logger.log(ERROR, "Порт% не доступен".formatted(port));
        }
    }
}