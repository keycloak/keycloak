package org.keycloak.theme;

import org.keycloak.models.ThemeManager;
import org.keycloak.provider.ProviderFactory;

/**
 */
public interface ThemeManagerFactory extends ProviderFactory<ThemeManager> {
  void clearCache();
}
