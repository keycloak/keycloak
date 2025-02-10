package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.net.URI;

import static org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticatorFactory.CONFIG_KEY_BODY_LOCALIZATION_KEY;
import static org.keycloak.authentication.authenticators.browser.DisplayInfoScreenAuthenticatorFactory.CONFIG_KEY_HEADER_LOCALIZATION_KEY;

public class DisplayInfoScreenAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        final var config = authenticationFlowContext.getAuthenticatorConfig().getConfig();
        final var headerLocalizationKey = config.get(CONFIG_KEY_HEADER_LOCALIZATION_KEY);
        final var bodyLocalizationKey = config.get(CONFIG_KEY_BODY_LOCALIZATION_KEY);

        sendInfoChallenge(authenticationFlowContext, headerLocalizationKey, bodyLocalizationKey);
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        authenticationFlowContext.success();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    private void sendInfoChallenge(final AuthenticationFlowContext context, final String headerLocalizationKey,
            final String bodyLocalizationKey) {
        final URI action = context.getActionUrl(context.generateAccessCode());

        final var challengeResponse = context.form()
                .setAuthContext(null)
                .setInfo(bodyLocalizationKey)
                .setAttribute("messageHeader", headerLocalizationKey)
                .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, action)
                .createInfoPage();
        context.challenge(challengeResponse);
    }
}
