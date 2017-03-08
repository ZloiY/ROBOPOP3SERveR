package com.company;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.MessageBuilder;
import org.apache.james.mime4j.message.MultipartBuilder;
import org.apache.james.mime4j.message.SingleBodyBuilder;
import org.apache.james.mime4j.stream.NameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Terenfear on 03.03.2017.
 */


/**
 * Класс, используемый для составления письма, представленного по спецификации MIME.
 * <p>В данной реализации сервера каждое письмо представлено в виде набора файлов, хранящихся в отдельной директории. Инофрмация об иерархии частей письма и заголовках этих частей хранится в файле {@link #PARTS_JSON} в этой же директории.
 */
public class MessageAssembler {
    /**
     * Имя файла в формате <i>.json</i>, хранящего информацию о структуре письма.
     */
    public final static String PARTS_JSON = "parts.json";
    /**
     * Текст содержимого письма.
     */
    private String text = null;
    /**
     * Массив файлов, прикреплённых к письму.
     */
    private List<File> attachments = null;
    /**
     * Уникальный идентификатор письма в ящике.
     */
    private String uniqueId = null;

    /**
     * Создаёт письмо по спецификации MIME, используя файлы из указанной директории.
     *
     * @param dir директория, содержащая файлы, необходимые для создания письма
     * @return собранное из файлов письмо
     */
    public Message assemble(File dir) {
        Message message = null;
        attachments = new ArrayList<>();
        File[] jsons = dir.listFiles((dir2, name) -> name.equals(PARTS_JSON));
        if (jsons != null && jsons.length == 1) {
            JSONObject parsedJSON = parseJSON(jsons[0]);
            if (parsedJSON != null) {
                try {
                    message = (Message) assembleEntity(parsedJSON, dir, true);
                } catch (org.apache.james.mime4j.dom.field.ParseException | java.text.ParseException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        }
        return message;
    }

    /**
     * Собирает письмо или его часть в виде объекта класса {@link Entity} по описанной в JSON-объекте структуре, исользуя данные из указанного каталога.
     *
     * @param jsonIn    JSON-объект, содержащий структуру части письма или его части
     * @param dir       каталог, содержащий необходимые файлы
     * @param isMessage является ли собираемый объект письмом или частью письма
     * @return письмо или его часть в виде объекта класса {@link Entity}
     */
    private Entity assembleEntity(JSONObject jsonIn, File dir, boolean isMessage) throws org.apache.james.mime4j.dom.field.ParseException, java.text.ParseException, UnsupportedEncodingException {
        Entity entity = null;
        MessageBuilder messageBuilder = MessageBuilder.create();
        JSONObject jsonHeader = (JSONObject) jsonIn.get("header");
        if (isMessage)
            fillMainHeader(jsonHeader, messageBuilder, dir);
        fillContentTransferEncoding(jsonHeader, messageBuilder);
        fillContentType(jsonHeader, messageBuilder);
        fillContentDisposition(jsonHeader, messageBuilder);
        if (isMultipart(jsonHeader)) {
            JSONArray jsonBodyArray = (JSONArray) jsonIn.get("body");
            MultipartBuilder mBuilder = MultipartBuilder.create(getContentSubtype(jsonHeader));
            for (Object object : jsonBodyArray) {
                JSONObject jsonPart = (JSONObject) object;
                Entity assembledPart = assembleEntity(jsonPart, dir, false);
                if (assembledPart != null)
                    mBuilder.addBodyPart(assembledPart);
            }
            if (mBuilder == null) {
                System.err.println("Error while assembling multipart in " + dir);
                return null;
            }
            messageBuilder.setBody(mBuilder.build());
        } else {
            String bodyFileName = (String) jsonIn.get("body");
            File bodyFile = new File(dir + File.separator + bodyFileName);
            if (!bodyFile.exists() || !bodyFile.isFile()) {
                System.err.println("No body file in" + bodyFile);
                return null;
            }
            String charset = getContentCharset(jsonHeader);
            if (charset != null) {
                try (Reader in = new InputStreamReader(new FileInputStream(bodyFile), charset)) {
                    SingleBodyBuilder sBuilder = SingleBodyBuilder.create();
                    sBuilder.setCharset(Charset.forName(charset));
                    text = IOUtils.toString(in);
                    sBuilder.setText(text);
                    messageBuilder.setBody(sBuilder.build());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                try (FileInputStream in = new FileInputStream(bodyFile)) {
                    SingleBodyBuilder sBuilder = SingleBodyBuilder.create();
                    sBuilder.readFrom(in);
                    messageBuilder.setBody(sBuilder.build());
                    attachments.add(bodyFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        entity = messageBuilder.build();
        return entity;
    }

    /**
     * Заполняет главный заголовок письма.
     *
     * @param jsonHeader JSON-объект, содержащий полную информацию о заголовке
     * @param builder    объект, строящий письмо
     * @param dir        каталог, содержащий необходимые файлы
     */
    private void fillMainHeader(JSONObject jsonHeader, MessageBuilder builder, File dir) throws org.apache.james.mime4j.dom.field.ParseException, java.text.ParseException {
        String mainHeaderName = (String) jsonHeader.get("message_header_file");
        File mainHeaderFile = new File(dir + File.separator + mainHeaderName);
        if (!mainHeaderFile.exists() || !mainHeaderFile.isFile()) {
            System.err.println("No header file in" + mainHeaderFile);
            return;
        }
        JSONObject jsonMainHeader = parseJSON(mainHeaderFile);
        JSONArray fromArray = (JSONArray) jsonMainHeader.get("from");
        JSONArray toArray = (JSONArray) jsonMainHeader.get("to");
        List<Mailbox> fromMBoxes = new ArrayList<>();
        List<Mailbox> toMBoxes = new ArrayList<>();
        fromArray.forEach((Object object) -> {
            JSONObject jsonObject = (JSONObject) object;
            fromMBoxes.add(new Mailbox((String) jsonObject.get("name"), (String) jsonObject.get("local_part"), (String) jsonObject.get("domain")));
        });
        toArray.forEach((Object object) -> {
            JSONObject jsonObject = (JSONObject) object;
            toMBoxes.add(new Mailbox((String) jsonObject.get("name"), (String) jsonObject.get("local_part"), (String) jsonObject.get("domain")));
        });
        builder.setFrom(fromMBoxes);
        builder.setTo(toMBoxes);
        DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        String date = (String) jsonMainHeader.get("date");
        builder.setDate(df.parse(date));
        String subject = (String) jsonMainHeader.get("subject");
        builder.setSubject(subject);
        String messageId = (String) jsonMainHeader.get("message_id");
        builder.setMessageId(messageId);
        uniqueId = (String) jsonMainHeader.get("unique_id");
    }

    /**
     * Заполняет часть заголовка, отвечающую за тип содержимого письма или его части.
     *
     * @param jsonHeader JSON-объект, содержащий информацию о типе содержимого
     * @param builder    объект, конструирующий письмо
     */
    private void fillContentType(JSONObject jsonHeader, MessageBuilder builder) {
        JSONObject jsonCType = (JSONObject) jsonHeader.get("content_type");
        String mimeType = (String) jsonCType.get("mime_type");
        JSONObject jsonCTparams = (JSONObject) jsonCType.get("params");
        List<NameValuePair> pairs = new ArrayList<>();
        jsonCTparams.forEach((key, value) -> {
            try {
                pairs.add(new NameValuePair((String) key, MimeUtility.encodeText((String) value, String.valueOf(StandardCharsets.UTF_8), "B")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        builder.setContentType(mimeType);
    }

    /**
     * Заполняет часть заголовка, отвечающую за расположение содержимого письма или его части.
     *
     * @param jsonHeader JSON-объект, содержащий информацию о расположении содержимого
     * @param builder    объект, конструирующий письмо
     */
    private void fillContentDisposition(JSONObject jsonHeader, MessageBuilder builder) throws UnsupportedEncodingException {
        JSONObject jsonCDisp = (JSONObject) jsonHeader.get("content_disposition");
        if (jsonCDisp != null) {
            String dispType = (String) jsonCDisp.get("type");
            String dispFilename = (String) jsonCDisp.get("filename");
            dispFilename = MimeUtility.encodeWord(dispFilename, String.valueOf(StandardCharsets.UTF_8), "B");
            builder.setContentDisposition(dispType, dispFilename);
        }
    }

    /**
     * Заполняет часть заголовка, отвечающую за шифрование содержимого письма или его части.
     *
     * @param jsonHeader JSON-объект, содержащий информацию о шифровании содержимого
     * @param builder    объект, конструирующий письмо
     */
    private void fillContentTransferEncoding(JSONObject jsonHeader, MessageBuilder builder) {
        String encoding = (String) jsonHeader.get("content_transfer_encoding");
        builder.setContentTransferEncoding(encoding);
    }

    /**
     * Проверяет, является ли письмо или его часть составным объектом.
     *
     * @param jsonHeader JSON-объект, содержащий заголовок письма или его части.
     * @return {@code true} если является, иначе {@code false}
     */
    private boolean isMultipart(JSONObject jsonHeader) {
        JSONObject jsonCType = (JSONObject) jsonHeader.get("content_type");
        return (boolean) jsonCType.get("is_multipart");
    }

    /**
     * Возвращает кодировку текстового содержимого письма или его части.
     * @param jsonHeader JSON-объект, содержащий заголовок письма или его части
     * @return кодировка текстового содержимого. Если передана не текстовая часть, то возвращает {@code null}
     */
    private String getContentCharset(JSONObject jsonHeader) {
        JSONObject jsonCType = (JSONObject) jsonHeader.get("content_type");
        JSONObject jsonCTparams = (JSONObject) jsonCType.get("params");
        return (String) jsonCTparams.get("charset");
    }

    /**
     * Возвращает подтип содержимого письма или его части.
     * @param jsonHeader JSON-объект, содержащий заголовок письма или его части
     * @return подтип текстового содержимого
     */
    private String getContentSubtype(JSONObject jsonHeader) {
        JSONObject jsonCType = (JSONObject) jsonHeader.get("content_type");
        String mimeType = (String) jsonCType.get("mime_type");
        return mimeType.split("/")[1];
    }

    /**
     * Получает JSON-объект из файла.
     *
     * @param file файл, содержащий JSON-объект
     * @return данные в виде JSON-объекта
     */
    private JSONObject parseJSON(File file) {
        JSONObject jsonObject = null;
        JSONParser parser = new JSONParser();
        try (Reader fileReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            jsonObject = (JSONObject) parser.parse(fileReader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return jsonObject;
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
     * Возвращает массив с файлами, прикреплёнными к письму.
     *
     * @return массив файлов, прикреплённых к письму
     */
    public List<File> getAttachments() {
        return attachments;
    }

    /**
     * Возвращает уникальный идентификатор письма.
     *
     * @return уникальный идентификатор письма
     */
    public String getUniqueId() {
        return uniqueId;
    }
}
