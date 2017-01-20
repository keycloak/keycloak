package org.keycloak.testsuite.performance.statistics;

/**
 * Statistic provides statistical information about a data set.
 * Number of measurements, minimum/maximum/average value and standard deviation.
 * 
 * @author tkyjovsk
 */
public interface Statistic {
    
    public long getCount();
    public long getMin();
    public long getMax();
    public double getAverage();
    public double getStandardDeviation();
    
}
