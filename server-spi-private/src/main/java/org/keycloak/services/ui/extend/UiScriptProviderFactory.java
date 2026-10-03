package org.keycloak.services.ui.extend;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiScriptProviderFactory<T> extends ComponentFactory<T, UiScriptProvider> {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    String getTagName();

    String getScriptPath();

    @Override
    default Map<String, Object> getTypeMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tagName", getTagName());
        metadata.put("scriptPath", getScriptPath());
        return metadata;
    }

    @Override
    default boolean isInternal() {
        return true;
    }
}
