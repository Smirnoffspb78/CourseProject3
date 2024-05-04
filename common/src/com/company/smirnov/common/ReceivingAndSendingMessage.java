package com.company.smirnov.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Устанавливает соединение между сервером и клиентом
 */
public class ReceivingAndSendingMessage implements AutoCloseable {

    /**
     * Счетчик подключений к серверу.
     */
    private static int idGlobalConnection;
    /**
     * Номер подключения к серверу.
     */
    private final int idConnection;
    /**
     * Канал для получения информации.
     */
    private final ObjectInputStream inputStream;
    /**
     * Канал для отправки информации.
     */
    private final ObjectOutput outputStream;

    /**
     * Конструктор создает канал для отправки и получения сообщений.
     *
     * @param socket Соединение
     * @throws IOException если возникает ошибка передачи данных
     */
    public ReceivingAndSendingMessage(Socket socket) throws IOException {
        requireNonNull(socket);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        idConnection = ++idGlobalConnection;
    }

    /**
     * Отправляет сообщение.
     *
     * @param message Сообщение
     * @throws IOException Если произошла ошибка при отправке сообщения
     */
    public void send(Message message) throws IOException {
        if (nonNull(message)) {
            message.setTimeOfSending(LocalDateTime.now());
            outputStream.writeObject(message);
            outputStream.flush();
        }

    }

    /**
     * Принимает сообщение.
     *
     * @return Сообщение
     * @throws IOException            Если произошла ошибка при получении сообщения
     * @throws ClassNotFoundException Отсутствует класс принимаемого объекта
     */
    public Message read() throws IOException, ClassNotFoundException {
        return (Message) inputStream.readObject();
    }

    @Override
    public void close() throws Exception {
        outputStream.close();
        inputStream.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceivingAndSendingMessage that)) {
            return false;
        }
        return idConnection == that.idConnection;
    }

    @Override
    public int hashCode() {
        return idConnection;
    }
}
