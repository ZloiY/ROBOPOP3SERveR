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

    int POP3_STATE_AUTHORIZATION = 300;
    int POP3_STATE_TRANSACTION = 301;
    int POP3_STATE_UPDATE = 302;

    int POP3_MSG_STATUS_INITIAL = 501;
    int POP3_MSG_STATUS_DELETED = 502;

    String APP_TITLE = "pop3server";
    String APP_VERSION = "1.0";

    String USERS_DOMAIN ="Users";
    String PASS_FILE = "pass.pwd";
    String LOG_FILE = "log.txt";
}
