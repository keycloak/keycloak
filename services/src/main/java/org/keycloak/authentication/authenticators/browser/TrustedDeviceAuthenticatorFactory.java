package org.keycloak.authentication.authenticators.browser;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.provider.ProviderConfigProperty;

public class TrustedDeviceAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-trusted-device";
    private static final TrustedDeviceAuthenticator SINGLETON = new TrustedDeviceAuthenticator();

    @Override
    public String getDisplayType() {
        return "Trusted Device";
    }

    @Override
    public String getReferenceCategory() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Validates Trusted Device Cookie";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
