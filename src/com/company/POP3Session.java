package com.company;

import com.sun.istack.internal.Nullable;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZloiY on 01-Feb-17.
 */
public class POP3Session implements POP3Defines {
    private int state;
    private int lastMsg;
    private File userHome;
    private String userName;
    private String password;
    private long totalMailSize;
    private Socket socConnection;
    private List<POP3Message> pop3MessageList;

    public POP3Session(Socket client_soc) {
        state = POP3_STATE_AUTHORIZATION;
        socConnection = client_soc;
        pop3MessageList = new ArrayList<>();
        lastMsg = 0;
        totalMailSize = 0;
    }

    public int sendResponse(String buf) {
        System.out.println("Direct Sending: " + buf);
        try {
            socConnection.getOutputStream().write(buf.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

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
        System.out.println("Sending: " + buf);
        try {
            socConnection.getOutputStream().write(buf.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nResponseType;
    }

    public Socket getSocConnection() {
        return socConnection;
    }

    public int sendResponse(int nResponseType) {
        return sendResponse(nResponseType, null);
    }

    private String getArguments(String buf) {
        int spaceId = buf.indexOf(' ');
        if (spaceId > 0 && spaceId < 5)
            return buf.substring(spaceId + 1, buf.length());
        else return null;
    }

    public int processSession(String buf) {
        String cmd = buf.substring(0, 4);
        switch (cmd) {
            case "USER":
                return processUSER(buf);
            case "PASS":
                return processPASS(buf);
            case "QUIT":
                return processQUIT(buf);
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
                System.out.println("God damned russian hackers: " + buf);
        }
        return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
    }

    private int processUSER(String buf) {
        System.out.println("ProcessUSER\n");
        if (state != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        String arguments = getArguments(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify the username");
        userName = arguments;
        File connectingUserHome = new File(USERS_DOMAIN + File.separator + userName);
        //System.out.println(userHome);
        if (!connectingUserHome.exists()) {
            System.out.println("User " + userName + " 's Home '" + connectingUserHome.getAbsolutePath() + "' not found\n");
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong username");
        }
        System.out.println("OK User " + userName + " Home " + connectingUserHome.getAbsolutePath() + "\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processPASS(String buf) {
        System.out.println("ProcessPASS\n");
        if (state != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        if (userName.length() < 1)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You did not introduce yourself");
        String arguments = getArguments(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify a password");
        password = arguments;
        if (login(userName, password))
            return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Now you can check your mail");
        else return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong password");
    }

    private int processQUIT(String buf) {
        System.out.println("ProcessQUIT\n");
        if (state == POP3_STATE_TRANSACTION)
            state = POP3_STATE_UPDATE;
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Goodbye");
        updateMails();
        return -1;
    }

    private int processSTAT() {
        System.out.println("ProcessSTAT\n");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        lastMsg = 1;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(pop3MessageList.size()) + " " + String.valueOf(totalMailSize));
    }

    private int processLIST(String buf) {
        int msgId = 0;
        String arguments = getArguments(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        System.out.println("ProcessLIST " + msgId + "\n");
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
        String arguments = getArguments(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No arguments");
        System.out.println("ProcessRETR " + msgId + "\n");
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
        String arguments = getArguments(buf);
        if (arguments != null) {
            try {
                msgId = Integer.valueOf(arguments);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No arguments");
        System.out.println("ProcessDELE " + msgId + "\n");
        if (state != POP3_STATE_TRANSACTION || msgId > pop3MessageList.size())
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        pop3MessageList.get(msgId - 1).delete();
        if (msgId > lastMsg)
            lastMsg = msgId;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processNOOP() {
        System.out.println("ProcessNOOP");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processLAST() {
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        System.out.println("ProcessLAST\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(lastMsg));
    }

    private int processRSET() {
        System.out.println("ProcessRSET");
        if (state != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        pop3MessageList.forEach(POP3Message::reset);
        lastMsg = 0;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private boolean login(String userName, String userPassword) {
        System.out.println("Login: ");
        System.out.println("user= [" + this.userName + "] password = [" + password + "]\n");
        String passPath = USERS_DOMAIN + File.separator + userName + File.separator + PASS_FILE;
        File passFile = new File(passPath);
        System.out.println("Pwd file: " + passPath + "\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
            String filePassword = "";
            int c;
            while ((c = reader.read()) != -1)
                filePassword += (char) c;
            if (filePassword.equals(userPassword)) {
                System.out.println("Password ok\n");
                state = POP3_STATE_TRANSACTION;
                userHome = new File(USERS_DOMAIN + File.separator + this.userName);
                lockMailDrop();
                return true;
            }
        } catch (FileNotFoundException e) {
            System.out.println("Password file is missing!");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void updateMails() {
        System.out.println("Updating mails\n");
        if (state != POP3_STATE_UPDATE) {
            System.out.println("Called update but state is not POP3_STATE_UPDATE (" + POP3_STATE_UPDATE + ")\n");
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
            getSocConnection().getOutputStream().write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void lockMailDrop() {
        System.out.println("Locking maildrop");
        if (!userHome.isDirectory()) {
            return;
        }
        File[] files = userHome.listFiles();
        if (files.length > 1)
            for (File file : files) {
                String fileName = file.getName();
                String fileExt = fileName.substring(fileName.length() - 3, fileName.length());
                if (file.isFile() && fileExt.equals("txt")){
                    pop3MessageList.add(new POP3Message(POP3_MSG_STATUS_INITIAL, file.getTotalSpace(), file));
                    totalMailSize += file.getTotalSpace();
                }
            }
        else System.out.println("No messages in " + userHome.getPath());
    }

}
