package com.company.smirnov.server;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.io.File.separator;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.getLogger;
import static java.lang.System.out;
import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.Files.writeString;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;

/**
 * Класс создает новый поток для работы с клиентом.
 */
public class ThreadForClient extends Thread {
    private final System.Logger logger = getLogger(ThreadForClient.class.getName());
    /**
     * Соединение клиента с сервером.
     */
    private final ReceivingAndSendingMessage connectionHandler;
    /**
     * Активные соединения.
     */
    private final List<ReceivingAndSendingMessage> connectionHandlers;
    /**
     * Пул сообщений.
     */
    private final BlockingQueue<Message> messages;

    /**
     * Максимальный размер файла для загрузки, [мБ].
     */
    private final double maxSizeFile;
    /**
     * Максимальная длина описания файла.
     */
    private final int maxLengthDescription;

    /**
     * Хранит список наименований файлов и их описание.
     */
    private final ConcurrentMap<String, String> nameFileAndDescription;

    /**
     * Имя сервера.
     */
    private static final String NAME_SERVER = "Server";

    public ThreadForClient(ReceivingAndSendingMessage connectionHandler, CopyOnWriteArrayList<ReceivingAndSendingMessage> connectionHandlers,
                           BlockingQueue<Message> messages, double maxSizeFile, int maxLengthDescription, ConcurrentMap<String, String> nameFileAndDescription) {
        this.connectionHandler = requireNonNull(connectionHandler);
        this.connectionHandlers = requireNonNull(connectionHandlers);
        this.messages = requireNonNull(messages);
        this.maxSizeFile = maxSizeFile;
        this.maxLengthDescription = maxLengthDescription;
        this.nameFileAndDescription = requireNonNull(nameFileAndDescription);
    }

    /**
     * Получает информацию о всех доступных файлах, загруженных на сервер.
     *
     * @return Список всех файлов сервера
     */
    private String computeFiles() {
        StringBuilder builderFilesDirectory = new StringBuilder();
        if (!nameFileAndDescription.isEmpty()) {
            builderFilesDirectory.append("Список доступных файлов на сервере:\n");
            nameFileAndDescription.forEach((key, value) -> builderFilesDirectory.append(key).append("; ").append(value).append("\n"));
        } else {
            builderFilesDirectory.append("Файлы на сервере отсутствуют.");
        }
        out.println(builderFilesDirectory);
        return builderFilesDirectory.toString();
    }

    /**
     * Вспомогательный метод загружает файл на сервер.
     *
     * @param fileTxtMessage Сообщение с текстовым файлом
     * @return Сообщение о добавлении файла для активных соединений
     */
    private Message downloadFile(FileTxtMessage fileTxtMessage) {
        Message message = new Message(NAME_SERVER);
        try {
            String fileName = checkFile(fileTxtMessage.getFileName());
            writeString(get(".%sserver%ssrc%sfiles%s".formatted(separator, separator, separator, separator) + fileName),
                    fileTxtMessage.getTextFile(),
                    CREATE,
                    APPEND,
                    SYNC);
            writeString(get(".%sserver%ssrc%sfiles%sdescription%sdescription.csv"
                            .formatted(separator, separator, separator, separator, separator)),
                    fileName + ";" + fileTxtMessage.getDescriptionFile() + "\n",
                    CREATE,
                    APPEND,
                    WRITE,
                    SYNC);
            message.setText("На сервер добавлен файл %s; %s.".formatted(fileName, fileTxtMessage.getDescriptionFile()));
            nameFileAndDescription.put(fileName, fileTxtMessage.getDescriptionFile());
            return message;
        } catch (IOException e) {
            logger.log(ERROR, "Ошибка добавления файла.");
            message.setText("Ошибка добавления файла.");
            return message;
        }
    }

