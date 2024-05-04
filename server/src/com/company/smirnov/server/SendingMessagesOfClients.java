package com.company.smirnov.server;

import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Класс занимается рассылкой сообщений клиентам в отдельном потоке.
 */
public class SendingMessagesOfClients extends Thread {


    /**
     * Соединение с сервером.
     */
    private final ReceivingAndSendingMessage connectionHandler;
    /**
     * Сообщение.
     */
    private final Message message;
    /**
     * Флаг отправки сообщения всем соединениям
     */
    private final boolean allSending;
    /**
     * Список активных соединений.
     */
    private final List<ReceivingAndSendingMessage> connectionHandlers;
    /**
     * Список сообщений для рассылки.
     */
    private final BlockingQueue<Message> messages;

    /**
     * Конструктор создает поток объект для взаимодействия сервера с клиентом
     *
     * @param connectionHandler  Соединение с сервером
     * @param message            Сообщение
     * @param allSending         Флаг отправки сообщения всем подключенным клиентам
     * @param connectionHandlers Все активные соединения
     * @param messages           Список сообщений для отправки клиентам
     */
    public SendingMessagesOfClients(ReceivingAndSendingMessage connectionHandler, Message message, boolean allSending
            , List<ReceivingAndSendingMessage> connectionHandlers, BlockingQueue<Message> messages) {
        this.connectionHandler = requireNonNull(connectionHandler, "connectionHandler=null");
        this.message = requireNonNull(message, "message=null");
        this.allSending = allSending;
        this.connectionHandlers = requireNonNull(connectionHandlers);
        this.messages = requireNonNull(messages);
    }

    /**
     * Рассылает сообщения пользователям
     */
    @Override
    public void run() {
        if (allSending) {
            messages.stream().filter(Objects::nonNull)
                    .forEach(messageAll -> {
                        connectionHandlers.stream()
                                .filter(connectionHandlerAll -> nonNull(connectionHandlerAll) && !Objects.equals(this.connectionHandler, connectionHandlerAll))
                                .forEach(connectionHandlerAll -> {
                                    try {
                                        connectionHandlerAll.send(messageAll);
                                    } catch (IOException e) {
                                        connectionHandlers.remove(connectionHandlerAll);
                                    }
                                });
                        messages.remove(messageAll);
                    });
        } else {
            try {
                connectionHandler.send(message);
            } catch (IOException e) {
                connectionHandlers.remove(connectionHandler);
            }
            messages.remove(message);
        }
    }
}
