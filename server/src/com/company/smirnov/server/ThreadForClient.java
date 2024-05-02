package com.company.smirnov.server;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.getLogger;
import static java.lang.System.out;
import static java.nio.file.Files.writeString;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

/**
 * Класс создает новый поток для работы с клиентом.
 */
public class ThreadForClient extends Thread {
    private final System.Logger logger = getLogger(ThreadForClient.class.getName());
    private final ReceivingAndSendingMessage connectionHandler;

    private List<ReceivingAndSendingMessage> connectionHandlers;
    private BlockingQueue<Message> messages;

    /**
     * Максимальный размер файла для загрузки, [мБ].
     */
    private final double maxSizeFile;
    /**
     * Максимальная длина имени файла.
     */
    private final int lengthName;

    public ThreadForClient(ReceivingAndSendingMessage connectionHandler, List<ReceivingAndSendingMessage> connectionHandlers,
                           BlockingQueue<Message> messages, double maxSizeFile, int lengthName) {
        this.connectionHandler = connectionHandler;
        this.connectionHandlers = connectionHandlers;
        this.messages = messages;
        this.maxSizeFile = maxSizeFile;
        this.lengthName = lengthName;
    }

    /**
     * Получает информацию о всех доступных файлах, загруженных на сервер.
     *
     * @return Список всех файлов сервера
     */
    private String computeFiles() {
        File directory = new File("C:/Java_Education/ITMO/CourseProject3/server/src/files");//C:/Java_Education/ITMO/CourseProject3
        File[] subFiles = directory.listFiles();
        out.println("Список доступных файлов на сервере:");
        StringBuilder builderFilesDirectory = new StringBuilder("Список доступных файлов на сервере:\n");
        if (nonNull(subFiles)) {
            Arrays.stream(subFiles).filter(File::isFile)
                    .map(File::toString)
                    .map(pathFile -> pathFile.split("\\\\"))
                    .map(pathFileDivision -> pathFileDivision[pathFileDivision.length - 1])
                    .forEach(fileName -> builderFilesDirectory.append(fileName).append("\n"));
        } else {
            builderFilesDirectory.append("Файлы на сервере отсутствуют.");
        }
        out.println(builderFilesDirectory);

        return builderFilesDirectory.toString();


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
            out.println(fromClient.getText());
            Message message = new Message("server: " + fromClient.getSender());
            if (Objects.equals(fromClient.getText(), downloadFileCommand)) {
                FileTxtMessage fileTxtMessage = (FileTxtMessage) fromClient;
                if (fileTxtMessage.getSizeFile() <= maxSizeFile * 1_000_000 || fileTxtMessage.getFileName().length() > lengthName) {
                    try {
                        String fileName = checkFile(fileTxtMessage.getFileName());
                        writeString(get("C:/Java_Education/ITMO/CourseProject3/server/src/files/" + fileName), fileTxtMessage.getTextFile(), ///**
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                        message.setText("На сервер добавлен файл %s.".formatted(fileName));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    message.setText("Файл превышает %sМб или слишком длинный.".formatted(maxSizeFile));
                    allSending = false;
                }
                // Работа с получением всех доступных файлов на сервере
            } else if (Objects.equals(getNameFilesCommand, fromClient.getText())) {
                message.setText(computeFiles());
                allSending = false;
            } else {
                message.setText("%s\n%s".formatted(fromClient.getSender(), fromClient.getText()));
            }
            try {
                messages.put(message); //метод будет заблокирован блокирующей очередью, если список переполнен
            } catch (InterruptedException e) {
                logger.log(WARNING, "Пул сообщений переполнен.");
            }
            new SendingMessagesOfClients(connectionHandler, message, allSending, connectionHandlers, messages).start();
        }
    }

    /**
     * Вспомогательный метод, который проверяет наличие этого файла в директории
     */
    private String checkFile(String nameFile) {
        Set<String> allFilesSet = new HashSet<>(asList(computeFiles().split("\n")));
        if (allFilesSet.contains(nameFile)) {
            int numberVersion = 0;
            String[] fileNameDivision = nameFile.split("\\.");
            while (allFilesSet.contains(nameFile)) {
                StringBuilder newFileName = new StringBuilder();
                Arrays.stream(fileNameDivision)
                        .limit(fileNameDivision.length - 1)
                        .forEach(newFileName::append);
                newFileName.append("(").append(++numberVersion).append(").").append(fileNameDivision[fileNameDivision.length - 1]);
                nameFile = newFileName.toString();
            }
        }
        return nameFile;
    }
}
