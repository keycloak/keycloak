package org.keycloak.services.resource;

import org.keycloak.provider.Provider;
import org.keycloak.theme.Theme;

import java.io.IOException;

/**
 * <p>A {@link AccountResourceProvider} creates JAX-RS resource instances for the Account endpoints, allowing
 * an implementor to override the behavior of the entire Account console.
 */
public interface AccountResourceProvider extends Provider {
  public static final String PROVIDER_CLASS_KEY = "accountResourceProvider";

  /** Return true if this should be used with the given theme. */
  default boolean useWithTheme(Theme theme) {
    try {
      String providerClass = theme.getProperties().getProperty(PROVIDER_CLASS_KEY);
      return (providerClass != null && providerClass.equals(getClass().getName()));
    } catch (IOException ignore) {
      return false;
    }
  }

  /** Returns a JAX-RS resource instance. */
  Object getResource();
}
