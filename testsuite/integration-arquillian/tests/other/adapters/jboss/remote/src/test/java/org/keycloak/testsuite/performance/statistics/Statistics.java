package org.keycloak.testsuite.performance.statistics;

import java.util.Map;

/**
 * A Map of named statistics.
 *
 * @author tkyjovsk
 * @param <T>
 */
public interface Statistics<T extends Statistic> extends Map<String, T> {

}
