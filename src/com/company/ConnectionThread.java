package com.company;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by ZloiY on 02-Feb-17.
 */
public class ConnectionThread implements Runnable, POP3Defines {
    private Thread thread;
    private Socket clientSock;
    ConnectionThread(Socket clientSocket){
        thread = new Thread();
        clientSock = clientSocket;
    }
    public void run(){
        POP3Session session = new POP3Session(clientSock);
        session.sendResponse(POP3_WELCOME_RESPONSE);
        try {
            while (session.getSocConnection().getInputStream().available() != 0){
                if(-1==session.processSession(session.getSocConnection().getInputStream().toString())){
                    System.out.println("Connection thread closing...\n");
                    return;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return;
    }

    public Thread getThread(){return thread;}
}
