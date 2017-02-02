package com.company;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
    private Socket m_socConnection;
    private List<POP3Message> m_pPop3MessageList;

    public POP3Session(Socket client_soc){
        m_nState = POP3_STATE_AUTHORIZATION;
        m_socConnection = client_soc;
        m_pPop3MessageList = new ArrayList<>();
        m_nLastMsg = 0;
    }

    private int sendRespones(String buf){
        System.out.println("Direct Sending: "+buf);
        try {
            m_socConnection.getOutputStream().write(buf.getBytes());
        }catch (IOException e){
            e.printStackTrace();
        }
        return 0;
    }

    private int sendRespones(int nResponseType, String message){
        String buf="";
        if (nResponseType == POP3_DEFAULT_AFFERMATIVE_RESPONSE){
            if(message.length()!=0)
                buf = "+OK "+message+" \r\n";
            else buf = "+OK Action performed\r\n";
        }
        else if(nResponseType == POP3_DEFAULT_NEGATIVE_RESPONSE)
                buf="-ERR An error occured\r\n";
        else if(nResponseType == POP3_WELCOME_RESPONSE)
                buf="+OK "+APP_TITLE+" "+APP_VERSION+" POP3 Server ready on \r\n";
        else if (nResponseType == POP3_WELCOME_RESPONSE)
                buf="+OK "+m_nTotalMailCount+" "+m_dwTotalMailSize+"\r\n";
        System.out.println("Sending: "+buf);
        try{
            m_socConnection.getOutputStream().write(message.getBytes());
        }catch (IOException e){
            e.printStackTrace();
        }
        return nResponseType;
    }

    private int processSession (String buf){
        switch (buf){
            case "USER": return processUSER(buf);
            case "PASS": return processPASS(buf);
            case "QUIT": return processQUIT(buf);
            case "STAT": return processSTAT(buf);
            case "LIST": return processLIST(buf);
            case "RETR": return processRETR(buf);
            case "DELE": return processDELE(buf);
            case "NOOP": return processNOOP(buf);
            case "LAST": return processLAST(buf);
            case "RSET": return processRSET(buf);
            case "RPOP": return processRPOP(buf);
            case "TOP": return processTOP(buf);
            default:
                System.out.println("Fucking russian hackers");
        }
        return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
    }

    private int processUSER(String buf){
        System.out.println("ProcessUSER\n");
        m_szUserName = buf;
        //System.out.println(m_szUserHome);
        if(!new File(buf).exists()){
            System.out.println("User "+m_szUserName+" 's Home '"+m_szUserHome+"' not found\n");
            return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
        }
        System.out.println("OK User "+m_szUserHome+" Home "+m_szUserHome+"\n");
        if(m_nState != POP3_STATE_AUTHORIZATION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        return sendRespones(POP3_DEFAULT_AFFERMATIVE_RESPONSE,"");
    }

    private int processPASS(String buf){
        System.out.println("ProcessPASS\n");
        m_szPassword = buf;
        if(m_nState!=POP3_STATE_AUTHORIZATION || m_szUserName.length()<1) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        if (login(m_szUserName, m_szPassword)) return sendRespones(POP3_DEFAULT_AFFERMATIVE_RESPONSE,"");
        else return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
    }

    private int processQUIT(String buf){
        System.out.println("ProcessQUIT\n");
        if(m_nState==POP3_STATE_TRANSACTION)
            m_nState=POP3_STATE_UPDATE;
        sendRespones(POP3_DEFAULT_AFFERMATIVE_RESPONSE, "Goodbye");
        updateMails();
        return -1;
    }

    private int processSTAT(String buf){
        System.out.println("ProcessSTAT\n");
        if(m_nState!=POP3_STATE_TRANSACTION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
        m_nLastMsg=1;
        return sendRespones(POP3_STAT_RESPONSE, "");
    }

    private int processLIST(String buf){
        int msg_id = buf.hashCode();
        System.out.println("ProcessLIST "+msg_id+"\n");
        if(m_nState!=POP3_STATE_TRANSACTION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        if(msg_id>0){
            String resp;
            resp = "+OK "+msg_id+" "+m_pPop3MessageList.get(msg_id).getSize();
            return sendRespones(resp);
        }else{
            sendRespones("+OK \r\n");
            for (int i=0; i < m_pPop3MessageList.size(); i++){
                String resp;
                resp = ""+i+1+" "+m_pPop3MessageList.get(i).getSize()+"\r\n";
                sendRespones(resp);
            }
            sendRespones(".\r\n");
        }
        return 0;
    }

    private int processRETR(String buf){
        int msg_id = buf.hashCode();
        System.out.println("ProcessRETR "+msg_id+"\n");
        if (m_nState!=POP3_STATE_TRANSACTION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
        if (msg_id>m_pPop3MessageList.size()) return sendRespones("-ERR Invalid message number\r\n");
        String resp = "+OK "+m_pPop3MessageList.get(msg_id-1).getSize()+" octets\r\n";
        sendRespones(resp);
        //sendMessageFile(m_pPop3MessageList.get(msg_id-1).getPath());
        sendRespones("\r\n.\r\n");
        return 0;
    }

    private int processDELE(String buf){
        int msg_id = buf.hashCode();
        System.out.println("ProcessDELE "+msg_id+"\n");
        if(m_nState != POP3_STATE_TRANSACTION || msg_id > m_nTotalMailCount) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        m_pPop3MessageList.get(msg_id-1).delete();
        return sendRespones(POP3_DEFAULT_AFFERMATIVE_RESPONSE,"");
    }

    private int processNOOP(String buf){
        System.out.println("ProcessNOOP");
        return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
    }

    private int processLAST(String buf){
        if(m_nState!=POP3_STATE_TRANSACTION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        System.out.println("ProcessLAST\n");
        String resp = "+OK "+m_nLastMsg+"\r\n";
        return sendRespones(resp);
    }

    private int processRSET(String buf){
        System.out.println("ProcessRSET");
        if(m_nState!=POP3_STATE_TRANSACTION) return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
        for (int i=0; i < m_pPop3MessageList.size(); i++) m_pPop3MessageList.get(i).reset();
        return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE,"");
    }

    private int processRPOP(String buf){
        System.out.println("ProcessRPOP\n");
        return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
    }

    private int processTOP(String buf){
        System.out.println("ProcessTOP");
        return sendRespones(POP3_DEFAULT_NEGATIVE_RESPONSE, "");
    }

    private boolean login(String szName, String szPassword){
        System.out.println("Login: ");
        System.out.println("user= ["+m_szUserName+"] password = ["+m_szPassword+"]\n");
        String lpPwdFile, lpUserHome;
        lpPwdFile = "\\"+szName+"\\"+szPassword;
        System.out.println("Pwd file: "+lpPwdFile+"\n");
        if(new File(lpPwdFile).exists()){
            System.out.println("Password ok\n");
            m_nState = POP3_STATE_TRANSACTION;
            lpUserHome = "\\"+szName;
            setHomePath(lpUserHome);
            //LockMailDrop();
            return true;
        }
        return false;
    }

    private boolean setHomePath(String lpPath){
        m_szUserHome = lpPath;
        return true;
    }

   private void updateMails(){
       System.out.println("Updating mails\n");
       if(m_nState!=POP3_STATE_UPDATE){
           System.out.println("Called update but state is nt POP3_STATE_UPDATE ("+POP3_STATE_UPDATE+")\n");
           return;
       }
       for (int i=0; i < m_pPop3MessageList.size(); i++)
           if(m_pPop3MessageList.get(i).getStatus() > 0){
               System.out.println("Delete file "+m_pPop3MessageList.get(i).getPath()+"\n");
               boolean del = new File(m_pPop3MessageList.get(i).getPath()).delete();
           }
   }

   private int sendMessageFile(String szFilePath){
    return 0;
   }

}
