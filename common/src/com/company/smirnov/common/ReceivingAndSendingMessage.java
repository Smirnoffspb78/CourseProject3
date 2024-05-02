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
    private Socket socket;  //Для установки соединения между клиентом и сервером нужен сокет

    private static int idGlobalConnection;
    /**
     * Номер подключения к серверу.
     */
    private final int idConnection;
    private final ObjectInputStream inputStream; //Для получения сообщения
    private final ObjectOutput outputStream; //Для отправки сообщения

    public ReceivingAndSendingMessage(Socket socket) throws IOException {
        requireNonNull(socket);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream()); //инпут создается после аутпута, т.к. иначе инпут заблокирует инпут, т.к. канал будет ждать получения сообщения
        idConnection = ++idGlobalConnection;
    }

    public void send(Message message) throws IOException {
        if (nonNull(message)) {
            message.setTimeOfSending(LocalDateTime.now());
            outputStream.writeObject(message);
            outputStream.flush();
        }

    }

    public Message read() {
        try {
            return (Message) inputStream.readObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public int getIdConnection() {
        return idConnection;
    }

    @Override
    public void close() throws Exception {
        outputStream.close();
        inputStream.close();
        socket.close();
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
