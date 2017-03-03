package com.company;

/**
 * Интерфейс, описывающий константы, используемые в данной реализации POP3 сервера.
 */
public interface POP3Defines {
    /**
     * Порт, используемый протоколом POP3. Значение по-умолчанию: 110.
     */
    int POP3_PORT = 110;

    /**
     * Индикатор выполнения, сообщающий об ошибке. Значение по-умолчанию: 0.
     */
    int POP3_DEFAULT_NEGATIVE_RESPONSE = 0;

    /**
     * Индикатор выполнения, сообщающий об успехе. Значение по-умолчанию: 1.
     */
    int POP3_DEFAULT_AFFIRMATIVE_RESPONSE = 1;

    /**
     * Внутренний индикатор приветственного сообщения. Значение по-умолчанию: 2.
     */
    int POP3_WELCOME_RESPONSE = 2;

    /**
     * Внутренний индикатор, сообщающий, что клиент закончил сессию. Значение по-умолчанию: 100.
     */
    int POP3_SESSION_QUITED = 100;

    /**
     * Состояние сервера, при котором производится авторизация клиента. Значение по-умолчанию: 300.
     */
    int POP3_STATE_AUTHORIZATION = 300;

    /**
     * Состояние сервера, при котором производятся операции с письмами. Значение по-умолчанию: 301.
     */
    int POP3_STATE_TRANSACTION = 301;

    /**
     * Состояние сервера, при котором к письмам применяются изменения, сделанные пользователем. Значение по-умолчанию: 302.
     */
    int POP3_STATE_UPDATE = 302;

    /**
     * Изначальный статус всех сообщений в почтовом ящике. Значение по-умолчанию: 501.
     */
    int POP3_MSG_STATUS_INITIAL = 501;

    /**
     * Статус сообщений, помеченных как удаленные.  Значение по-умолчанию: 502.
     */
    int POP3_MSG_STATUS_DELETED = 502;

    /**
     * Название ПО, используемого сервером. Значение по-умолчанию: robopop3server.
     */
    String APP_TITLE = "robopop3server";

    /**
     * Версия ПО, используемая сервером. Значение по-умолчанию: 1.0.
     */
    String APP_VERSION = "1.0";

    /**
     * Имя директории, в которой хранятся данные и почтовые
     * ящики всех зарегестрирован пользователь.  Значение
     * по-умолчанию: Users.
     */
    String USERS_DIRECTORY ="Users";

    /**
     * Имя файлов, используемых сервером для хранения паролей
     * для каждого пользователя. Значение по-умолчанию: pass.pwd.
     */
    String PASS_FILE = "pass.pwd";

    /**
     * Имя файла, который используется для логирования работы сервера.
     * Значение по-умолчанию: log.txt.
     */
    String LOG_FILE = "log.txt";
}
