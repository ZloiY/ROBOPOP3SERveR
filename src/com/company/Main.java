package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main implements POP3Defines{

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(POP3_PORT);
            System.out.println("Server is online and waiting new clients.\n");
            acceptConnection(serverSocket);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void acceptConnection(ServerSocket serverSocket){
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    ConnectionThread connectionThread = new ConnectionThread(socket);
                    if (connectionThread.getThread()==null) return;
                    connectionThread.getThread().start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
