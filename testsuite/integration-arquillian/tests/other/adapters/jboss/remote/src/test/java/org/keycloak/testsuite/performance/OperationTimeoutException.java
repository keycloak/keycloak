package org.keycloak.testsuite.performance;

/**
 *
 * @author tkyjovsk
 */
public class OperationTimeoutException extends Exception {

    private final String metric;
    private final long value;

    public OperationTimeoutException(String metric, Throwable cause) {
        this(metric, 0, cause);
    }

    public OperationTimeoutException(String metric, long value, Throwable cause) {
        super(cause);
        this.metric = metric;
        this.value = value;
    }

    public String getMetric() {
        return metric;
    }

    public long getValue() {
        return value;
    }

}
