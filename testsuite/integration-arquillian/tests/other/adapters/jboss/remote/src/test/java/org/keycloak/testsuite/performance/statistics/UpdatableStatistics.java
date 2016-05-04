package org.keycloak.testsuite.performance.statistics;

/**
 *
 * @author tkyjovsk
 */
public interface UpdatableStatistics extends Statistics<UpdatableStatistic> {

    public void reset();

    public void addValue(String statistic, long value);

}
