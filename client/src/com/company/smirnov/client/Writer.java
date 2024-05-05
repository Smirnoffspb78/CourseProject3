package com.company.smirnov.client;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.logging.Logger;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.getLogger;
import static java.lang.System.in;
import static java.nio.file.Files.newBufferedReader;
import static java.lang.System.Logger.Level.INFO;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Отправляет сообщения на сервер
 */
public class Writer extends Thread {
    private final System.Logger logger = getLogger(Writer.class.getName());
    private final Scanner scanner = new Scanner(in);

    /**
     * Имя пользователя
     */
    private final String username;
    /**
     * Устанавливает соединение между сервером и клиентом.
     */
    private final ReceivingAndSendingMessage connectionHandler;

    /**
     * Конструктор создает объект для отправки сообщений на сервер.
     *
     * @param username          Имя пользователя
     * @param connectionHandler Соединение между сервером и клиентом
     */
    public Writer(String username, ReceivingAndSendingMessage connectionHandler) {
        if (isNull(username)) {
            throw new NullPointerException("username is null");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("username is blank");
        }
        this.username = username;
        this.connectionHandler = requireNonNull(connectionHandler);
    }

    /**
     * Вспомогательный метод формирует сообщение для отправки txt файла
     *
     * @param text текст сообщения
     * @return сообщение для отправки на сервер
     */
    private Message generateTxtMessage(String text) {
        logger.log(INFO, "Введите имя файла c расширением txt или полный путь к нему.");
        String pathFile = scanner.nextLine();
        StringBuilder stringBuilder = new StringBuilder();
        FileTxtMessage message = null;
        try (BufferedReader buffer = newBufferedReader(Path.of(pathFile))) {
            buffer
                    .lines()
                    .forEach(string -> stringBuilder.append(string).append("\n"));
            logger.log(INFO, "Введите описание файла");
            String descriptionFile = scanner.nextLine();
            message = new FileTxtMessage(username, pathFile, descriptionFile);
            message.setText(text);
            message.setTextFile(stringBuilder.toString());
        } catch (IOException e) {
            logger.log(INFO, "Файл не найден.");
        }
        return message;
    }

    /**
     * Вспомогательный метод отправляет сообщение на сервер
     *
     * @param message Сообщение
     * @return true - Если соединение с сервером активно, false - если соединение с сервером нарушено
     */
    private boolean sendMessage(Message message) {
        try {
            if (nonNull(message)) {
                connectionHandler.send(message);
            }
            return true;
        } catch (IOException e) {
            logger.log(ERROR, "Соединение с сервером отсутствует");
            try {
                connectionHandler.close();
            } catch (Exception ex) {
                logger.log(ERROR, "Соединение отсутствует");
            }
            return false;
        }
    }

    @Override
    public void run() {
        String exitCommand = "/exit";
        String downloadFileCommand = "/download";
        String text;
        Message message;
        boolean continueCommand = true;
        logger.log(INFO, "Введите текст сообщения");
        while (continueCommand) {
            text = scanner.nextLine();
            if (text.equals(exitCommand)) {
                message = null;
            } else if (text.equals(downloadFileCommand)) {
                message = generateTxtMessage(text);
            } else {
                message = new Message(username);
                message.setText(text);
            }
            continueCommand = sendMessage(message);
        }
    }
}