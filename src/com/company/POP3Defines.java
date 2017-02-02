package com.company;

import java.io.File;

/**
 * Created by ZloiY on 01-Feb-17.
 */
public interface POP3Defines {
    int POP3_PORT = 110;
    int POP3_DEFAULT_NEGATIVE_RESPONSE = 0;
    int POP3_DEFAULT_AFFIRMATIVE_RESPONSE = 1;
    int POP3_WELCOME_RESPONSE = 2;

    int POP3_STATE_AUTHORIZATION = 3;
    int POP3_STATE_TRANSACTION = 4;
    int POP3_STATE_UPDATE = 5;

    int POP3_STAT_RESPONSE = 16;
    int POP3_MSG_STATUS_UNDEFINED = 0;
    int POP3_MSG_STATUS_NEW = 1;
    int POP3_MSG_STATUS_READ = 2;
    int POP3_MSG_STATUS_REPLIED = 4;
    int POP3_MSG_STATUS_DELETED = 8;
    int POP3_MSG_STATUS_CUSTOM = 16;
    String APP_TITLE = "pop3server";
    String APP_VERSION = "1.0";

    String USERS_DOMAIN = File.pathSeparator + "Users";
    String PASS_FILE = "pass.txt";
}
