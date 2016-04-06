package org.keycloak.testsuite.performance.metrics.impl;

import org.jboss.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import org.keycloak.testsuite.performance.metrics.ComputedMetrics;

/**
 *
 * @author tkyjovsk
 */
public class ResultsWithThroughput extends Results {

    public static final Logger LOG = Logger.getLogger(ResultsWithThroughput.class);
    private final Map<String, Double> throughput;

    public ResultsWithThroughput(ComputedMetrics metrics, long durationMillis) {
        super(metrics);
        throughput = new HashMap<>();
        for (String metric : keySet()) {
            throughput.put(metric, (double) get(metric).getCount() / durationMillis * 1000);
        }
    }

    public Map<String, Double> getThroughput() {
        return throughput;
    }

    @Override
    public String toString() {
        return "Results: " + super.toString() + "\n"
                + "Throughput: " + getThroughput();
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " Throughput";

    }

    public void logResults() {
        LOG.info(getHeader());
        for (String metric : keySet()) {
            LOG.info(metric + " " + get(metric).toLogString() + " " + getThroughput().get(metric));
        }
    }

}
