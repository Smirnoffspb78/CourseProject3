package com.company.smirnov.server;

import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Класс занимается рассылкой сообщений клиентам в отдельном потоке.
 */
public class SendingMessagesOfClients {


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
     * Конструктор создает поток объект для взаимодействия сервера с клиентом
     *
     * @param connectionHandler  Соединение с сервером
     * @param message            Сообщение
     * @param allSending         Флаг отправки сообщения всем подключенным клиентам
     * @param connectionHandlers Все активные соединения
     */
    public SendingMessagesOfClients(ReceivingAndSendingMessage connectionHandler, Message message, boolean allSending
            , List<ReceivingAndSendingMessage> connectionHandlers) {
        this.connectionHandler = requireNonNull(connectionHandler, "connectionHandler=null");
        this.message = requireNonNull(message, "message=null");
        this.allSending = allSending;
        this.connectionHandlers = requireNonNull(connectionHandlers);
    }

    /**
     * Рассылает сообщения пользователям
     */

    public void sendingMessage() {
        if (allSending) {
            connectionHandlers.stream()
                    .filter(connectionHandlerAll -> nonNull(connectionHandlerAll) && !Objects.equals(this.connectionHandler, connectionHandlerAll))
                    .forEach(connectionHandlerAll -> {
                        try {
                            connectionHandlerAll.send(message);
                        } catch (IOException e) {
                            connectionHandlers.remove(connectionHandlerAll);
                        }

                    });
        } else {
            try {
                connectionHandler.send(message);
            } catch (IOException e) {
                connectionHandlers.remove(connectionHandler);
            }
        }
    }
}
