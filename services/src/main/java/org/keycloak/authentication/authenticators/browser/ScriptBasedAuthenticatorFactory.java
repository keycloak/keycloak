package org.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

import static java.util.Arrays.asList;
import static org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticator.SCRIPT_CODE;
import static org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticator.SCRIPT_DESCRIPTION;
import static org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticator.SCRIPT_NAME;
import static org.keycloak.provider.ProviderConfigProperty.SCRIPT_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

/**
 * An {@link AuthenticatorFactory} for {@link ScriptBasedAuthenticator}s.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ScriptBasedAuthenticatorFactory implements AuthenticatorFactory {

    static final String PROVIDER_ID = "auth-script-based";

    static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.OPTIONAL,
            AuthenticationExecutionModel.Requirement.DISABLED};

    static final ScriptBasedAuthenticator SINGLETON = new ScriptBasedAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session) {

        /*
         would be great to have the actual authenticatorId here in order to initialize the authenticator in the ctor with
         the appropriate config from session.getContext().getRealm().getAuthenticatorConfigById(authenticatorId);

         This would help to avoid potentially re-evaluating the provide script multiple times per authenticator execution.
        */
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {
        //NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOOP
    }

    @Override
    public void close() {
        //NOOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "script";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Script-based Authentication";
    }

    @Override
    public String getHelpText() {
        return "Script based authentication.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {

        ProviderConfigProperty name = new ProviderConfigProperty();
        name.setType(STRING_TYPE);
        name.setName(SCRIPT_NAME);
        name.setLabel("Script Name");
        name.setHelpText("The name of the script used to authenticate.");

        ProviderConfigProperty description = new ProviderConfigProperty();
        description.setType(STRING_TYPE);
        description.setName(SCRIPT_DESCRIPTION);
        description.setLabel("Script Description");
        description.setHelpText("The description of the script used to authenticate.");

        ProviderConfigProperty script = new ProviderConfigProperty();
        script.setType(SCRIPT_TYPE);
        script.setName(SCRIPT_CODE);
        script.setLabel("Script Source");
        script.setDefaultValue("//enter your script here");
        script.setHelpText("The script used to authenticate. Scripts must at least define a function with the name 'authenticate' that accepts a context (AuthenticationFlowContext) parameter." +
                "This authenticator exposes the following additional variables: 'script', 'LOG'");

        return asList(name, description, script);
    }
}
