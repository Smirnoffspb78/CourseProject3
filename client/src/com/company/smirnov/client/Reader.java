package com.company.smirnov.client;

import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import static java.lang.System.out;

/**
 * Читает сообщения от сервера.
 */
class Reader extends Thread {

    /**
     * Устанавливает соединение между клиентом и сервером.
     */
    private final ReceivingAndSendingMessage connectionHandler;

    /**
     * Конструктор создает объект для чтения сообщений.
     *
     * @param connectionHandler Устанавливает соединение между клиентом и сервером
     */
    public Reader(ReceivingAndSendingMessage connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void run() {
        while (true) {
            Message message = connectionHandler.read();
            out.println(message.getText());
        }
    }
}