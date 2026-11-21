package org.keycloak.services.ui.extend;

import org.keycloak.provider.Provider;

/**
 * SPI for admin console pages backed by a dynamically loaded JavaScript module.
 * Unlike {@link UiPageProvider}, this does not use config properties / Components CRUD.
 */
public interface UiScriptProvider extends Provider {
}
