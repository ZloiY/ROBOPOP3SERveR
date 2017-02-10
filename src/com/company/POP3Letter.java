package com.company;

import java.io.File;

/**
 * Класс, используемый для представления информации о каждом 
 * письме в почтовом ящике пользователя.
 * <p>Содержит информацию о файле, хранящем текст письма, статус письма и объем памяти, занимаемой файлом письма.
 */
public class POP3Letter implements POP3Defines {
    private File letterFile;
    private int status;
    private long size;

    /**
     * Конструктор класса.
     * @param nStatus статус письма
     * @param nSize размер письма
     * @param letterFile файл письма
     */
    public POP3Letter(int nStatus, long nSize, File letterFile) {
        status = nStatus;
        size = nSize;
        this.letterFile = letterFile;
    }

    /**
     * Присваисвает письму статус "удалено".
     */
    public void delete() {
        status = POP3_MSG_STATUS_DELETED;
    }

    /**
     * Сбрасывает статус письма
     */
    public void reset() {
        status = POP3_MSG_STATUS_INITIAL;
    }

    /**
     * Возвращает статус письма.
     * @return статус письма
     */
    public int getStatus() {
        return status;
    }

    /**
     * Возвращает размер письма.
     * @return размер письма
     */
    public long getSize() {
        return size;
    }

    /**
     * Возвращает файл, содержащий текст письма.
     * @return файл с текстом письма
     */
    public File getFile() {
        return letterFile;
    }
}
