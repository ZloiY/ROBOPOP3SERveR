package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс, используемый для запуска сервера, настройки сокетов и приема соединиений.
 */
public class Main implements POP3Defines {
    private static LogThread logThread;
    private static List<ConnectionThread> threadList;

    /**
     * Точка входа серверного преложения.
     * @param args аргументы командной строки, переданные при запуске
     */
    public static void main(String[] args) {
        threadList = new ArrayList<>();
        try {
            ServerSocket serverSocket = new ServerSocket(POP3_PORT);
            logThread = new LogThread();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    logThread.log("SIGINT Shutting down");
                    try {
                        if (!threadList.isEmpty()){
                            for (ConnectionThread thread : threadList)
                                thread.endSession();
                        }
                        serverSocket.close();
                        logThread.closeThread();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            logThread.start();
            logThread.log("Server is online and waiting new clients.\n");
            acceptConnection(serverSocket);
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Принимает новое соединение, запускаетс новый поток для работы
     * с клиентом.
     * @param serverSocket сокет сервера
     * @see ServerSocket
     */
    private static void acceptConnection(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                ConnectionThread connectionThread = new ConnectionThread(socket, logThread);
                connectionThread.start();
                threadList.add(connectionThread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
