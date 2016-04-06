package org.keycloak.testsuite.performance.metrics;

/**
 *
 * @author tkyjovsk
 */
public interface ComputedMetrics extends Metrics<ComputedMetric> {

    public void reset();
    public void addValue(String metric, long value);
    public Metrics computeMetrics();
    
}
