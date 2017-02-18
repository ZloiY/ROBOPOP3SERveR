package com.company;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Поток для логирования работы сервера. Принимает сообщения от потоков,
 * работающих с сессиями клиентов, и выводит их в окно терминала, а
 * также заносит их в лог-файл.
 * @see ConnectionThread
 * @see POP3Session
 * @see #LOG_FILE
 */

public class LogThread extends Thread implements POP3Defines {
    private ConcurrentLinkedQueue<String> queue;
    private final Object MONITOR = new Object();
    private File logFile;
    private boolean pauseThreadFlag = false;
    private boolean closeThreadFlag = false;

    /**
     * Конструктор потока логирования. Создает файл, в который будет
     * записываться лог, а также создает очередь для записываемых сообщений.
     */
    public LogThread() {
        super();
        queue = new ConcurrentLinkedQueue<>();
        logFile = new File(LOG_FILE);
    }

    /**
     * Выводит получаемую логгером информацию в консоль и записывает её
     * в файл {@code LOG_FILE}, значение которого определено в {@link POP3Defines}.
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Добавляет полученное сообщение в очередь.
     * @param msg сообщение для логирования
     */
    public void log(String msg) {
        queue.add(msg);
        resumeThread();
    }

    /**
     * Проверяет, приостановлен ли поток логирования.
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
     * Приостанавливает поток логирования.
     */
    public void pauseThread(){
        pauseThreadFlag = true;
    }

    /**
     * Возобновляет работу потока логирования.
     */
    public void resumeThread() {
        synchronized (MONITOR) {
            pauseThreadFlag = false;
            MONITOR.notify();
        }
    }

    /**
     * Устанавливает флаг для закрытия потока логирования.
     */
    public void closeThread() {
        closeThreadFlag = true;
    }
}
