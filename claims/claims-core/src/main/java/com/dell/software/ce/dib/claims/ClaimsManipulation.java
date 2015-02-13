package com.dell.software.ce.dib.claims;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.util.MultivaluedHashMap;

public interface ClaimsManipulation extends Provider {
    public void initClaims(MultivaluedHashMap<String, String> claims, UserSessionModel userSession, ClientModel model, UserModel user);
}
