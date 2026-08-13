package org.keycloak.provider;

import org.keycloak.models.ModelException;

/**
 * Exception thrown when a provider configuration property name is not unique.
 * This is used to indicate that a property with the same name already exists
 * in the configuration, which violates the uniqueness constraint.
 */
public class ProviderConfigPropertyNameNotUniqueException extends ModelException {

  public ProviderConfigPropertyNameNotUniqueException(String message) {
    super(message);
  }
}
