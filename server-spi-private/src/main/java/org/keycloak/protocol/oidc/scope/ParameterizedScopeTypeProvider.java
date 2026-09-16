package org.keycloak.protocol.oidc.scope;

import jakarta.annotation.Nonnull;

import org.keycloak.Config;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 * Defines the type and validation rules for parameterized client scopes.
 *
 * <p>Built-in types (string, number, boolean, username) validate the captured parameter value
 * via {@link #validateParameter}. Only the "custom" type uses an admin-defined regex for matching.
 */
public interface ParameterizedScopeTypeProvider extends Provider, ProviderFactory<ParameterizedScopeTypeProvider> {

    int MAX_PARAMETER_LENGTH = 255;

    String PINNED_IDENTITY_NOTE_PREFIX = "kc.scope.pinned.";

    /**
     * Clears pinned identity notes from the client session. Must be called when a fresh
     * authorization replaces an existing client session so that identity pins from a prior
     * consent are reset. Without this, a reused client session would reject a valid re-consent
     * when the entity behind a parameterized scope (e.g. a username) was recreated with a new ID.
     *
     * <p>On refresh (no user interaction), the pins remain intact and any identity mismatch
     * causes the scope to be silently dropped, preventing consent from transferring to a
     * different entity.
     *
     * @param clientSession the client session to clear pinned identities from
     */
    static void clearPinnedIdentities(AuthenticatedClientSessionModel clientSession) {
        clientSession.getNotes().keySet().stream()
                .filter(k -> k.startsWith(PINNED_IDENTITY_NOTE_PREFIX))
                .toList()
                .forEach(clientSession::removeNote);
    }

    /**
     * Resolves the client session that carries pinned identity notes (see {@link #clearPinnedIdentities}) from the
     * user session already attached to the request context.
     *
     * @return the client session, or {@code null} if no user session (or matching client session) is in context yet
     */
    static AuthenticatedClientSessionModel resolveClientSessionFromContext(KeycloakSession session, ClientModel client) {
        UserSessionModel userSession = session.getContext().getUserSession();
        if (userSession == null || client == null) {
            return null;
        }
        return userSession.getAuthenticatedClientSessionByClient(client.getId());
    }

    /**
     * @return the unique type name, also used as the provider ID
     */
    String getTypeName();

    /**
     * Whether this scope type allows the same parameterized scope to appear multiple times
     * in a single request with different parameter values (e.g., {@code scope:val1 scope:val2}).
     *
     * @return {@code true} if multiple parameter values are allowed, {@code false} otherwise
     */
    default boolean isRepeatable() {
        return true;
    }

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
     * Use for authorization checks when the user is known after authorization. Default implementation
     * is the same than the non-user variant.
     *
     * @param currentUser the authenticated user, never {@code null}
     * @param scope the client scope model, never {@code null}
     * @param parameter the captured parameter value, never {@code null} or empty
     * @throws InvalidScopeParameterException if the parameter is invalid for the given user
     */
    default void validateParameterWithUser(@Nonnull UserModel currentUser, @Nonnull ClientScopeModel scope, @Nonnull String parameter) throws InvalidScopeParameterException {
        validateParameter(scope, parameter);
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
