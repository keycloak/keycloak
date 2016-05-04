package org.keycloak.testsuite.performance.statistics;

import java.math.BigDecimal;

/**
 * Allows to compute statistical values without holding the actual measurements.
 *
 * @author tkyjovsk
 */
public final class MovingUpdatableStatistic implements UpdatableStatistic {

    public static final String STATISTIC_TYPE_PROPERTY_VALUE = "moving";

    private BigDecimal sum;
    private BigDecimal sumSquare;
    private long count;
    private long min;
    private long max;

    public MovingUpdatableStatistic() {
        reset();
    }

    @Override
    public synchronized void reset() {
        this.sum = new BigDecimal(0);
        this.sumSquare = new BigDecimal(0);
        count = 0;
        min = Long.MAX_VALUE;
        max = 0;
    }

    @Override
    public synchronized void addValue(long value) {
        sum = sum.add(new BigDecimal(value));
        sumSquare = sumSquare.add(new BigDecimal(value * value));
        min = Math.min(min, value);
        max = Math.max(max, value);
        count++;
    }

    @Override
    public synchronized long getCount() {
        return count;
    }

    @Override
    public synchronized long getMin() {
        return min;
    }

    @Override
    public synchronized long getMax() {
        return max;
    }

    @Override
    public synchronized double getAverage() {
        return count == 0 ? 0
                : sum.longValue() / count;
    }

    @Override
    public synchronized double getStandardDeviation() {
        double average = getAverage();
        return count == 0 ? 0
                : Math.sqrt(sumSquare.longValue() / count - (average * average));
    }

    @Override
    public String toString() {
        return Double.toString(getAverage());
    }

}
