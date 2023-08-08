package org.keycloak.services.resource;

import org.keycloak.provider.Provider;
import org.keycloak.theme.Theme;

/**
 * <p>A {@link AccountResourceProvider} creates JAX-RS resource instances for the Account endpoints, allowing
 * an implementor to override the behavior of the entire Account console.
 */
public interface AccountResourceProvider extends Provider {
  /** Return true if this should be used with the given theme. */
  boolean useWithTheme(Theme theme);

  /** Returns a JAX-RS resource instance. */
  Object getResource();
}
