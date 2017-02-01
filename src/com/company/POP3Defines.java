package com.company;

/**
 * Created by ZloiY on 01-Feb-17.
 */
public interface POP3Defines {
    public static int POP3_PORT = 110;
    public static int POP3_DEFAULT_NEGATIVE_RESPONSE = 0;
    public static int POP3_DEFAULT_AFFERMATIVE_RESPONSE = 1;
    public static int POP3_WELCOME_RESPONSE = 2;
    public static int POP3_STATE_AUTHORIZATION = 1;
    public static int POP3_STATE_TRANSACTION = 2;
    public static int POP3_STATE_UPDATE = 4;
    public static int POP3_STAT_RESPONSE = 16;
    public static int POP3_MSG_STATUS_UNDEFINED = 0;
    public static int POP3_MSG_STATUS_NEW = 1;
    public static int POP3_MSG_STATUS_READ = 2;
    public static int POP3_MSG_STATUS_REPLIED = 4;
    public static int POP3_MSG_STATUS_DELETED = 8;
    public static int POP3_MSG_STATUS_CUSTOM = 16;
    public static String APP_TITLE = "pop3server";
    public static String APP_VERSION = "1.0";

}
