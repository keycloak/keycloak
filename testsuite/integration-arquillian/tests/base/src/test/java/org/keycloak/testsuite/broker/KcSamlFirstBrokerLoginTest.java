package org.keycloak.testsuite.broker;

import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcSamlFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }


    @Test
    @Override
    public void testUpdateProfileIfNotMissingInformation() {
        // skip this test as this provider do not return name and surname so something is missing always
    }
}
