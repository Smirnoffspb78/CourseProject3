package com.company.smirnov.client;

import com.company.smirnov.common.FileTxtMessage;
import com.company.smirnov.common.Message;
import com.company.smirnov.common.ReceivingAndSendingMessage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.io.File.separator;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.getLogger;
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

    private final System.Logger logger = getLogger(Reader.class.getName());

    /**
     * Устанавливает соединение между клиентом и сервером.
     */
    private final ReceivingAndSendingMessage connectionHandler;
    /**
     * Относительный путь для сохранения файлов
     */
    private final String directorySaveFile = ".%sclient%ssrc%sfiles%s".replace("%s", separator);

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
            Set<String> allFilesSet = new HashSet<>();
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
                logger.log(ERROR,"Соединение с сервером прервано");
                try {
                    connectionHandler.close();
                } catch (Exception ex) {
                    logger.log(ERROR, "Соединение отсутствует");
                }
                return;
            }
            if (message instanceof FileTxtMessage fileTxtMessage) {
                try {
                    String filename = fileTxtMessage.getFileName();
                    filename = checkFile(filename);
                    writeString(get(directorySaveFile
                                    + filename),
                            fileTxtMessage.getTextFile(),
                            CREATE,
                            APPEND,
                            SYNC);
                    logger.log(INFO,"Получен файл c сервера.");
                } catch (IOException e) {
                    logger.log(ERROR,"Не удалось загрузить файл.");
                }
            } else {
                out.printf("%s: %s%n%s%n", message.getSender(), message.getTimeOfSending(), message.getText());
            }
        }
    }
}