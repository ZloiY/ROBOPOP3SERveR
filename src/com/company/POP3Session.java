package com.company;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by ZloiY on 01-Feb-17.
 */
public class POP3Session implements POP3Defines {
    private int m_nState;
    private int m_nLastMsg;
    private String m_szUserHome;
    private String m_szUserName;
    private String m_szPassword;
    private int m_nTotalMailCount, m_dwTotalMailSize;
    private POP3Message m_pPop3MessageHead;
    private Socket m_socConnection;

    public POP3Session(Socket client_soc){
        m_nState = POP3_STATE_AUTHORIZATION;
        m_socConnection = client_soc;
        m_pPop3MessageHead = new POP3Message();
        m_nLastMsg = 0;
    }

    private int SendRespones(String buf){
        System.out.println("Direct Sending: "+buf);
        try {
            m_socConnection.getOutputStream().write(buf.getBytes());
        }catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    private int SendRespones(int nResponseType, String message){
        if (nResponseType == POP3_DEFAULT_AFFERMATIVE_RESPONSE){
            if(message.length()!=0)
                System.out.println("+OK " + message+"\r\n");
            else System.out.println("+OK Actions performed\r\n");
        }
        else if(nResponseType == POP3_DEFAULT_NEGATIVE_RESPONSE)
            System.out.println("-ERR An error occured\r\n");
        else if(nResponseType == POP3_WELCOME_RESPONSE)
            System.out.println("+OK "+APP_TITLE+" "+APP_VERSION+" POP3 Server ready on \r\n");
        else if (nResponseType == POP3_WELCOME_RESPONSE)
            System.out.println("+OK "+m_nTotalMailCount+" "+m_dwTotalMailSize+"\r\n");
        System.out.println("Sending: "+message);
        return nResponseType;
    }

    private int ProcessUSER(String message){
        System.out.println("ProcessUSER\n");
        m_szUserName = message;
        //System.out.println(m_szUserHome);
        return SendRespones(POP3_DEFAULT_AFFERMATIVE_RESPONSE,"");
    }
}
