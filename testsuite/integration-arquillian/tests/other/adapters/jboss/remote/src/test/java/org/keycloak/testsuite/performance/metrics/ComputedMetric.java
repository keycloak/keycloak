package org.keycloak.testsuite.performance.metrics;

/**
 *
 * @author tkyjovsk
 */
public interface ComputedMetric extends Metric {
    
    public void reset();
    public void addValue(long value);
    
}
