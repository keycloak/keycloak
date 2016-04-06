package org.keycloak.testsuite.performance.metrics.impl;

import java.util.ArrayList;
import java.util.Collections;
import org.keycloak.testsuite.performance.metrics.ComputedMetric;

/**
 *
 * @author tkyjovsk
 */
public class ArrayListMetric extends ArrayList<Long> implements ComputedMetric {

    @Override
    public void reset() {
        clear();
    }

    @Override
    public void addValue(long value) {
        add(value);
    }

    @Override
    public long getCount() {
        return size();
    }

    @Override
    public long getMin() {
        return Collections.min(this);
    }

    @Override
    public long getMax() {
        return Collections.max(this);
    }

    @Override
    public double getAverage() {
        long sum = 0;
        for (long l : this) {
            sum += l;
        }
        return isEmpty() ? 0 : sum / size();
    }

    @Override
    public double getStandardDeviation() {
        double average = getAverage();
        long sumSquare = 0;
        for (long l : this) {
            sumSquare += l * l;
        }
        return isEmpty() ? 0
                : Math.sqrt(sumSquare / size() - (average * average));
    }

}
