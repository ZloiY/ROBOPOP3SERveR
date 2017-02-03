package com.company;

import java.io.File;

/**
 * Created by ZloiY on 01-Feb-17.
 */
public class POP3Message implements POP3Defines {
    private File messageFile;
    private int status;
    private long size;

    public POP3Message() {
    }

    public POP3Message(int nStatus, long nSize, File messageFile) {
        status = nStatus;
        size = nSize;
        this.messageFile = messageFile;
    }

    public void setParams(int nStatus, long nSize, File messageFile) {
        status = nStatus;
        size = nSize;
        this.messageFile = messageFile;
    }

    public void setParams(POP3Message message) {
        status = message.getStatus();
        size = message.getSize();
        this.messageFile = message.getFile();
    }

    public void delete() {
        status = POP3_MSG_STATUS_DELETED;
    }

    public void reset() {
        status = POP3_MSG_STATUS_INITIAL;
    }

    public int getStatus() {
        return status;
    }

    public long getSize() {
        return size;
    }

    public File getFile() {
        return messageFile;
    }
}