    /**
     * Вспомогательный метод отправляет файл клиенту
     * @param fromClientFile файл для клиента
     * @return сообщение для клиента
     */
    private Message sendFile(Message fromClientFile) {
        Message message;
        if (nameFileAndDescription.containsKey(fromClientFile.getText())) {
            message = new FileTxtMessage(NAME_SERVER, fromClientFile.getText(), nameFileAndDescription.get(fromClientFile.getText()));
            StringBuilder stringBuilderFile = new StringBuilder();
            try (BufferedReader buffer = newBufferedReader(Path.of(".%sserver%ssrc%sfiles%s".formatted(separator, separator, separator, separator) + fromClientFile.getText()))) {
                buffer
                        .lines()
                        .forEach(string -> stringBuilderFile.append(string).append("\n"));
                message.setText(stringBuilderFile.toString());
            } catch (IOException e) {
                logger.log(ERROR, "Файл отсутствует или не доступен");
            }
        } else {
            message = new Message(NAME_SERVER);
            message.setText("Файл на сервере отсутствует");
        }
        return message;
    }

    /**
     * Вспомогательный метод, который проверяет наличие этого файла в директории
     */
    private String checkFile(String nameFile) {
        Set<String> allFilesSet = new CopyOnWriteArraySet<>();
        nameFileAndDescription.forEach((key, value) -> allFilesSet.add(key));
        if (allFilesSet.contains(nameFile)) {
            int numberVersion = 0;
            String[] fileNameDivision = nameFile.split("\\.");
            while (allFilesSet.contains(nameFile)) {
                StringBuilder newFileName = new StringBuilder();
                Arrays.stream(fileNameDivision)
                        .limit((long) fileNameDivision.length - 1)
                        .forEach(newFileName::append);
                newFileName.append("(").append(++numberVersion).append(").").append(fileNameDivision[fileNameDivision.length - 1]);
                nameFile = newFileName.toString();
            }
        }
        return nameFile;
    }

    /**
     * Вспомогательный метод добавляет сообщения в пул и создает объект для рассылки сообщения в новом потоке
     *
     * @param message    Сообщение
     * @param allSending Флаг рассылки сообщений
     *                   true - рассылается сообщение всем, кроме отправителя,
     *                   false - отправляется сообщение только отправителю
     */
    private void sendMessage(Message message, boolean allSending) {
        try {
            messages.put(message); //метод будет заблокирован блокирующей очередью, если список переполнен
        } catch (InterruptedException e) {
            logger.log(WARNING, "Пул сообщений переполнен.");
        }
        new SendingMessagesOfClients(connectionHandler, message, allSending, connectionHandlers, messages).start();
    }

    @Override
    public void run() {
        String downloadFileCommand = "/download";
        String getNameFilesCommand = "/files";
        boolean allSending;
        while (true) {
            allSending = true;
            Message fromClient;
            try {
                fromClient = connectionHandler.read();
            } catch (Exception e) {
                logger.log(INFO, "Пользователь отключился от сервера");
                connectionHandlers.remove(connectionHandler);
                return;
            }
            out.printf("%s: %s%n%s%n", fromClient.getSender(), fromClient.getTimeOfSending(), fromClient.getText());
            Message message;
            if (Objects.equals(fromClient.getText(), downloadFileCommand)) {
                message = new Message(NAME_SERVER);
                if (((FileTxtMessage) fromClient).getSizeFile() <= maxSizeFile * 1_000_000
                        && ((FileTxtMessage) fromClient).getDescriptionFile().length() < maxLengthDescription
                        && !((FileTxtMessage) fromClient).getDescriptionFile().isBlank()) {
                    message = downloadFile(((FileTxtMessage) fromClient));
                } else {
                    message.setText("Файл превышает %sМб или слишком длинный или отсутствует описание файла.".formatted(maxSizeFile));
                    allSending = false;
                }
            } else if (Objects.equals(getNameFilesCommand, fromClient.getText())) {
                allSending = false;
                Message messageFiles = new Message(NAME_SERVER);
                messageFiles.setText("%sВведите имя файла для загрузки".formatted(computeFiles()));
                sendMessage(messageFiles, allSending);
                Message fromClientFile;
                try {
                    fromClientFile = connectionHandler.read();
                    message = sendFile(fromClientFile);
                } catch (Exception e) {
                    logger.log(INFO, "Пользователь отключился от сервера");
                    connectionHandlers.remove(connectionHandler);
                    return;
                }

            } else {
                message = new Message(fromClient.getSender());
                message.setText(/*"%s: %s%n%s".formatted(fromClient.getSender(), fromClient.getTimeOfSending(), */fromClient.getText());
            }
            sendMessage(message, allSending);
        }
    }
}
