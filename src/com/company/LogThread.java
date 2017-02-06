package com.company;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Terenfear on 06.02.2017.
 */
public class LogThread extends Thread implements POP3Defines {
    private ConcurrentLinkedQueue<String> queue;
    private final Object GUI_INITIALIZATION_MONITOR = new Object();
    private File logFile;
    private boolean pauseThreadFlag = false;
    private boolean closeThreadFlag = false;

    public LogThread() {
        super();
        queue = new ConcurrentLinkedQueue<>();
        logFile = new File(LOG_FILE);
    }

    @Override
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

    public void log(String msg) {
        queue.add(msg);
        resumeThread();
    }

    private void checkForPaused() {
        synchronized (GUI_INITIALIZATION_MONITOR) {
            while (pauseThreadFlag) {
                try {
                    GUI_INITIALIZATION_MONITOR.wait();
                } catch (Exception e) {
                }
            }
        }
    }

    public void pauseThread() throws InterruptedException {
        pauseThreadFlag = true;
    }

    public void resumeThread() {
        synchronized (GUI_INITIALIZATION_MONITOR) {
            pauseThreadFlag = false;
            GUI_INITIALIZATION_MONITOR.notify();
        }
    }

    public void closeThread() {
        closeThreadFlag = true;
    }
}
