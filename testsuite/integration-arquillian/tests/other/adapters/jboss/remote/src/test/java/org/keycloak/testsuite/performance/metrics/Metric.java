package org.keycloak.testsuite.performance.metrics;

/**
 *
 * @author tkyjovsk
 */
public interface Metric {
    
    public long getCount();
    public long getMin();
    public long getMax();
    public double getAverage();
    public double getStandardDeviation();
    
}
