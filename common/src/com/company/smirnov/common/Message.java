package com.company.smirnov.common;

import java.io.Serializable;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

/**
 * Сообщение.
 */
public class Message implements Serializable {
    /**
     * Время отправки сообщения.
     */
    private LocalDateTime timeOfSending;
    /**
     * Текст сообщения.
     */
    private String text;
    /**
     * Имя отправителя
     */
    private final String sender;

    /**
     * Конструктор создает сообщение.
     *
     * @param sender Отправитель
     */
    public Message(String sender) {
        this.sender = requireNonNull(sender, "sender=null");
    }

    public LocalDateTime getTimeOfSending() {
        return timeOfSending;
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }

    public void setTimeOfSending(LocalDateTime timeOfSending) {
        this.timeOfSending = requireNonNull(timeOfSending, "timeOfSending=null");
    }

    public void setText(String text) {
        this.text = requireNonNull(text, "text=null");
    }

}

