package org.keycloak.protocol.oidc;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.util.TokenUtil;

public class DefaultRequestedScopeValidationProvider implements RequestedScopeValidationProvider {

	@Override
	public boolean isValid(KeycloakSession session, String scopes, ClientModel client, UserModel user) {
		if (scopes == null) {
			return true;
		}

		Set<String> clientScopes = TokenManager.getRequestedClientScopes(session, scopes, client, user)
				.filter(((Predicate<ClientScopeModel>) ClientModel.class::isInstance).negate())
				.map(ClientScopeModel::getName)
				.collect(Collectors.toSet());
		Collection<String> requestedScopes = TokenManager.parseScopeParameter(scopes).collect(Collectors.toSet());

		if (TokenUtil.isOIDCRequest(scopes)) {
			requestedScopes.remove(OAuth2Constants.SCOPE_OPENID);
		}

		if (!requestedScopes.isEmpty() && clientScopes.isEmpty()) {
			return false;
		}

		for (String requestedScope : requestedScopes) {
			// we also check dynamic scopes in case the client is from a provider that dynamically provides scopes to their clients
			if (!clientScopes.contains(requestedScope) && client.getDynamicClientScope(requestedScope) == null) {
				return false;
			}
		}

		return true;
	}
} 