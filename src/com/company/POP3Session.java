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
    private int m_nState;
    private int m_nLastMsg;
    private File m_szUserHome;
    private String m_szUserName;
    private String m_szPassword;
    private int m_nTotalMailCount, m_dwTotalMailSize;
    private Socket m_socConnection;
    private List<POP3Message> m_pPop3MessageList;

    public POP3Session(Socket client_soc) {
        m_nState = POP3_STATE_AUTHORIZATION;
        m_socConnection = client_soc;
        m_pPop3MessageList = new ArrayList<>();
        m_nLastMsg = 0;
    }

    public int sendResponse(String buf) {
        System.out.println("Direct Sending: " + buf);
        try {
            m_socConnection.getOutputStream().write(buf.getBytes());
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
            m_socConnection.getOutputStream().write(buf.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nResponseType;
    }

    public Socket getM_socConnection() {
        return m_socConnection;
    }

    public int sendResponse(int nResponseType) {
        return sendResponse(nResponseType, null);
    }

    private String getArguments(String buf) {
        int spaceId = buf.indexOf(' ');
        if (spaceId > 0 && spaceId < 5)
            return buf.substring(spaceId + 1, buf.length() - 2);
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
                System.out.println("God damned russian hackers");
        }
        return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
    }

    private int processUSER(String buf) {
        System.out.println("ProcessUSER\n");
        if (m_nState != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        String arguments = getArguments(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify the username");
        m_szUserName = arguments;
        File connectingUserHome = new File(USERS_DOMAIN + File.pathSeparator + m_szUserName);
        //System.out.println(m_szUserHome);
        if (!connectingUserHome.exists()) {
            System.out.println("User " + m_szUserName + " 's Home '" + connectingUserHome.getAbsolutePath() + "' not found\n");
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong username");
        }
        System.out.println("OK User " + m_szUserHome + " Home " + connectingUserHome.getAbsolutePath() + "\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processPASS(String buf) {
        System.out.println("ProcessPASS\n");
        if (m_nState != POP3_STATE_AUTHORIZATION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        if (m_szUserName.length() < 1)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You did not introduce yourself");
        String arguments = getArguments(buf);
        if (arguments == null)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "You should specify a password");
        m_szPassword = arguments;
        if (login(m_szUserName, m_szPassword))
            return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Now you can check your mail");
        else return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Wrong password");
    }

    private int processQUIT(String buf) {
        System.out.println("ProcessQUIT\n");
        if (m_nState == POP3_STATE_TRANSACTION)
            m_nState = POP3_STATE_UPDATE;
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, "Goodbye");
        updateMails();
        return -1;
    }

    private int processSTAT() {
        System.out.println("ProcessSTAT\n");
        if (m_nState != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        m_nLastMsg = 1;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(m_nTotalMailCount) + " " + String.valueOf(m_dwTotalMailSize));
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
        if (m_nState != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        if (msgId > m_nTotalMailCount)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "No such message, only " + m_nTotalMailCount + " messages in maildrop");
        if (msgId > 0) {
            POP3Message message = m_pPop3MessageList.get(msgId);
            if (message.getStatus() == POP3_MSG_STATUS_DELETED)
                return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "This message has been deleted");
            else
                return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(msgId) + " " + message.getSize());
        } else {
            sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);

            for (int i = 0; i < m_pPop3MessageList.size(); i++) {
                if (m_pPop3MessageList.get(i).getStatus() != POP3_MSG_STATUS_DELETED)
                    sendResponse(String.valueOf(i + 1) + " " + m_pPop3MessageList.get(i).getSize() + "\r\n");
            }
            sendResponse(".\r\n");
        }
        return 0;
    }

    private int processRETR(String buf) {
        if (m_nState != POP3_STATE_TRANSACTION)
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
        if (msgId > m_nTotalMailCount)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "Invalid message number");
        POP3Message message = m_pPop3MessageList.get(msgId - 1);
        if (message.getStatus() == POP3Defines.POP3_MSG_STATUS_DELETED)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE, "This message has been deleted");
        sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(message.getSize()) + " octets");
        sendMessageFile(message.getPath());
        sendResponse("\r\n.\r\n");
        if (msgId > m_nLastMsg)
            m_nLastMsg = msgId;
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
        if (m_nState != POP3_STATE_TRANSACTION || msgId > m_nTotalMailCount)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        m_pPop3MessageList.get(msgId - 1).delete();
        if (msgId > m_nLastMsg)
            m_nLastMsg = msgId;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processNOOP() {
        System.out.println("ProcessNOOP");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private int processLAST() {
        if (m_nState != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        System.out.println("ProcessLAST\n");
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE, String.valueOf(m_nLastMsg));
    }

    private int processRSET() {
        System.out.println("ProcessRSET");
        if (m_nState != POP3_STATE_TRANSACTION)
            return sendResponse(POP3_DEFAULT_NEGATIVE_RESPONSE);
        m_pPop3MessageList.forEach(POP3Message::reset);
        m_nLastMsg = 0;
        return sendResponse(POP3_DEFAULT_AFFIRMATIVE_RESPONSE);
    }

    private boolean login(String userName, String userPassword) {
        System.out.println("Login: ");
        System.out.println("user= [" + m_szUserName + "] password = [" + m_szPassword + "]\n");
        String passPath = USERS_DOMAIN + File.pathSeparator + userName + File.pathSeparator + PASS_FILE;
        File passFile = new File(passPath);
        System.out.println("Pwd file: " + passPath + "\n");
        try (BufferedReader reader = new BufferedReader(new FileReader(passFile))) {
            String filePassword = "";
            int c;
            while ((c = reader.read()) != -1)
                filePassword += (char) c;
            if (filePassword.equals(userPassword)) {
                System.out.println("Password ok\n");
                m_nState = POP3_STATE_TRANSACTION;
                m_szUserHome = new File(USERS_DOMAIN + File.pathSeparator + m_szUserName);
                //LockMailDrop();
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
        if (m_nState != POP3_STATE_UPDATE) {
            System.out.println("Called update but state is not POP3_STATE_UPDATE (" + POP3_STATE_UPDATE + ")\n");
            return;
        }
        for (POP3Message message : m_pPop3MessageList){
            if (message.getStatus() == POP3_MSG_STATUS_DELETED)
                new File(message.getPath()).delete();
        }
    }

    private int sendMessageFile(String szFilePath) {
        File contentFile = new File(szFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(contentFile))) {
            String content = "";
            int c;
            while ((c = reader.read()) != -1)
                content += ((char) c) == '\n' ? "\r\n" : (char) c;
            getM_socConnection().getOutputStream().write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
