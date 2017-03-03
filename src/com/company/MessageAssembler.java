package com.company;

import com.sun.istack.internal.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.message.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Terenfear on 03.03.2017.
 */


/**
 * Класс, используемый для составления письма, представленного по спецификации MIME.
 * <p>В данной реализации сервера каждое письмо представлено в виде набора файлов, хранящихся в отдельной директории. В данной реализации сервера каждая атомарная часть письма хранится в виде двух файлов: файла с содержанием и файла-заголовка. Части письма, содержащие другие части, представлены только в виде файла-заголовка. Инофрмация об иерархии частей и связи заголовков с содержимым, а также информация о кодировке (для текстовых частей) и подтипе (для составных частей), хранится в файле {@link #JSON_FILE} в той же директории.
 * <p>
 * Пример:
 * <ul>
 * <li>Содержимое папки письма:
 * <blockquote><pre>
 * 1398064678_499298938.gif
 * 9QF13x0YvZ0.jpg
 * header_1398064678_499298938.gif.hr
 * header_9QF13x0YvZ0.jpg.hr
 * header_labyrinth2.jpg.hr
 * header_multipart0.hr
 * header_multipart00.hr
 * header_textfile000.hr
 * header_textfile001.hr
 * labyrinth2.jpg
 * mainheader.hr
 * parts.json
 * textfile000.txt
 * textfile001.html
 * </pre></blockquote>
 * <li>Содержимое файла {@link #JSON_FILE}:
 * <blockquote><pre>
 * [
 * &nbsp;{
 * &nbsp;&nbsp;"is_multipart": true,
 * &nbsp;&nbsp;"subtype": "mixed",
 * &nbsp;&nbsp;"header": "header_multipart0.hr",
 * &nbsp;&nbsp;"body": [
 * &nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": true,
 * &nbsp;&nbsp;&nbsp;&nbsp;"subtype": "alternative",
 * &nbsp;&nbsp;&nbsp;&nbsp;"header": "header_multipart00.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;"body": [
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"charset": "UTF-8",
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": false,
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"header": "header_textfile000.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"body": "textfile000.txt"
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;},
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"charset": "UTF-8",
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": false,
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"header": "header_textfile001.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"body": "textfile001.html"
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}
 * &nbsp;&nbsp;&nbsp;&nbsp;]
 * &nbsp;&nbsp;&nbsp;},
 * &nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": false,
 * &nbsp;&nbsp;&nbsp;&nbsp;"header": "header_9QF13x0YvZ0.jpg.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;"body": "9QF13x0YvZ0.jpg"
 * &nbsp;&nbsp;&nbsp;},
 * &nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": false,
 * &nbsp;&nbsp;&nbsp;&nbsp;"header": "header_1398064678_499298938.gif.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;"body": "1398064678_499298938.gif"
 * &nbsp;&nbsp;&nbsp;},
 * &nbsp;&nbsp;&nbsp;{
 * &nbsp;&nbsp;&nbsp;&nbsp;"is_multipart": false,
 * &nbsp;&nbsp;&nbsp;&nbsp;"header": "header_labyrinth2.jpg.hr",
 * &nbsp;&nbsp;&nbsp;&nbsp;"body": "labyrinth2.jpg"
 * &nbsp;&nbsp;&nbsp;}
 * &nbsp;&nbsp;]
 * &nbsp;}
 * ]
 * </pre></blockquote>
 * </ul>
 */
public class MessageAssembler {
    /**
     * Имя файла в формате <i>.json</i>, хранящего информацию о структуре письма.
     */
    public final static String JSON_FILE = "parts.json";
    /**
     * Имя хранящего заголовок поля в файле {@link #JSON_FILE}.
     */
    public final static String HEADER = "header";
    /**
     * Имя хранящего содержимое поля в файле {@link #JSON_FILE}.
     */
    public final static String BODY = "body";
    /**
     * Имя хранящего кодировку поля в файле {@link #JSON_FILE}.
     */
    public final static String CHARSET = "charset";
    /**
     * Имя хранящего подтип поля в файле {@link #JSON_FILE}.
     */
    public final static String SUBTYPE = "subtype";
    /**
     * Имя поля, хрнанящего информацию, является ли часть составной, в файле {@link #JSON_FILE}.
     */
    public final static String IS_MULTIPART = "is_multipart";
    /**
     * Создаёт части содержимого письма, используя иноформацию из файлов.
     */
    private BodyFactory bodyFactory = new BasicBodyFactory();
    /**
     * Создаёт заголовки письма и его частей, используя иноформацию из файлов.
     */
    private MessageBuilder messageBuilder = new DefaultMessageBuilder();
    /**
     * Текст содержимого письма.
     */
    private String text = null;
    /**
     * Массив файлов, прикреплённых к письму.
     */
    private List<File> attachments = null;

