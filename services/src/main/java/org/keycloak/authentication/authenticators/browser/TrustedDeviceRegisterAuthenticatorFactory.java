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

public class TrustedDeviceRegisterAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "auth-trusted-device-register";
    private static final TrustedDeviceRegisterAuthenticator SINGLETON = new TrustedDeviceRegisterAuthenticator();

    @Override
    public String getDisplayType() {
        return "Trusted Device Register";
    }

    @Override
    public String getReferenceCategory() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public boolean isConfigurable() {
        return false;
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
        return "Asks the user if they want to 'remember' their device. If they agree, a cookie is set with a configured period of time.";
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
