package org.keycloak.services.util;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;

import java.util.Map;

public class TokenClientAttributeHelper {
    public static Integer getClientSessionIdleTimeout(RealmModel realm, String clientId) {
        String clientSessionIdleTemp = TokenClientAttributeHelper.getAttributes(realm, clientId, "client.session.idle.timeout");
        if (clientSessionIdleTemp != null) {
            return Integer.valueOf(clientSessionIdleTemp);
        }

        return null;
    }

    public static Integer getClientSessionMaxLifespan(RealmModel realm, String clientId) {
        String clientSessionMaxLifespan = TokenClientAttributeHelper.getAttributes(realm, clientId, "client.session.max.lifespan");
        if (clientSessionMaxLifespan != null) {
            return Integer.valueOf(clientSessionMaxLifespan);
        }

        return null;
    }

    private static String getAttributes(RealmModel realm, String clientId, String attributeName) {
        if (realm == null || clientId == null || attributeName == null) {
            return null;
        }

        ClientModel model = realm.getClientByClientId(clientId);
        if (model == null) {
            return null;
        }

        Map<String, String> attributes = model.getAttributes();
        if (attributes == null) {
            return null;
        }

        String value = attributes.get(attributeName);
        if (value == null) {
            return null;
        }

        return value;
    }
}
