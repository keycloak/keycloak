package org.keycloak.models.workflow;

import java.net.URI;
import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.email.ActionTokenEmail;
import org.keycloak.email.EmailException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.urls.HostnameProvider;
import org.keycloak.urls.UrlType;

import org.jboss.logging.Logger;

public class InviteUserStepProvider implements WorkflowStepProvider {

    public static final String CONFIG_ACTIONS = "actions";
    public static final String CONFIG_CLIENT_ID = "client-id";
    public static final String CONFIG_REDIRECT_URI = "redirect-uri";

    static final String HOSTNAME_NOT_CONFIGURED_MESSAGE =
            "invite-user requires a configured Keycloak hostname: set --hostname=<full URL> "
                    + "or the realm 'frontendUrl' attribute";

    private static final List<String> DEFAULT_ACTIONS = List.of(
            UserModel.RequiredAction.UPDATE_PASSWORD.name(),
            UserModel.RequiredAction.VERIFY_EMAIL.name()
    );

    private static final Logger LOG = Logger.getLogger(InviteUserStepProvider.class);

    private final KeycloakSession session;
    private final ComponentModel stepModel;

    public InviteUserStepProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.stepModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(WorkflowExecutionContext context) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());
        if (user == null) {
            return;
        }

        String clientId = stepModel.getConfig().getFirst(CONFIG_CLIENT_ID);
        String redirectUri = stepModel.getConfig().getFirst(CONFIG_REDIRECT_URI);

        ActionTokenEmail.Result resolved;
        try {
            resolved = ActionTokenEmail.resolveParams(session, realm, user, clientId, redirectUri, null);
        } catch (IllegalArgumentException e) {
            LOG.warnf("Skipping invite for user %s: %s", user.getUsername(), e.getMessage());
            return;
        }
        if (resolved.getParams().isEmpty()) {
            return;
        }

        URI baseUri = resolveBaseUri(session);
        if (baseUri == null) {
            // Factory validates this, but hostname configuration can change at runtime.
            LOG.warnf("Skipping invite for user %s: %s", user.getUsername(), HOSTNAME_NOT_CONFIGURED_MESSAGE);
            return;
        }

        List<String> actions = stepModel.getConfig().getOrDefault(CONFIG_ACTIONS, DEFAULT_ACTIONS);

        try {
            ActionTokenEmail.sendInviteUser(session, realm, user, resolved.getParams().get(), actions, baseUri);
        } catch (EmailException e) {
            LOG.errorv(e, "Failed to send invite email to user {0} ({1})", user.getUsername(), user.getEmail());
        }
    }

    /**
     * Resolves the Keycloak base URI from the {@link HostnameProvider} without
     * relying on the request-scoped {@code KeycloakContext}.
     *
     * @return the base URI, or {@code null} if no static hostname is configured
     */
    static URI resolveBaseUri(KeycloakSession session) {
        try {
            return session.getProvider(HostnameProvider.class).getBaseUri(null, UrlType.FRONTEND);
        } catch (NullPointerException e) {
            LOG.debugv(e, "HostnameProvider could not resolve a base URI without an active request");
            return null;
        }
    }
}
