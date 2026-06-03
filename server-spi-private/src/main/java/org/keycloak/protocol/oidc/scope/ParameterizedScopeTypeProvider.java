package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.Config;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * Defines the type and validation rules for parameterized client scopes.
 *
 * <p>Built-in types (string, number, boolean, username) validate the captured parameter value
 * via {@link #validateParameter}. Only the "custom" type uses an admin-defined regex for matching.
 */
public interface ParameterizedScopeTypeProvider extends Provider, ProviderFactory<ParameterizedScopeTypeProvider> {

    /**
     * @return the unique type name, also used as the provider ID
     */
    String getTypeName();

    /**
     * Validates the captured parameter value at request time (no authenticated user yet).
     * Implementations should normalize the parameter before validation (e.g. lowercase usernames,
     * strip leading zeros from numbers).
     *
     * @param scope the client scope model, never {@code null}
     * @param parameter the captured parameter value, never {@code null} or empty
     * @throws InvalidScopeParameterException if the parameter is invalid
     */
    void validateParameter(@Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException;

    /**
     * Validates the parameter when the authenticated user is known (code-to-token, refresh, token exchange).
     * Use for authorization checks such as whether the user can act on the given parameter value.
     *
     * @param currentUser the authenticated user, never {@code null}
     * @param scope the client scope model, never {@code null}
     * @param parameter the captured parameter value, never {@code null} or empty
     * @throws InvalidScopeParameterException if the parameter is invalid for the given user
     */
    default void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
    }

    @Override
    default ParameterizedScopeTypeProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    default String getId() {
        return getTypeName();
    }

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }
}
