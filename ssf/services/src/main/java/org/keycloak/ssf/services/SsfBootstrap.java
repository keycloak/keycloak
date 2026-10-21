package org.keycloak.ssf.services;

import org.keycloak.models.RealmModel;
import org.keycloak.ssf.transmitter.SsfScopes;

public class SsfBootstrap {

    public static void addSsfSupport(RealmModel realm) {
        if (realm == null) {
            return;
        }
        SsfScopes.createDefaultClientScopes(realm);
    }
}
