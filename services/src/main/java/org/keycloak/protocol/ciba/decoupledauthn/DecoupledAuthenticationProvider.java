package org.keycloak.protocol.ciba.decoupledauthn;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ciba.endpoints.request.BackchannelAuthenticationRequest;
import org.keycloak.provider.Provider;

public interface DecoupledAuthenticationProvider extends Provider {

    void doBackchannelAuthentication(ClientModel client, BackchannelAuthenticationRequest request, int expiresIn, String authResultId, String userSessionIdWillBeCreated);

}
