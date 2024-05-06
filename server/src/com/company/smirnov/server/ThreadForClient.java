package com.company.smirnov.server;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.io.File.separator;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.getLogger;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.nonNull;
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
    private final Map<String, String> nameFileAndDescription;

    /**
     * Имя сервера.
     */
    private static final String NAME_SERVER = "Server";

    public ThreadForClient(ReceivingAndSendingMessage connectionHandler, List<ReceivingAndSendingMessage> connectionHandlers,
                           double maxSizeFile, int maxLengthDescription, Map<String, String> nameFileAndDescription) {
        this.connectionHandler = requireNonNull(connectionHandler);
        this.connectionHandlers = requireNonNull(connectionHandlers);
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
        logger.log(INFO, builderFilesDirectory);
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
        String fileName = checkFile(fileTxtMessage.getFileName());
        try {
            write(get(".%sserver%ssrc%sfiles%s".replace("%s", separator) + fileName), fileTxtMessage.getTxtFile());
            logger.log(INFO, "На сервер добавлен файл %s; %s.".formatted(fileName, fileTxtMessage.getDescriptionFile()));
            try {
                writeString(get(".%sserver%ssrc%sfiles%sdescription%sdescription.csv"
                                .replace("%s", separator)),
                        fileName + ";" + fileTxtMessage.getDescriptionFile() + "\n",
                        CREATE,
                        APPEND,
                        WRITE,
                        SYNC);
                message.setText("На сервер добавлен файл %s; %s.".formatted(fileName, fileTxtMessage.getDescriptionFile()));
                nameFileAndDescription.put(fileName, fileTxtMessage.getDescriptionFile());
            } catch (IOException e) {
                logger.log(ERROR, "Ошибка записи в реестр.");
                message.setText("Ошибка записи в реестр.");
            }
            return message;
        } catch (IOException e) {
            logger.log(ERROR, "Ошибка добавления файла.");
            message.setText("Ошибка добавления файла.");
            return message;
        }
    }

    /**
     * Вспомогательный метод отправляет файл клиенту
     *
     * @param fromClientFile файл для клиента
     * @return сообщение для клиента
     */
    private Message sendFile(Message fromClientFile) {
        if (nameFileAndDescription.containsKey(fromClientFile.getText())) {
            FileTxtMessage message = new FileTxtMessage(NAME_SERVER, fromClientFile.getText(), nameFileAndDescription.get(fromClientFile.getText()));
            try {
                byte[] bytesFile = readAllBytes(Path.of(".%sserver%ssrc%sfiles%s".replace("%s", separator) + fromClientFile.getText()));
                message.setTxtFile(bytesFile);
                return message;
            } catch (IOException e) {
                logger.log(ERROR, "Файл отсутствует или не доступен");
                message.setText("Файл отсутствует или не доступен");
                return message;
            }
        } else {
            Message message = new Message(NAME_SERVER);
            message.setText("Файл на сервере отсутствует");
            return message;
        }
    }

    /**
     * Вспомогательный метод, который проверяет наличие этого файла в директории
     */
    private String checkFile(String nameFile) {
        if (nameFileAndDescription.containsKey(nameFile)) {
            int numberVersion = 0;
            String[] fileNameDivision = nameFile.split("\\.");
            while (nameFileAndDescription.containsKey(nameFile)) {
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

    @Override
    public void run() {
        boolean allSending;
        boolean checkConnectClient = true;
        while (checkConnectClient) {
            allSending = true;
            Message fromClient;
            try {
                fromClient = connectionHandler.read();
            } catch (Exception e) {
                logger.log(INFO, "Пользователь отключился от сервера");
                connectionHandlers.remove(connectionHandler);
                checkConnectClient = false;
                fromClient = null;
            }
            if (nonNull(fromClient)) {
                logger.log(INFO, "%s: %s%n%s%n".formatted(fromClient.getSender(), fromClient.getTimeOfSending(), fromClient.getText()));
                Message message = switch (fromClient.getText()) {
                    case "/download" -> {
                        message = new Message(NAME_SERVER);
                        if (fromClient instanceof FileTxtMessage fromClientTxt
                                && fromClientTxt.checkSize(maxSizeFile * 1_000_000, fromClientTxt.getDescriptionFile(), maxLengthDescription)) {
                            message = downloadFile(fromClientTxt);
                        } else {
                            message.setText("Файл превышает %sМб или слишком длинный или отсутствует описание файла.".formatted(maxSizeFile));
                            allSending = false;
                        }
                        yield (message);
                    }
                    case "/files" -> {
                        allSending = false;
                        Message messageFiles = new Message(NAME_SERVER);
                        if (nameFileAndDescription.isEmpty()) {
                            message = new Message(NAME_SERVER);
                            message.setText(computeFiles());
                        } else {
                            messageFiles.setText("%sВведите имя файла для загрузки".formatted(computeFiles()));
                            new SendingMessagesOfClients(connectionHandler, messageFiles, allSending, connectionHandlers).sendingMessage();
                            Message fromClientFile;
                            try {
                                fromClientFile = connectionHandler.read();
                                message = sendFile(fromClientFile);
                            } catch (Exception e) {
                                logger.log(INFO, "Пользователь отключился от сервера");
                                connectionHandlers.remove(connectionHandler);
                                checkConnectClient = false;
                                message = null;
                            }
                        }
                        yield (message);
                    }
                    default -> {
                        message = new Message(fromClient.getSender());
                        message.setText(fromClient.getText());
                        yield (message);
                    }
                };
                if (nonNull(message)) {
                    new SendingMessagesOfClients(connectionHandler, message, allSending, connectionHandlers).sendingMessage();
                }
            }
        }
    }
}
