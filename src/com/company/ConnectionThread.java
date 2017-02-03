package com.company;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by ZloiY on 02-Feb-17.
 */
public class ConnectionThread extends Thread implements POP3Defines {
    private Socket clientSock;

    ConnectionThread(Socket clientSocket) {
        clientSock = clientSocket;
    }

    public void run() {
        POP3Session session = new POP3Session(clientSock);
        session.sendResponse(POP3_WELCOME_RESPONSE);
        System.out.println("thread start");
        try {
            while (true/*clientSock.getInputStream().available() != 0*/) {
                System.out.println("while");
                BufferedInputStream stream = new BufferedInputStream(clientSock.getInputStream());
                String msg = "";
                int c;
                int lastC = 0;
                while ((c = stream.read()) != 13 && lastC != 10) {
                    msg += (char) c;
                    lastC = c;
                }
                System.out.println(msg.length() == 0 ? "empty" : msg);
                if (-1 == session.processSession(msg)) {
                    System.out.println("Connection thread closing...\n");
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}
