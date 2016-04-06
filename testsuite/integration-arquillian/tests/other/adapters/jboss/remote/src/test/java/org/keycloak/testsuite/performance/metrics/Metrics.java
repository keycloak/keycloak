package org.keycloak.testsuite.performance.metrics;

import java.util.Map;

/**
 *
 * @author tkyjovsk
 * @param <T>
 */
public interface Metrics<T extends Metric> extends Map<String, T> {

}
