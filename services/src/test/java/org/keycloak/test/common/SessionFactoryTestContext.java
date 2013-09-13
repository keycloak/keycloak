package org.keycloak.test.common;

import org.keycloak.services.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SessionFactoryTestContext {

    void beforeTestClass();

    void afterTestClass();

    /**
     * Init system properties (or other configuration) to ensure that KeycloakApplication.buildSessionFactory() will return correct
     * instance of KeycloakSessionFactory for our test
     */
    void initEnvironment();
}