    /**
     * Создаёт письмо по спецификации MIME, используя файлы из указанной директории. Описание структуры файлов находится в описании класса.
     *
     * @param dir директория, содержащая файлы, необходимые для создания письма
     * @return собранное из файлов письмо
     */
    public Message assembleFromFiles(File dir) {
        Message entity = new MessageImpl();
        attachments = new ArrayList<>();
        File[] jsons = dir.listFiles((dir2, name) -> name.equals(JSON_FILE));
        if (jsons != null && jsons.length == 1) {
            JSONArray parsedJSON = parseJSON(jsons[0]);
            if (parsedJSON != null) {
                for (Object object : parsedJSON) {
                    JSONObject part = (JSONObject) object;
                    fillEntityFromJSON(entity, part, dir);
                }
            }
        }
        return entity;
    }

    /**
     * Получает JSON-массив из файла.
     *
     * @param file файл, содержащий JSON-массив
     * @return данные в виде JSON-массива
     */
    private JSONArray parseJSON(File file) {
        JSONArray jsonArray = null;
        JSONParser parser = new JSONParser();
        try (Reader fileReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            jsonArray = (JSONArray) parser.parse(fileReader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    /**
     * Заполняет содержимое и заголовок письма или его части, используя данные из JSON-объекта.
     *
     * @param entity     письмо или его часть
     * @param jsonObject JSON-объект, хранящий инофрмацию о письме или его части
     * @param dir        директория, в которой находятся файлы письма
     */
    private void fillEntityFromJSON(Entity entity, JSONObject jsonObject, File dir) {
        Boolean isMultipart = (Boolean) jsonObject.get(IS_MULTIPART);
        String headerName = (String) jsonObject.get(HEADER);
        String charSet = (String) jsonObject.get(CHARSET);
        String subType = (String) jsonObject.get(SUBTYPE);
        if (isMultipart) {
            JSONArray parsedParts = (JSONArray) jsonObject.get(BODY);
            fillMultipartBody(entity, parsedParts, dir, subType);
        } else {
            String bodyName = (String) jsonObject.get(BODY);
            fillSingleBody(entity, new File(dir + File.separator + bodyName), charSet);
        }
        fillHeader(entity, new File(dir + File.separator + headerName));
    }

    /**
     * Заполняет содержимое письма или его атомарной части, используя данные из файла и указанную кодировку (кодировка используется только для текстовых частей).
     *
     * @param entity  письмо или его неделимая часть
     * @param file    файл, данные из которого используются при заполнении
     * @param charSet кодировка, используемая при заполнении (только для текстовых частей)
     */
    private void fillSingleBody(Entity entity, File file, @Nullable String charSet) {
        try (FileInputStream in = new FileInputStream(file)) {
            Body body;
            if (charSet == null) {
                body = bodyFactory.binaryBody(in);
                attachments.add(file);
            } else {
                body = bodyFactory.textBody(in, charSet);
                if (text == null) {
                    text = IOUtils.toString(((TextBody) body).getReader());
                }
            }
            entity.setBody(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Заполняет содержимое составного письма или его составной части, используя информацию, находящуюся в JSON-массиве, и устанавливает подтип составной части.
     *
     * @param entity    составное письмо или составная часть письма
     * @param jsonArray JSON-массив, хранящий информацию, необходимую для заполнения
     * @param dir       директория, в которой находятся файлы письма
     * @param subType   подтип составной части письма
     */
    private void fillMultipartBody(Entity entity, JSONArray jsonArray, File dir, String subType) {
        Multipart multiBody = new MultipartImpl(subType);
        List<Entity> bodyParts = new ArrayList<>();
        for (Object object : jsonArray) {
            Entity parsedBodyPart = new BodyPart();
            JSONObject jsonBodyPart = (JSONObject) object;
            fillEntityFromJSON(parsedBodyPart, jsonBodyPart, dir);
            bodyParts.add(parsedBodyPart);
        }
        multiBody.setBodyParts(bodyParts);
        entity.setBody(multiBody);
    }

    /**
     * Заполняет заголовок письма или его части, используя содержимое файла.
     *
     * @param entity письмо или часть письма
     * @param file   файл, содержащий заголовок
     */
    private void fillHeader(Entity entity, File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            entity.setHeader(messageBuilder.parseHeader(in));
        } catch (IOException | MimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Возвращает текст письма.
     *
     * @return текст письма
     * @throws NullPointerException при вызове до {@link #assembleFromFiles(File) сборки сообщения}
     */
    public String getText() throws NullPointerException {
        return text;
    }

    /**
     * Возвращает массив с файлами, прикреплёнными к письму.
     *
     * @return массив файлов, прикреплённых к письму
     * @throws NullPointerException при вызове до {@link #assembleFromFiles(File) сборки сообщения}
     */
    public List<File> getAttachments() throws NullPointerException {
        return attachments;
    }
}
