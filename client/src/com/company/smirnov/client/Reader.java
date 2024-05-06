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
import static java.nio.file.Files.write;
import static java.nio.file.Paths.get;
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
    private Set<String> checkFile() {
        File directory = new File(directorySaveFile);
        File[] subFiles = directory.listFiles();
        Set<String> allFilesSet = new HashSet<>();
        if (nonNull(subFiles)) {
            Arrays.stream(subFiles).filter(File::isFile)
                    .map(File::toString)
                    .map(pathFile -> pathFile.split("\\\\"))
                    .map(pathFileDivision -> pathFileDivision[pathFileDivision.length - 1])
                    .forEach(allFilesSet::add);
        }
        return allFilesSet;
    }

    @Override
    public void run() {
        while (true) {
            Message message;
            try {
                message = connectionHandler.read();
            } catch (Exception e) {
                logger.log(ERROR, "Соединение с сервером прервано");
                try {
                    connectionHandler.close();
                } catch (Exception ex) {
                    logger.log(ERROR, "Соединение отсутствует");
                }
                return;
            }
            if (message instanceof FileTxtMessage fileTxtMessage) {
                Set<String> allFiles = checkFile();
                if (allFiles.contains(fileTxtMessage.getFileName())) {
                    logger.log(ERROR, "Файл с таким именем уже существует");
                } else {
                    try {
                        write(get(directorySaveFile
                                + fileTxtMessage.getFileName()), fileTxtMessage.getTxtFile());
                        logger.log(INFO, "Получен файл c сервера.");
                    } catch (IOException e) {
                        logger.log(ERROR, "Не удалось загрузить файл.");
                    }
                }
            } else {
                out.printf("%s: %s%n%s%n", message.getSender(), message.getTimeOfSending(), message.getText());
            }
        }
    }
}