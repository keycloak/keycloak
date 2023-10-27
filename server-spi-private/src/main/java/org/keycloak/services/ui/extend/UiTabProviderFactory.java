package org.keycloak.services.ui.extend;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiTabProviderFactory<T> extends ComponentFactory<T, UiTabProvider> {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }
}
