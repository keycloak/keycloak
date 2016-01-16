package org.keycloak.models;

/**
 * Task to be executed inside transaction
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface KeycloakSessionTask {

    public void run(KeycloakSession session);

}
