package org.keycloak.services.ui.extend;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public interface UiPageProviderFactory<T> extends ComponentFactory<T, UiPageProvider> {
    default T create(KeycloakSession session, ComponentModel model) {
        return null;
    }
}
