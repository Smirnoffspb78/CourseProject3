package com.company.smirnov.common;

import java.io.File;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;


/**
 * Отправляет файлы
 */
public class FileTxtMessage extends Message {
    private final String fileName;
    private String textFile = "";
    private final long sizeFile;

    /**
     * Конструктор создает сообщение.
     *
     * @param sender Отправитель
     */
    public FileTxtMessage(String sender, String pathFile) {
        super(sender);
        if (nonNull(pathFile)) {
            if (pathFile.isBlank()) {
                throw new IllegalArgumentException("pathFile is blank");
            }
        } else {
            throw new NullPointerException("pathFile=null");
        }
        String[] namesTxt = pathFile.split("/");
        fileName = namesTxt[namesTxt.length - 1];
        sizeFile = new File(fileName).length();
        String[] formats= ".".split(fileName);
        String checkFormat=formats[formats.length-1];
        if (Objects.equals(checkFormat, "txt")){
           throw new IllegalArgumentException("format is not txt");
        }
    }

    public long getSizeFile() {
        return sizeFile;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTextFile() {
        return textFile;
    }

    public void setTextFile(String textFile) {
        this.textFile = requireNonNull(textFile);
    }
}
