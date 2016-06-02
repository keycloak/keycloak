package org.keycloak.testsuite.performance.statistics;

/**
 * UpdatableStatistic. A Statistic that can be updated, e.g. from PerformanceTest.Runnable.
 * Implementations should be thread-safe.
 *
 * @author tkyjovsk
 */
public interface UpdatableStatistic extends Statistic {

    public void reset();

    public void addValue(long value);

}
