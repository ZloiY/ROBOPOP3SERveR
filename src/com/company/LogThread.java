package com.company;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogThread extends Thread implements POP3Defines {
    private ConcurrentLinkedQueue<String> queue;
    private final Object MONITOR = new Object();
    private File logFile;
    private boolean pauseThreadFlag = false;
    private boolean closeThreadFlag = false;

    /**
     * Конструктор логгера, создайм файл в который будет записываться лог,
     * создаём очередь для записываемых сообщений
     */
    public LogThread() {
        super();
        queue = new ConcurrentLinkedQueue<>();
        logFile = new File(LOG_FILE);
    }

    /**
     * Выводим получаемую логгером информацию в консоль и записываем её в файл
     */
    public void run() {
        String loggingMsg = "";
        try (PrintStream stream = new PrintStream(new FileOutputStream(logFile, false))) {
            while (!closeThreadFlag) {
                checkForPaused();
                while ((loggingMsg = queue.poll()) != null) {
                    System.out.println(loggingMsg);
                    stream.println(loggingMsg);
                }
                pauseThread();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Добавляет полученное сообщение в очередь.
     * @param msg сообщение для логгирования
     */
    public void log(String msg) {
        queue.add(msg);
        resumeThread();
    }

    /**
     * Если стоит флаг паузы, то приостанавливаем процесс логирования.
     */
    private void checkForPaused() {
        synchronized (MONITOR) {
            while (pauseThreadFlag) {
                try {
                    MONITOR.wait();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Ставим флаг паузы.
     * @throws InterruptedException
     */
    public void pauseThread() throws InterruptedException {
        pauseThreadFlag = true;
    }

    /**
     * Снимаем флаг паузы.
     */
    public void resumeThread() {
        synchronized (MONITOR) {
            pauseThreadFlag = false;
            MONITOR.notify();
        }
    }

    /**
     * Устанавливаем флаг закрытия потока.
     */
    public void closeThread() {
        closeThreadFlag = true;
    }
}
