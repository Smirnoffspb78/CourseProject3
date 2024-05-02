package com.company.smirnov.client;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

import static java.lang.System.*;
import static java.nio.file.Files.newBufferedReader;
import static java.lang.System.Logger.Level.INFO;

public class Writer extends Thread {
    private final System.Logger logger = getLogger(Writer.class.getName());
    private final Scanner scanner = new Scanner(in);

    /**
     * Имя пользователя
     */
    private final String username;
    private final ReceivingAndSendingMessage connectionHandler;

    public Writer(String username, ReceivingAndSendingMessage connectionHandler) {
        this.username = username;
        this.connectionHandler = connectionHandler;
    }

    @Override
        public void run() {
            String exitCommand = "/exit";
            String downloadFileCommand = "/download";
            String text = null;
            while (!Objects.equals(text, exitCommand)) {
                out.println("Введите текст сообщения");
                text = scanner.nextLine();
                if (!Objects.equals(text, exitCommand)) {
                    if (Objects.equals(text, downloadFileCommand)) {
                        out.println("Введите имя файла или полный путь к нему");
                        String pathFile = scanner.nextLine();
                        StringBuilder stringBuilder = new StringBuilder();
                        try(BufferedReader buffer= newBufferedReader(Path.of(pathFile))) {
                                    buffer
                                    .lines()
                                    .forEach(string -> stringBuilder.append(string).append("\n"));
                            FileTxtMessage fileTxtMessage = new FileTxtMessage(username, pathFile);
                            fileTxtMessage.setText(text);
                            fileTxtMessage.setTextFile(stringBuilder.toString());
                            connectionHandler.send(fileTxtMessage);
                        } catch (IOException e) {
                            logger.log(INFO, "Файл не найден.");
                        }
                    } else {
                        Message message = new Message(username);
                        message.setText(text);
                        try {
                            connectionHandler.send(message);
                        } catch (IOException e) {
                            logger.log(INFO, "Соединение с сервером отсутствует");
                            text = "/exit";
                        }
                    }
                }
            }
        }
    }