package org.keycloak.testsuite.performance;

/**
 *
 * @author tkyjovsk
 */
public class LoginLogoutTestParameters {

    // Statistics
    public static final String ACCESS_REQUEST_TIME = "ACCESS_REQUEST";
    public static final String LOGIN_REQUEST_TIME = "LOGIN_REQUEST";
    public static final String LOGIN_VERIFY_REQUEST_TIME = "LOGIN_VERIFY_REQUEST";
    public static final String LOGOUT_REQUEST_TIME = "LOGOUT_REQUEST";
    public static final String LOGOUT_VERIFY_REQUEST_TIME = "LOGOUT_VERIFY_REQUEST";

    // Limits
    public static final Integer MAX_LOGIN_TIME_AVERAGE = Integer.parseInt(System.getProperty("max.login.time.average", "500"));
    public static final Integer MAX_LOGOUT_TIME_AVERAGE = Integer.parseInt(System.getProperty("max.logout.time.average", "500"));
    public static final double MAX_TIMEOUT_PERCENTAGE = Double.parseDouble(System.getProperty("max.timeout.percentage", "0"));

    // Other
    public static final Integer PASSWORD_HASH_ITERATIONS = Integer.parseInt(System.getProperty("password.hash.iterations", "1"));
    
    public static boolean isMeasurementWithinLimits(PerformanceMeasurement measurement) {
        return isTimeoutPercentageWithinLimits(measurement)
                && measurement.getStatistics().get(LOGIN_REQUEST_TIME).getAverage() < MAX_LOGIN_TIME_AVERAGE
                && measurement.getStatistics().get(LOGOUT_REQUEST_TIME).getAverage() < MAX_LOGOUT_TIME_AVERAGE;
    }

    public static boolean isTimeoutPercentageWithinLimits(PerformanceMeasurement measurement) {
        boolean withinLimits = true;
        for (String statistic : measurement.getStatistics().keySet()) {
            withinLimits = withinLimits && measurement.getTimeoutPercentage(statistic) <= MAX_TIMEOUT_PERCENTAGE;
        }
        return withinLimits;
    }

}
