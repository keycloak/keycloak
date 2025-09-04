package org.keycloak.broker.provider;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;

public interface ClientAssertionContext {

    RealmModel getRealm();
    ClientModel getClient();
    String getAssertionType();
    JWSInput getJwsInput();
    JsonWebToken getToken();
    boolean isFailure();
    String getError();

    boolean failure(String error);

}
