package org.keycloak.ssf.services;

import org.keycloak.models.RealmModel;
import org.keycloak.ssf.transmitter.SsfScopes;

public class SsfBootstrap {

    public static void addSsfSupport(RealmModel realm) {

        // FIXME this might be expensive if done for many realms
        SsfScopes.createDefaultClientScopes(realm);
    }
}
