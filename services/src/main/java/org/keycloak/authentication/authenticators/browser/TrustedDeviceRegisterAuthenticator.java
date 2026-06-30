package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.requiredactions.TrustedDeviceRegister;
import org.keycloak.common.util.Time;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProvider;
import org.keycloak.credential.TrustedDeviceCredentialProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;
import org.keycloak.representations.TrustedDeviceToken;

public class TrustedDeviceRegisterAuthenticator implements Authenticator, CredentialValidator<TrustedDeviceCredentialProvider> {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String tokenString = context.getSession().getProvider(CookieProvider.class).get(CookieType.TRUSTED_DEVICE);
        TrustedDeviceToken deviceToken = context.getSession().tokens().decode(tokenString, TrustedDeviceToken.class);
        if (deviceToken != null && deviceToken.getExp() > Time.currentTime()) {
            boolean tokenIsValid = context.getUser().credentialManager().getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                    .map(TrustedDeviceCredentialModel::createFromCredentialModel)
                    .anyMatch(c -> c.getDeviceId().equals(deviceToken.getDeviceId()) && c.getExpireTime() > Time.currentTime());
            if (tokenIsValid) {
                context.success();
                return;
            }
        }

        context.getUser().addRequiredAction(TrustedDeviceRegister.PROVIDER_ID);
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.credentialManager().isConfiguredFor(getCredentialProvider(session).getType());
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        user.addRequiredAction(TrustedDeviceRegister.PROVIDER_ID);
    }

    @Override
    public void close() {

    }

    @Override
    public TrustedDeviceCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (TrustedDeviceCredentialProvider) session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.PROVIDER_ID);
    }
}
