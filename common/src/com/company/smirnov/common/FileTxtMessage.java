package com.company.smirnov.common;

import java.io.File;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;


/**
 * Отправляет файлы
 */
public class FileTxtMessage extends Message {
    /**
     * Наименование файла.
     */
    private final String fileName;
    /**
     * Размер файла.
     */
    private final double sizeFile;
    /**
     * Описание файла.
     */
    private final String descriptionFile;
    /**
     * Содержимое файла
     */
    private String textFile = "";

    /**
     * Конструктор создает сообщение.
     *
     * @param sender Отправитель
     */
    public FileTxtMessage(String sender, String pathFile, String descriptionFile) {
        super(sender);
        this.descriptionFile = requireNonNull(descriptionFile);
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
        String[] formats = ".".split(fileName);
        String checkFormat = formats[formats.length - 1];
        if (Objects.equals(checkFormat, "txt")) {
            throw new IllegalArgumentException("format is not txt");
        }
    }

    public boolean checkSize(double maxLength) {
        return sizeFile <= maxLength;
    }

    public double getSizeFile() {
        return sizeFile;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTextFile() {
        return textFile;
    }

    public String getDescriptionFile() {
        return descriptionFile;
    }

    public void setTextFile(String textFile) {
        this.textFile = requireNonNull(textFile);
    }
}
