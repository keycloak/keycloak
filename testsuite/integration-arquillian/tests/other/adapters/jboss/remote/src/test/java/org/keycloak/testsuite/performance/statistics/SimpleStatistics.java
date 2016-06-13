package org.keycloak.testsuite.performance.statistics;

import java.util.TreeMap;

/**
 *
 * @author tkyjovsk
 */
public class SimpleStatistics extends TreeMap<String, SimpleStatistic> implements Statistics<SimpleStatistic> {

    public SimpleStatistics(Statistics statistics) {
        for (Object statistic : statistics.keySet()) {
            put(statistic.toString(), new SimpleStatistic((Statistic) statistics.get(statistic)));
        }
    }

}
