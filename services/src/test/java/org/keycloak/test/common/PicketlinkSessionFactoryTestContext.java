package org.keycloak.test.common;

import org.keycloak.services.resources.KeycloakApplication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PicketlinkSessionFactoryTestContext implements SessionFactoryTestContext {

    @Override
    public void beforeTestClass() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterTestClass() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initEnvironment() {
        System.setProperty(KeycloakApplication.SESSION_FACTORY, KeycloakApplication.SESSION_FACTORY_PICKETLINK);
    }
}
