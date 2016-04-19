package org.keycloak.testsuite.performance;

/**
 *
 * @author tkyjovsk
 */
public class LoginLogoutParameters {

    public static final Integer AVERAGE_LOGIN_TIME_LIMIT = Integer.parseInt(System.getProperty("average.login.time.limit", "500"));
    public static final Integer AVERAGE_LOGOUT_TIME_LIMIT = Integer.parseInt(System.getProperty("average.logout.time.limit", "500"));

    public static final String ACCESS_REQUEST_TIME = "ACCESS_REQUEST";
    public static final String LOGIN_REQUEST_TIME = "LOGIN_REQUEST";
    public static final String LOGIN_VERIFY_REQUEST_TIME = "LOGIN_VERIFY_REQUEST";
    public static final String LOGOUT_REQUEST_TIME = "LOGOUT_REQUEST";
    public static final String LOGOUT_VERIFY_REQUEST_TIME = "LOGOUT_VERIFY_REQUEST";

}
