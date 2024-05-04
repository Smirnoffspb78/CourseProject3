package com.company.smirnov.client;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.io.File.separator;
import static java.lang.System.out;
import static java.nio.file.Files.writeString;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Читает сообщения от сервера.
 */
class Reader extends Thread {

    /**
     * Устанавливает соединение между клиентом и сервером.
     */
    private final ReceivingAndSendingMessage connectionHandler;
    /**
     * Относительный путь для сохранения файлов
     */
    private final String directorySaveFile = ".%sclient%ssrc%sfiles%s".formatted(separator, separator, separator, separator);

    /**
     * Конструктор создает объект для чтения сообщений.
     *
     * @param connectionHandler Устанавливает соединение между клиентом и сервером
     */
    public Reader(ReceivingAndSendingMessage connectionHandler) {
        this.connectionHandler = requireNonNull(connectionHandler);
    }

    /**
     * Вспомогательный метод, который проверяет наличие этого файла в директории
     */
    private String checkFile(String nameFile) {
        File directory = new File(directorySaveFile);
        File[] subFiles = directory.listFiles();
        if (nonNull(subFiles)) {
            Set<String> allFilesSet = new CopyOnWriteArraySet<>();
            Arrays.stream(subFiles).filter(File::isFile)
                    .map(File::toString)
                    .map(pathFile -> pathFile.split("\\\\"))
                    .map(pathFileDivision -> pathFileDivision[pathFileDivision.length - 1])
                    .forEach(allFilesSet::add);
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
        }
        return nameFile;
    }


    @Override
    public void run() {
        while (true) {
            Message message;
            try {
                message = connectionHandler.read();
            } catch (Exception e) {
                out.println("Соединение с сервером прервано");
                return;
            }
            if (message instanceof FileTxtMessage) {
                try {
                    String filename = ((FileTxtMessage) message).getFileName();
                    filename = checkFile(filename);
                    writeString(get(directorySaveFile.formatted(separator, separator, separator, separator)
                                    + filename),
                            ((FileTxtMessage) message).getTextFile(),
                            CREATE,
                            APPEND,
                            SYNC);
                    out.println("Получен файл c сервера.");
                } catch (IOException e) {
                    out.println("Не удалось загрузить файл.");
                }
            } else {
                out.printf("%s: %s%n%s%n", message.getSender(), message.getTimeOfSending(), message.getText());
            }
        }
    }
}