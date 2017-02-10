package com.company;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с сессией клиента и сообщениями почтового ящика (класс {@link POP3Message}).
 * <p>Реализует интерфейс почтового ящика (протокол POP3) по регламенту RFC 1225:
 * <ul><li>аутентификацию (методы {@link #processUSER(String)} и {@link #processPASS(String)})
 * <li>получение краткой информации о количестве писем и объёме занимамого ими пространства (метод {@link #processSTAT()})
 * <li>получение краткой информации об идентификаторе пиьсма и объёме занимамого им пространства (также, в зависимости от параметров у полученной от клиента конмаде, может быть получена такая информация о всех письмах) (метод {@link #processLIST(String)})
 * <li>передачу клиенту содержимого запрашиваемого письма (метод {@link #processRETR(String)})
 * <li>удаление запрашиваемого письма (метод {@link #processDELE(String)})
 * <li>получение отклика от сервера (метод {@link #processNOOP()})
 * <li>получение наивысшего идентификатора среди всех писем, к которым было обращение (метод {@link #processLAST()})
 * <li>сброс всех изменений, произведённых пользователем (метод {@link #processRSET()})
 * <li>получение заголовка и заданного количества строк из запрашиваемого сообщения (метод {@link #processTOP(String)})
 * <li>завершение сессии и принятие изменений (метод {@link #processQUIT()})
 * </ul>
 */

public class POP3Session implements POP3Defines {
    private static final String SPLITTER = " ";

    private int state;
    private int lastMsg;
    private File userHome;
    private String userName;
    private String password;
    private long totalMailSize;
    private Socket socConnection;
    private LogThread logThread;
    private List<POP3Message> pop3MessageList;

    /**
     * Контруктор класса.
     *
     * @param clientSoc сокет клиента
     * @param logThread поток для логгирования диалога клиент-сервер
     * @see Socket
     * @see LogThread
     */
    public POP3Session(Socket clientSoc, LogThread logThread) {
        state = POP3_STATE_AUTHORIZATION;
        socConnection = clientSoc;
        this.logThread = logThread;
        pop3MessageList = new ArrayList<>();
        lastMsg = 0;
        totalMailSize = 0;
    }

    /**
     * Отправляет клиенту ответ без индикатора выполнения.
     *
     * @param message строка, которая будет отправлена клиенту в качестве ответа
     */
    public void sendResponse(String message) {
        logThread.log("Direct Sending: " + message);
        try {
            socConnection.getOutputStream().write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляет клиенту ответ с заданным индикатором выполнения.
     *
     * @param nResponseType индикатор выполнения (значения индикаторов объявлены в {@link POP3Defines})
     * @param message       строка, которая будет отправлена клиенту в качестве ответа. Может быть {@code null}, в таком случае будет отправлено краткое сообщение о статусе выполнения действия
     * @return значение индикатора выполнения действия
     */

    public int sendResponse(int nResponseType, @Nullable String message) {
        String buf = "";
        switch (nResponseType) {
            case POP3_DEFAULT_AFFIRMATIVE_RESPONSE:
                if (message != null)
                    buf = "+OK " + message + "\r\n";
                else buf = "+OK Action performed\r\n";
                break;

            case POP3_DEFAULT_NEGATIVE_RESPONSE:
                if (message != null)
                    buf = "-ERR " + message + "\r\n";
                else buf = "-ERR An error occurred\r\n";
                break;

            case POP3_WELCOME_RESPONSE:
                buf = "+OK " + APP_TITLE + " " + APP_VERSION + " POP3 Server ready on\r\n";
                break;

            default:
                throw new RuntimeException("Invalid response type.");
        }
        logThread.log("Sending: " + buf);
        try {
            socConnection.getOutputStream().write(buf.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nResponseType;
    }

    /**
     * Отправляет клиенту краткое сообщение о статусе выполнения задания с заданным индикатором выполнения.
     *
     * @param nResponseType индикатор выполнения (значения индикаторов объявлены в {@link POP3Defines})
     * @return значение индикатора выполнения действия
     */
    public int sendResponse(int nResponseType) {
        return sendResponse(nResponseType, null);
    }

    /**
     * Получает параметр запроса клиента, если он имеется.
     * @param buf строка, содержащая полученный от клиента запрос
     * @return параметр запроса клиента либо {@code null}, если параметра в запросе нет
     */
    private String getParam(String buf) {
        String[] parts = buf.split(SPLITTER);
        if (parts.length > 1)
            return parts[1];
        else return null;
    }

    /**
     * Анализирует полученный от клиента запрос и инициирует выполнение требуемого клиентом действия.
     * @param buf запрос клиента
     * @return индикатор выполнения инициированного действия либо индикатор {@link #POP3_DEFAULT_NEGATIVE_RESPONSE}, если клиент неправильно сформировал запрос
     * @see POP3Defines
     */
    public int processSession(String buf) {
        String cmd = buf.substring(0, 4);
        switch (cmd) {
            case "USER":
                return processUSER(buf);
            case "PASS":
                return processPASS(buf);
            case "QUIT":
                return processQUIT();
            case "STAT":
                return processSTAT();
            case "LIST":
                return processLIST(buf);
            case "RETR":
                return processRETR(buf);
            case "DELE":
                return processDELE(buf);
            case "NOOP":
                return processNOOP();
            case "LAST":
                return processLAST();
            case "RSET":
                return processRSET();
            default:
                if (buf.substring(0, 3).equals("TOP"))
                    return processTOP(buf);
                else logThread.log("God damned russian hackers: " + buf);
        }
        return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
    }

    /**
     * Обрабатывает команду USER, полученную от клиента, и проверяет, корректно ли полученное имя пользователя и зарегестрирован ли данный пользователь на сервере. Действие не будет выполнено, если сервер не находится в состоянии {@link #POP3_STATE_AUTHORIZATION}.
     * @param buf строка, содержащая запрос от клиента
     * @return индикатор выполнения действия
     */
    private int processUSER(String buf) {
        logThread.log("ProcessUSER\n");
        if (state != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        String arguments = getParam(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify the username");
        userName = arguments;
        File connectingUserHome = new File(USERS_DOMAIN + File.separator + userName);
        if (!connectingUserHome.exists()) {
            logThread.log("User " + userName + "'s Home '" + connectingUserHome.getAbsolutePath() + "' not found\n");
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong username");
        }
        logThread.log("OK User " + userName + " Home " + connectingUserHome.getAbsolutePath() + "\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    /**
     * Обрабатывает команду PASS, полученную от клиента, и проверяет, корректен ли полученный пароль и соответствует ли он полученному ранее от клиента имени пользователя, от лица которого происходит аутентификация. Также проверяет, было ли ранее предоставлено клиентом имя пользователя, зарегестрированного в системе (т. е. действие может быть выполнено только после завершения {@link #processUSER(String)} c индикатором {@link #POP3_DEFAULT_AFFIRMATIVE_RESPONSE}). Кроме того, действие не будет выполнено, если сервер не находится в состоянии {@link #POP3_STATE_AUTHORIZATION}.
     * @param buf строка, содержащая запрос от клиента
     * @return индикатор выполнения действия
     */
    private int processPASS(String buf) {
        logThread.log("ProcessPASS\n");
        if (state != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        if (userName.length() < 1)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You did not introduce yourself");
        String arguments = getParam(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify a password");
        password = arguments;
        if (login(userName, password))
            return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Now you can check your mail");
        else return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong password");
    }

    /**
     * Обрабатывает команду QUIT, полученную от клиента. Если на момент обработки сервер находится в состоянии {@link #POP3_STATE_TRANSACTION}, то сервер переходит в состояние {@link #POP3_STATE_UPDATE}, инициируется применение сделанных клиентом изменений и завершение сессии. В других состояниях команда выполнена не будет.
     * @return внутренний индикатор {@link #POP3_SESSION_QUITTED}, сообщающий, что клиент закончил сессию
     */
    private int processQUIT() {
        logThread.log("ProcessQUIT\n");
        if (state == POP3_STATE_TRANSACTION)
            state = POP3_STATE_UPDATE;
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Goodbye");
        updateMails();
        return POP3_SESSION_QUITTED;
    }

    /**
     * Обрабатывает команду STAT, полученную от клиента. Если на момент обработки сервер находится в состоянии {@link #POP3_STATE_TRANSACTION}, то сервер отправляет клиенту краткую информацию о количестве писем и объеме занимаемого ими пространства. В других состояниях команда выполнена не будет.
     * @return индикатор выполнения действия
     */
    private int processSTAT() {
        logThread.log("ProcessSTAT\n");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        lastMsg = 1;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(pop3MessageList.size()) + " " + String.valueOf(totalMailSize));
    }

    private int processLIST(String buf) {
        int msgId = 0;
        String arguments = getParam(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        logThread.log("ProcessLIST " + msgId + "\n");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        if (msgId > pop3MessageList.size())
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No such message, only " + pop3MessageList.size() + " messages in maildrop");
        if (msgId > 0) {
            POP3Message message = pop3MessageList.get(msgId);
            if (message.getStatus() == POP3_MSG_STATUS_DELETED)
                return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "This message has been deleted");
            else
                return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(msgId) + " " + message.getSize());
        } else {
            sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);

            for (int i = 0; i < pop3MessageList.size(); i++) {
                if (pop3MessageList.get(i).getStatus() != POP3_MSG_STATUS_DELETED)
                    sendResponse(String.valueOf(i + 1) + " " + pop3MessageList.get(i).getSize() + "\r\n");
            }
            sendResponse(".\r\n");
        }
        return 0;
    }

    private int processRETR(String buf) {
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        int msgId = 0;
        String arguments = getParam(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No arguments");
        logThread.log("ProcessRETR " + msgId + "\n");
        if (msgId > pop3MessageList.size())
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Invalid message number");
        POP3Message message = pop3MessageList.get(msgId - 1);
        if (message.getStatus() == POP3Defines.POP3_MSG_STATUS_DELETED)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "This message has been deleted");
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(message.getSize()) + " octets");
        sendMessageFile(message.getFile());
        sendResponse("\r\n.\r\n");
        if (msgId > lastMsg)
            lastMsg = msgId;
        return 0;
    }

    private int processDELE(String buf) {
        int msgId = 0;
        String arguments = getParam(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No arguments");
        logThread.log("ProcessDELE " + msgId + "\n");
        if (state != POP3_STATE_TRANSACTION || msgId > pop3MessageList.size())
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        pop3MessageList.get(msgId - 1).delete();
        if (msgId > lastMsg)
            lastMsg = msgId;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processNOOP() {
        logThread.log("ProcessNOOP");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processLAST() {
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        logThread.log("ProcessLAST\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(lastMsg));
    }

    private int processRSET() {
        logThread.log("ProcessRSET");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        pop3MessageList.forEach(POP3Message::reset);
        lastMsg = 0;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processTOP(String buf) {
        logThread.log("ProcessTOP");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        String[] parts = buf.split(SPLITTER);
        int msgId = Integer.valueOf(parts[1]);
        int lineNumber = Integer.valueOf(parts[2]);
        POP3Message message = pop3MessageList.get(msgId - 1);
        if (message.getStatus() == POP3_MSG_STATUS_DELETED)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "This message has been deleted");
        String header = "";
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(message.getFile()))) {
            int c, lastC = 0;
            boolean crlf = false;
            while ((c = reader.read()) != -1) {
                header += ((char) c) == '\n' ? "\r\n" : (char) c;
                if (c == '\r' && crlf) {
                    reader.read();
                    break;
                }
                crlf = (lastC == '\r' && c == '\n');
                lastC = c;
            }
            lastC = 0;
            for (int lineId = 0; lineId < lineNumber; lineId++) {
                String line = "";
                while ((c = reader.read()) != -1 || (lastC == '\r' && c == '\n')) {
                    line += ((char) c) == '\n' ? "\r\n" : (char) c;
                    lastC = c;
                }
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
        sendResponse(header);
        sendResponse("\r\n");
        for (String line : lines) {
            sendResponse(line);
        }
        sendResponse("\r\n.\r\n");
        return POP3_DEFAULT_AFFIRMATIVE_RESPONSE;
    }

    private boolean login(String userName, String userPassword) {
        logThread.log("Login: ");
        logThread.log("user= [" + this.userName + "] password = [" + password + "]\n");
        String passPath = USERS_DOMAIN + File.separator + userName + File.separator + PASS_FILE;
        File passFile = new File(passPath);
        logThread.log("Pwd file: " + passPath + "\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
            String filePassword = "";
            int c;
            while ((c = reader.read()) != -1)
                filePassword += (char) c;
            if (filePassword.equals(userPassword)) {
                logThread.log("Password ok\n");
                state = POP3_STATE_TRANSACTION;
                userHome = new File(USERS_DOMAIN + File.separator + this.userName);
                lockMailDrop();
                return true;
            }
        } catch (FileNotFoundException e) {
            logThread.log("Password file is missing!");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateMails() {
        logThread.log("Updating mails\n");
        if (state != POP3_STATE_UPDATE) {
            logThread.log("Called update but state is not POP3_STATE_UPDATE (" + POP3_STATE_UPDATE + ")\n");
            return;
        }
        for (POP3Message message : pop3MessageList) {
            if (message.getStatus() == POP3_MSG_STATUS_DELETED)
                message.getFile().delete();
        }
    }

    private int sendMessageFile(File messageFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(messageFile))) {
            String content = "";
            int c;
            while ((c = reader.read()) != -1)
                content += ((char) c) == '\n' ? "\r\n" : (char) c;
            socConnection.getOutputStream().write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void lockMailDrop() {
        logThread.log("Locking maildrop");
        if (!userHome.isDirectory()) {
            return;
        }
        File[] files = userHome.listFiles();
        if (files.length > 1)
            for (File file : files) {
                String fileName = file.getName();
                String fileExt = fileName.substring(fileName.length() - 3, fileName.length());
                if (file.isFile() && fileExt.equals("txt")) {
                    pop3MessageList.add(new POP3Message(POP3_MSG_STATUS_INITIAL, file.length(), file));
                    totalMailSize += file.length();
                }
            }
        else logThread.log("No messages in " + userHome.getPath());
    }

}
