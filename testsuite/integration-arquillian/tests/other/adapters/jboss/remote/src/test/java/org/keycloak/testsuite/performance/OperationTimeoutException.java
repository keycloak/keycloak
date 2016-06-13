package org.keycloak.testsuite.performance;

/**
 *
 * @author tkyjovsk
 */
public class OperationTimeoutException extends Exception {

    private final String statistic;
    private final long value;

    public OperationTimeoutException(String statistic, Throwable cause) {
        this(statistic, 0, cause);
    }

    public OperationTimeoutException(String statistic, long value, Throwable cause) {
        super(cause);
        this.statistic = statistic;
        this.value = value;
    }

    public String getStatistic() {
        return statistic;
    }

    public long getValue() {
        return value;
    }

}
