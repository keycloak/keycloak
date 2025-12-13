package org.keycloak.authentication.authenticators.conditional;

import org.keycloak.Config.Scope;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class ConditionalGroupAuthenticatorFactory implements ConditionalAuthenticatorFactory {
    public static final String PROVIDER_ID = "conditional-user-group";

    public static final String CONDITIONAL_USER_GROUP = "condUserGroup";
    public static final String CONF_NEGATE = "negate";

    @Override
    public void init(Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
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

    @Override
    public String getDisplayType() {
        return "Condition - user group";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final Requirement[] REQUIREMENT_CHOICES = {
            Requirement.REQUIRED,
            Requirement.DISABLED
    };

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Flow is executed only if user is member of the given group.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty group = new ProviderConfigProperty();
        group.setType(ProviderConfigProperty.GROUP_TYPE);
        group.setName(CONDITIONAL_USER_GROUP);
        group.setLabel("User group");
        group.setHelpText("Group the user should be in to execute this flow. Click 'Select Group' button to browse groups, or just type it in the textbox");

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput.setName(CONF_NEGATE);
        negateOutput.setLabel("Negate output");
        negateOutput.setHelpText("Apply a NOT to the check result. When this is true, then the condition will evaluate to true just if user is NOT in the specified group. When this is false, the condition will evaluate to true just if user is in the specified group");

        return Arrays.asList(group, negateOutput);
    }

    @Override
    public ConditionalAuthenticator getSingleton() {
        return ConditionalGroupAuthenticator.SINGLETON;
    }
}
