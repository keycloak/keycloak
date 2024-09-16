package org.keycloak.test.framework.server;

/**
 *
 * Used to deploy a module of custom providers to the Keycloak server for the purpose of testing with the
 * Keycloak testing framework.
 * It is not meant for a single Provider implementation.
 * Implement the methods to return values used in your provider module's pom file.
 *
 * @author <a href="mailto:svacek@redhat.com">Simon Vacek</a>
 */
public interface ProviderModule {

    String groupId();

    String artifactId();

    String version();
}
