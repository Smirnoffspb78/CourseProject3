package com.company.smirnov.server;

import com.company.smirnov.common.Message;

import java.io.File;
import java.util.Arrays;

import static java.lang.System.out;

public enum RequestsServer {
    FILES("/files", "Получение списка доступных файлов."){
        @Override
        public Message messageForClient() {
            File directory = new File("C:/Java_Education/ITMO/CourseProject3");
            File[] subFiles = directory.listFiles();
            out.println("Список доступных файлов:");
            if (subFiles != null) {
                Arrays.stream(subFiles).filter(File::isFile)
                        .map(File::toString)
                        .forEach(out::println);
            }
            return null/*new Message("Default message")*/;
        }
    };

    private final String nameRequest;
    private final String descriptionRequest;

    RequestsServer(String nameRequest, String descriptionRequest) {
        this.nameRequest = nameRequest;
        this.descriptionRequest = descriptionRequest;
    }

    public Message messageForClient() {
        return null/*new Message("Default message")*/;
    }
}
