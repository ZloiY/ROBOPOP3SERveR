package com.company;

import java.io.File;

public class POP3Message implements POP3Defines {
    private File messageFile;
    private int status;
    private long size;

    /**
     * Конструктор класса.
     * @param nStatus статус сообщения
     * @param nSize размер сообщения
     * @param messageFile файл сообщения
     */
    public POP3Message(int nStatus, long nSize, File messageFile) {
        status = nStatus;
        size = nSize;
        this.messageFile = messageFile;
    }

    /**
     * Присваисвает статусу сообщения статус удаленно
     */
    public void delete() {
        status = POP3_MSG_STATUS_DELETED;
    }

    /**
     * Сбрасывает статус сообщения
     */
    public void reset() {
        status = POP3_MSG_STATUS_INITIAL;
    }

    /**
     * @return статус сообщения
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return разммер сообщения
     */
    public long getSize() {
        return size;
    }

    /**
     * @return файл сообщения
     */
    public File getFile() {
        return messageFile;
    }
}
