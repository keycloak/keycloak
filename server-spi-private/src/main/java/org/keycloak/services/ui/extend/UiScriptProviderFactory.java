package org.keycloak.services.ui.extend;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

/**
 * Factory for {@link UiScriptProvider}. {@link #getId()} is used as the nav label;
 * {@link #getUrl()} is the module URL the admin console dynamically imports.
 * The module must export a {@code default} function that receives
 * {@code { realm, i18n, adminClient }} and returns an {@code HTMLElement}.
 */
public interface UiScriptProviderFactory extends ProviderFactory<UiScriptProvider> {

    @Override
    default UiScriptProvider create(KeycloakSession session) {
        return null;
    }

    /**
     * URL of an ES module the admin console will {@code import()}.
     */
    String getUrl();
}
