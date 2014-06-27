package org.keycloak.testsuite.performance;

import org.apache.jmeter.samplers.SampleResult;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface Worker {

    void setup(int workerId, KeycloakSession session);

    void run(SampleResult result, KeycloakSession session);

    void tearDown();

}
