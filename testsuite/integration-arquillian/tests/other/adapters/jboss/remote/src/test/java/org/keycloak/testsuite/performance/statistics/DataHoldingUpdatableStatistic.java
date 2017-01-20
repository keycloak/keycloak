package org.keycloak.testsuite.performance.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author tkyjovsk
 */
public class DataHoldingUpdatableStatistic implements UpdatableStatistic {
    
    public static final String STATISTIC_TYPE_PROPERTY_VALUE = "data";

    private final List<Long> data = Collections.synchronizedList(new ArrayList<Long>());

    @Override
    public synchronized void reset() {
        data.clear();
    }

    @Override
    public synchronized void addValue(long value) {
        data.add(value);
    }

    @Override
    public synchronized long getCount() {
        return data.size();
    }

    @Override
    public synchronized long getMin() {
        return Collections.min(data);
    }

    @Override
    public synchronized long getMax() {
        return Collections.max(data);
    }

    @Override
    public synchronized double getAverage() {
        long sum = 0;
        for (long l : data) {
            sum += l;
        }
        return data.isEmpty() ? 0 : sum / data.size();
    }

    @Override
    public synchronized double getStandardDeviation() {
        double average = getAverage();
        long sumSquare = 0;
        for (long l : data) {
            sumSquare += l * l;
        }
        return data.isEmpty() ? 0
                : Math.sqrt(sumSquare / data.size() - (average * average));
    }

}
