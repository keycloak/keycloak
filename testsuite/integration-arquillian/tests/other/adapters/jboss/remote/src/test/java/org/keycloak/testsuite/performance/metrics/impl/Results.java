package org.keycloak.testsuite.performance.metrics.impl;

import java.util.TreeMap;
import org.keycloak.testsuite.performance.metrics.ComputedMetrics;
import org.keycloak.testsuite.performance.metrics.Metrics;

/**
 *
 * @author tkyjovsk
 */
public class Results extends TreeMap<String, Result> implements Metrics<Result> {

    public Results(ComputedMetrics metrics) {
        for (String metric : metrics.keySet()) {
            put(metric, new Result(metrics.get(metric)));
        }
    }

    public String getHeader() {
        return "# Operation Count Min Max Average Standard-Deviation";
    }

}
