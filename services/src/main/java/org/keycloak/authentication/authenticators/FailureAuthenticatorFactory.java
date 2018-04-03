package org.keycloak.authentication.authenticators;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

public class FailureAuthenticatorFactory implements AuthenticatorFactory {

	public static final String PROVIDER_ID = "always-fail";
	public static final Authenticator SINGLETON = new FailureAuthenticator();

	@Override
	public String getDisplayType() {
		return "Deny All";
	}

	@Override
	public String getReferenceCategory() {
		return UserCredentialModel.PASSWORD;
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
			AuthenticationExecutionModel.Requirement.REQUIRED,
			AuthenticationExecutionModel.Requirement.DISABLED
	};

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return REQUIREMENT_CHOICES;
	}


	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public String getHelpText() {
		return "Authenticator returns failure every time.  Only use this if you want to disable auth entirely for the given flow.";
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return Collections.emptyList();
	}

	@Override
	public Authenticator create(final KeycloakSession session) {
		return SINGLETON;
	}

	@Override
	public void init(final Config.Scope config) {
		// no-op
	}

	@Override
	public void postInit(final KeycloakSessionFactory factory) {
		// no-op
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
