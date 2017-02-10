package com.company;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Поток для работы с соединением отдельного клиента.
 * <p>Выполняет прием данных от клиента с помощью {@link BufferedInputStream}, поддерживает сессию (работа с {@link POP3Session}) и обеспечивает связь сессии с потоком для логгирования диалога клиент-сервер {@link LogThread}.
 */

public class ConnectionThread extends Thread implements POP3Defines {
    private Socket clientSock;
    private LogThread logThread;

    /**
     * Конструктор класса.
     * @param clientSocket сокет клиента
     * @param logThread поток для логгирования диалога клиент-сервер
     * @see Socket
     * @see LogThread
     */
    ConnectionThread(Socket clientSocket, LogThread logThread) {
        clientSock = clientSocket;
        this.logThread = logThread;
    }

    /**
     * Открываем сессию с клиентом.
     */
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
                if (session.processSession(msg) == POP3_SESSION_QUITTED) {
                    logThread.log("Connection thread closing...\n");
                    clientSock.close();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
