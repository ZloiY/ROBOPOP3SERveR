package com.company;

import org.apache.james.mime4j.message.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс, используемый для представления информации о каждом
 * письме в почтовом ящике пользователя.
 * <p>Содержит информацию о файле, хранящем текст письма, статус письма и объем памяти, занимаемой файлом письма.
 */
public class POP3Letter implements POP3Defines {
    private File letterFile;
    private String uniqueId;
    private Message mimeMessage;
    private List<BodyPart> bodyParts;
    private List<Body> binaryBody;
    private TextBody simpleText;
    private TextBody htmlText;
    private SingleBody singleBody;
    private int status;
    private long size;

    /**
     * Конструктор класса.
     *
     * @param nStatus    статус письма
     * @param nSize      размер письма
     * @param letterFile файл письма
     * @param uniqueId   уникальный идентификатор
     */
    public POP3Letter(int nStatus, long nSize, File letterFile, String uniqueId) {
        status = nStatus;
        size = nSize;
        this.letterFile = letterFile;
        this.uniqueId = uniqueId;
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(letterFile))){
            mimeMessage = new Message(bufferedInputStream);
        }catch (IOException e){
            e.printStackTrace();
        }
        if (mimeMessage.isMultipart()) {
            binaryBody = new ArrayList<Body>();
            for (int i = 0; i < bodyParts.size(); i++) {
                BodyPart bodyPart = bodyParts.get(i);
                Body someBody = bodyPart.getBody();
                if (someBody instanceof BinaryBody)
                    binaryBody.add(someBody);
                else switch (someBody.getParent().getMimeType()) {
                    case "text/plain":
                        simpleText = (TextBody) someBody;
                        break;
                    case "text/html":
                        htmlText = (TextBody) someBody;
                        break;
                }
            }
        }
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
     * Возвращает файл, содержащий текст письма.
     *
     * @return файл с текстом письма
     */
    public File getFile() {
        return letterFile;
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
     * Возвращает заголовок письма.
     *
     * @return заголовок письма
     */
    public Header getHeader(){
        return mimeMessage.getHeader();
    }

    /**
     * Возвращает список прикреплённых к сообщению файлов.
     *
     * @return список прикреплённыйх файлов
     */
    public List<Body> getBinaryFiles(){
        return binaryBody;
    }

    /**
     * Возвращает текст письма.
     *
     * @return текст письма
     */
    public TextBody getSimpleText(){
        return simpleText;
    }

    /**
     * Возвращает текст письма в формате hmtl.
     *
     * @return текст письма в формате html
     */
    public TextBody getHtmlText(){
        return htmlText;
    }

    /**
     * Возвращет письмо типа MIME.
     *
     * @return MIME письмо
     */
    public Message getMimeMessage() {
        return mimeMessage;
    }

    /**
     * Возвращает тело письма.
     *
     * @return тело пиьсма
     */
    public Body getMessageBody(){
        return mimeMessage.getBody();
    }
}
