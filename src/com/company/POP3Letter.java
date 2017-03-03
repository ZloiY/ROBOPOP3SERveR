package com.company;


import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Класс, используемый для представления информации о каждом
 * письме в почтовом ящике пользователя.
 * <p>Содержит информацию о файле, хранящем текст письма, статус письма и объем памяти, занимаемой файлом письма.
 */
public class POP3Letter implements POP3Defines {
    /**
     * Директория, содержащая файлы пиьсма.
     */
    private File letterDir;
    /**
     * Письмо, офрмленное стандартно формату MIME.
     */
    private Message mimeMessage;
    /**
     * Заголовок письма.
     */
    private String header;
    /**
     * Текст письма.
     */
    private String text;
    /**
     * Массив, содержаший файлы, прикреплённые к письму.
     */
    private List<File> attachments;
    /**
     * Уникальный идентификатор пиьсма.
     */
    private String uniqueId;
    /**
     * Статус пиьсма.
     */
    private int status;
    /**
     * Размер пиьсма в байтах.
     */
    private long size = -1;

    /**
     * Конструктор класса.
     *
     * @param nStatus   статус письма
     * @param letterDir директория письма
     */
    public POP3Letter(int nStatus, File letterDir) {
        status = nStatus;
        this.letterDir = letterDir;
        MessageAssembler assembler = new MessageAssembler();
        mimeMessage = assembler.assembleFromFiles(letterDir);
        header = mimeMessage.getHeader().toString();
        try (ByteArrayOutputStream baoStream = new ByteArrayOutputStream()) {
            DefaultMessageWriter writer = new DefaultMessageWriter();
            writer.writeMessage(mimeMessage, baoStream);
            size = baoStream.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            text = assembler.getText();
            attachments = assembler.getAttachments();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        uniqueId = letterDir.getName();
    }

    /**
     * Отправляет клиенту содержимое письмо, оформленное в соответствии со спецификацией MIME.
     *
     * @param out выходной поток, в который будет отправлено письмо
     */
    public void send(OutputStream out) throws IOException {
        MessageWriter writer = new DefaultMessageWriter();
        writer.writeMessage(mimeMessage, out);

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
     *
     * @return статус письма
     */
    public int getStatus() {
        return status;
    }

    /**
     * Возвращает размер письма.
     *
     * @return размер письма
     */
    public long getSize() {
        return size;
    }

    /**
     * Возвращает уникальный ID письма.
     *
     * @return ID письма
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Возвращает директорию, в которой находятся файлы письма.
     *
     * @return ID письма
     */
    public File getLetterDir() {
        return letterDir;
    }

    /**
     * Возвращает текст письма.
     *
     * @return текст письма
     */
    public String getText() {
        return text;
    }

    /**
     * Возвращает список прикреплённых к сообщению файлов.
     *
     * @return список прикреплённыйх файлов
     */
    public List<File> getAttachments() {
        return attachments;
    }

    /**
     * Возвращает заголовок письма.
     *
     * @return заголовок письма
     */
    public String getHeader() {
        return header;
    }

    /**
     * Возвращет письмо, оформленное по стандарту MIME.
     *
     * @return MIME письмо
     */
    public Message getMimeMessage() {
        return mimeMessage;
    }
}
