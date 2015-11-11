package org.keycloak.authentication.authenticators.broker;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpUsernamePasswordFormFactory extends UsernamePasswordFormFactory {

    public static final String PROVIDER_ID = "idp-username-password-form";
    public static final UsernamePasswordForm IDP_SINGLETON = new IdpUsernamePasswordForm();

    @Override
    public Authenticator create(KeycloakSession session) {
        return IDP_SINGLETON;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Validates a password from login form. Username is already known from identity provider authentication";
    }

    @Override
    public String getDisplayType() {
        return "Username Password Form for identity provider reauthentication";
    }
}
