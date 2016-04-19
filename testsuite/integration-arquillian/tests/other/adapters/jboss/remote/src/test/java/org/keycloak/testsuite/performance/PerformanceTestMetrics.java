package org.keycloak.testsuite.performance;

import java.util.concurrent.ConcurrentHashMap;
import org.keycloak.testsuite.performance.metrics.ComputedMetric;
import org.keycloak.testsuite.performance.metrics.ComputedMetrics;
import org.keycloak.testsuite.performance.metrics.impl.ArrayListMetric;
import org.keycloak.testsuite.performance.metrics.impl.MovingAverageMetric;
import org.keycloak.testsuite.performance.metrics.impl.ResultsWithThroughput;

/**
 *
 * @author tkyjovsk
 */
public class PerformanceTestMetrics extends ConcurrentHashMap<String,ComputedMetric> implements ComputedMetrics {

    public static final String METRIC_MOVING_AVERAGE = "MovingAverage";
    public static final String METRIC_ARRAY_LIST = "ArrayList";
    public static final String METRIC = System.getProperty("metric", METRIC_MOVING_AVERAGE);

    Timer timer = new Timer();
    
    @Override
    public void reset() {
        clear();
        timer.reset();
    }

    private ComputedMetric getOrCreate(String metric) {
        ComputedMetric m = get(metric);
        if (m == null) {
            if (METRIC_ARRAY_LIST.equals(metric)) {
                m = new ArrayListMetric();
            } else {
                m = new MovingAverageMetric();
            }
            put(metric, m);
        }
        return m;
    }

    @Override
    public void addValue(String metric, long value) {
        getOrCreate(metric).addValue(value);
    }

    @Override
    public ResultsWithThroughput computeMetrics() {
        return new ResultsWithThroughput(this, timer.getElapsedTime());
    }

}
