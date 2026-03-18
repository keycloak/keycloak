package org.keycloak.authentication;

public interface ClientAuthenticationFlowContextSupplier<T> {

    T get(ClientAuthenticationFlowContext context) throws Exception;

}
