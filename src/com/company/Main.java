package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main implements POP3Defines {
    private static LogThread logThread;
    private static List<Socket> socketList;
    public static void main(String[] args) {
        socketList = new ArrayList<Socket>();
        try {
            ServerSocket serverSocket = new ServerSocket(POP3_PORT);
            logThread = new LogThread();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    logThread.log("SIGINT Shutting down");
                    logThread.closeThread();
                    try {
                        if (!socketList.isEmpty()){
                            for (Socket socket : socketList)
                                socket.close();
                        }
                        serverSocket.close();
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

    private static void acceptConnection(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                socketList.add(socket);
                ConnectionThread connectionThread = new ConnectionThread(socket, logThread);
                connectionThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
