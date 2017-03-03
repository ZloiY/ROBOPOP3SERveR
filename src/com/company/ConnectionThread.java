package com.company;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Поток для работы с соединением отдельного клиента.
 * <p>Выполняет прием данных от клиента с помощью
 * {@link BufferedInputStream}, поддерживает сессию
 * (работа с {@link POP3Session}) и обеспечивает связь
 * сессии с потоком для логгирования диалога клиент-сервер
 * {@link LogThread}.
 */

public class ConnectionThread extends Thread implements POP3Defines {
    /**
     * Сокет клиента.
     */
    private Socket clientSock;
    /**
     * Поток логгирования сессии.
     */
    private LogThread logThread;
    /**
     * Флаг показывающий активен ли данный поток.
     */
    private boolean flagIsOnline;

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
        flagIsOnline = true;
    }

    public void run() {
        POP3Session session = new POP3Session(clientSock, logThread);
        session.sendResponse(POP3_WELCOME_RESPONSE);
        try {
            while (flagIsOnline) {
                BufferedInputStream stream = new BufferedInputStream(clientSock.getInputStream());
                String msg = "";
                int c;
                int lastC = 0;
                while ((c = stream.read()) != 13 && lastC != 10) {
                    msg += (char) c;
                    lastC = c;
                }
                logThread.log("Message from client: " + (msg.length() == 0 ? "empty" : msg));
                if (session.processSession(msg) == POP3_SESSION_QUITED) {
                    logThread.log("Connection thread closing...\n");
                    clientSock.close();
                    return;
                }
            }
            session.shutdown();
            clientSock.close();
            logThread.log("Connection thread closing...\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Завершает сессию.
     */
    public void endSession(){
        flagIsOnline = false;
    }
}
