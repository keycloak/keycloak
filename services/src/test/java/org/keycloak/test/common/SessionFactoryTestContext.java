package org.keycloak.test.common;

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
