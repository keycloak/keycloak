package org.keycloak.authentication.authenticators.sessionlimits;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class RealmSessionLimitsAuthenticatorFactory implements AuthenticatorFactory {
    public static final String PROVIDER_ID = "realm-session-limits";
    public static final String REALM_LIMIT = "realmLimit";
    public static final String BEHAVIOR = "behavior";
    public static final String DENY_NEW_SESSION = "Deny new session";
    public static final String LOG_ONLY = "Do nothing, just log";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String USER_SESSION_LIMITS = "user-session-limits";

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getDisplayType() {
        return "Realm session count limiter";
    }

    @Override
    public String getReferenceCategory() {
        return null;
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
        return false;
    }

    @Override
    public String getHelpText() {
        return "Realm session count limiter config";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty realmSessionCountLimit = new ProviderConfigProperty();
        realmSessionCountLimit.setName(REALM_LIMIT);
        realmSessionCountLimit.setLabel("Session count limit for this realm");
        realmSessionCountLimit.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty behaviourProperty = new ProviderConfigProperty();
        behaviourProperty.setName(BEHAVIOR);
        behaviourProperty.setLabel("Behavior when user limit is exceeded");
        behaviourProperty.setType(ProviderConfigProperty.LIST_TYPE);
        behaviourProperty.setDefaultValue(LOG_ONLY);
        behaviourProperty.setOptions(Arrays.asList(DENY_NEW_SESSION, LOG_ONLY));

        ProviderConfigProperty customErrorMessage = new ProviderConfigProperty();
        customErrorMessage.setName(ERROR_MESSAGE);
        customErrorMessage.setLabel("Optional custom error message");
        customErrorMessage.setType(ProviderConfigProperty.STRING_TYPE);

        return Arrays.asList(realmSessionCountLimit, behaviourProperty, customErrorMessage);
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new RealmSessionLimitsAuthenticator(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
