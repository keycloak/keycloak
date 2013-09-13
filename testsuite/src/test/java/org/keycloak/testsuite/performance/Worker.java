package org.keycloak.testsuite.performance;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.log.Logger;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface Worker {

    void setup(int workerId, KeycloakSession identitySession);

    void run(SampleResult result, KeycloakSession identitySession);

    void tearDown();

}
