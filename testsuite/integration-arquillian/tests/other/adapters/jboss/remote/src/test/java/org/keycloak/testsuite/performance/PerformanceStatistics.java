package org.keycloak.testsuite.performance;

import org.keycloak.testsuite.performance.statistics.DataHoldingUpdatableStatistic;
import org.keycloak.testsuite.performance.statistics.MovingUpdatableStatistic;
import org.keycloak.testsuite.performance.statistics.SimpleStatistics;
import org.keycloak.testsuite.performance.statistics.UpdatableStatistic;
import org.keycloak.testsuite.performance.statistics.UpdatableStatistics;

import java.util.concurrent.ConcurrentHashMap;

/**
 * PerformanceStatistics. Concurrent hash map of UpdatableStatistic objects, 
 * type of which can be selected by the "statistic.type" property.
 *
 * @author tkyjovsk
 */
public class PerformanceStatistics extends ConcurrentHashMap<String, UpdatableStatistic> implements UpdatableStatistics {

    public static final String STATISTIC_TYPE = System.getProperty("statistic.type", MovingUpdatableStatistic.STATISTIC_TYPE_PROPERTY_VALUE);

    @Override
    public void reset() {
        clear();
    }

    private UpdatableStatistic createIfNullAndGet(String statistic) {
        UpdatableStatistic updatableStatistic = get(statistic);
        if (updatableStatistic == null) {
            switch (STATISTIC_TYPE) {
                case DataHoldingUpdatableStatistic.STATISTIC_TYPE_PROPERTY_VALUE:
                    updatableStatistic = new DataHoldingUpdatableStatistic();
                    break;
                case MovingUpdatableStatistic.STATISTIC_TYPE_PROPERTY_VALUE:
                    updatableStatistic = new DataHoldingUpdatableStatistic();
                    break;
                default:
                    throw new IllegalStateException(String.format(
                            "Unknown statistic type: '%s'. Supported values: %s | %s",
                            STATISTIC_TYPE,
                            DataHoldingUpdatableStatistic.STATISTIC_TYPE_PROPERTY_VALUE,
                            MovingUpdatableStatistic.STATISTIC_TYPE_PROPERTY_VALUE));
            }
            put(statistic, updatableStatistic);
        }
        return updatableStatistic;
    }

    @Override
    public void addValue(String statistic, long value) {
        createIfNullAndGet(statistic).addValue(value);
    }

    public SimpleStatistics snapshot() {
        return new SimpleStatistics(this);
    }

}
