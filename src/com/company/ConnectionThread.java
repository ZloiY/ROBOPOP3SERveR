package com.company;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by ZloiY on 02-Feb-17.
 */
public class ConnectionThread extends Thread implements POP3Defines {
    private Socket clientSock;
    private LogThread logThread;

    ConnectionThread(Socket clientSocket, LogThread logThread) {
        clientSock = clientSocket;
        this.logThread = logThread;
    }

    public void run() {
        POP3Session session = new POP3Session(clientSock, logThread);
        session.sendResponse(POP3_WELCOME_RESPONSE);
        try {
            while (true) {
                BufferedInputStream stream = new BufferedInputStream(clientSock.getInputStream());
                String msg = "";
                int c;
                int lastC = 0;
                while ((c = stream.read()) != 13 && lastC != 10) {
                    msg += (char) c;
                    lastC = c;
                }
                logThread.log(msg.length() == 0 ? "empty" : msg);
                if (-1 == session.processSession(msg)) {
                    logThread.log("Connection thread closing...\n");
                    clientSock.close();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}
