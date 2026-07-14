package org.keycloak.models.workflow;

import java.util.List;
import java.util.Set;

import org.keycloak.authentication.requiredactions.util.RequiredActionsValidator;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class InviteUserStepProviderFactory implements WorkflowStepProviderFactory<InviteUserStepProvider> {

    public static final String ID = "invite-user";

    @Override
    public InviteUserStepProvider create(KeycloakSession session, ComponentModel model) {
        return new InviteUserStepProvider(session, model);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<ResourceType> getSupportedResourceTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public String getHelpText() {
        return "Sends an invitation email with an action link to the user";
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model)
            throws ComponentValidationException {
        String clientId = model.get(InviteUserStepProvider.CONFIG_CLIENT_ID);
        String redirectUri = model.get(InviteUserStepProvider.CONFIG_REDIRECT_URI);

        if (redirectUri != null && clientId == null) {
            throw new ComponentValidationException("'redirect-uri' requires 'client-id' to be set");
        }
        if (clientId != null && realm.getClientByClientId(clientId) == null) {
            throw new ComponentValidationException("Client '" + clientId + "' does not exist");
        }

        List<String> actions = model.getConfig().get(InviteUserStepProvider.CONFIG_ACTIONS);
        if (actions != null && !RequiredActionsValidator.validRequiredActions(session, actions)) {
            throw new ComponentValidationException("Invalid required action configured for 'actions'");
        }

        // Workflow execution may run outside an HTTP request, so a static hostname is required.
        if (InviteUserStepProvider.resolveBaseUri(session) == null) {
            throw new ComponentValidationException(InviteUserStepProvider.HOSTNAME_NOT_CONFIGURED_MESSAGE);
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(InviteUserStepProvider.CONFIG_ACTIONS)
                    .label("Required Actions")
                    .helpText("Required actions the user must complete after clicking the invitation link. " +
                            "Defaults to UPDATE_PASSWORD and VERIFY_EMAIL.")
                    .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                    .add()
                .property()
                    .name(InviteUserStepProvider.CONFIG_CLIENT_ID)
                    .label("Client ID")
                    .helpText("Client id to associate with the action token. Defaults to the realm's system client.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .property()
                    .name(InviteUserStepProvider.CONFIG_REDIRECT_URI)
                    .label("Redirect URI")
                    .helpText("Where to send the user after completing the required actions. " +
                            "Must be a valid redirect URI for the configured client.")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .add()
                .build();
    }
}
