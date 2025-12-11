package org.keycloak.services.ui.extend;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiTabProviderFactory<T> extends ComponentFactory<T, UiTabProvider> {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    @Override
    default Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("path", getPath());
        metadata.put("params", getParams());
        return metadata;
    }

    String getPath();

    Map<String, String> getParams();
}
