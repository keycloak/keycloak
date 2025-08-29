package org.keycloak.broker.provider;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.JsonWebToken;

public interface ClientAssertionContext {

    String getAssertionType();
    JWSInput getJwsInput();
    JsonWebToken getToken();
    ClientModel getClient();
    boolean isFailure();
    String getError();

    boolean failure(String error);

}
