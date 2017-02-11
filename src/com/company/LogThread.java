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
<<<<<<< HEAD
     * Проверяет, приостановлен ли поток логирования.
=======
     * Если стоит флаг паузы, то приостанавливаем процесс логирования.
>>>>>>> 8d1db5a4b38e85023b03c1806d595cfcd51c2de9
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
<<<<<<< HEAD
     * Приостанавливает поток логирования.
=======
     * Ставим флаг паузы.
     * @throws InterruptedException
>>>>>>> 8d1db5a4b38e85023b03c1806d595cfcd51c2de9
     */
    public void pauseThread(){
        pauseThreadFlag = true;
    }

    /**
<<<<<<< HEAD
     * Возобновляет работу потока логирования.
=======
     * Снимаем флаг паузы.
>>>>>>> 8d1db5a4b38e85023b03c1806d595cfcd51c2de9
     */
    public void resumeThread() {
        synchronized (MONITOR) {
            pauseThreadFlag = false;
            MONITOR.notify();
        }
    }

    /**
<<<<<<< HEAD
     * Устанавливает флаг для закрытия потока логирования.
=======
     * Устанавливаем флаг закрытия потока.
>>>>>>> 8d1db5a4b38e85023b03c1806d595cfcd51c2de9
     */
    public void closeThread() {
        closeThreadFlag = true;
    }
}
