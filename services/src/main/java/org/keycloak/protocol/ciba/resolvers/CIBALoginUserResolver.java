package org.keycloak.protocol.ciba.resolvers;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

public interface CIBALoginUserResolver extends Provider {

    UserModel getUserFromLoginHint(String loginHint);

    UserModel getUserFromLoginHintToken(String loginHintToken);

    UserModel getUserFromIdTokenHint(String idToken);

    String getInfoUsedByAuthentication(UserModel user);

    UserModel getUserFromInfoUsedByAuthentication(String info);

}
