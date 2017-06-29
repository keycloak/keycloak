package org.keycloak.examples.authn.delegate.authenticator;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class AuthnDelegationAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {
    // TODO: if considering internationalization, need to move text onto message file for each locale.
    
    public static final String PROVIDER_ID = "auth-redirect";
    public static final AuthnDelegationAuthenticator SINGLETON = new AuthnDelegationAuthenticator();
    
    public static final String LOGIN_CHALLENGE_MARKER = "external.authentication.server.authentication.challenge.marker";

    
    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Scope config) {
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

    @Override
    public String getHelpText() {
        return "Delegate Authentication to External Server.";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String AS_AUTHN_URI = "external.authentication.server.authentication.uri";
    public static final String AS_USERID_URI = "external.authentication.server.authenticated.user.id.endpoint";
    public static final String FW_QUERY_PARAMS = "properties.forwarding.query.parameters";
    public static final String FW_HTTP_HEADERS = "properties.forwarding.http.headers";
    public static final String IS_HTTP_FORM_POST = "external.authentication.forwarding.form.post";
    public static final String IS_BACKEND_COMM_SSL_REQUIRED = "external.authentication.backend.communication.ssl.required";
    
    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(AS_AUTHN_URI);
        property.setLabel("External Authentication Server Authentication URI");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("URI for External Authentication Server.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AS_USERID_URI);
        property.setLabel("External Authentication Server Authenticated User ID Endpoint");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("URI for obtaining authenticated user id.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(FW_QUERY_PARAMS);
        property.setLabel("Properties for forwarding query parameters");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${jboss.server.config.dir}/forwarding-query-parameters.properties");
        property.setHelpText("Properties file for query parameters forwarding to external authentication server");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(FW_HTTP_HEADERS);
        property.setLabel("Properties for forwarding HTTP Headers");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${jboss.server.config.dir}/forwarding-http-headers.properties");
        property.setHelpText("Properties file for HTTP Header Fields forwarding to external authentication server");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(IS_HTTP_FORM_POST);
        property.setLabel("HTTP FORM POST for forwarding parameters");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(true);
        property.setHelpText("Conduct HTTP FORM POST to forward parameters to an external authentication server. If Off, conduct HTTP redirect instead.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(IS_BACKEND_COMM_SSL_REQUIRED);
        property.setLabel("SSL Required for Back End Connmunication");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(true);
        property.setHelpText("If true, Back End Commnication between keycloak and External Authentication Server requires TLS connections.");
        configProperties.add(property);
    }

    @Override
    public String getDisplayType() {
        return "Authentication Delegation";
    }

    @Override
    public String getReferenceCategory() {
        return "authentication delegation";
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };
    
    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

}
