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


public class TrustedDeviceAuthenticator implements Authenticator, CredentialValidator<TrustedDeviceCredentialProvider> {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        TrustedDeviceCredentialModel credential = validateCookie(context.getSession(), context.getUser());
        if (credential != null) {
            context.success();
        } else {
            context.getAuthenticationSession().addRequiredAction(TrustedDeviceRegister.PROVIDER_ID);
            context.attempted();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    public TrustedDeviceCredentialModel validateCookie(KeycloakSession session, UserModel user) {
        TrustedDeviceToken deviceToken = getCookie(session);
        if (deviceToken == null) {
            return null;
        }

        return user.credentialManager().getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE)
                .map(TrustedDeviceCredentialModel::createFromCredentialModel)
                .filter(c -> c.getDeviceId().equals(deviceToken.getDeviceId()))
                .filter(c -> c.getExpireTime() > Time.currentTime())
                .findFirst()
                .orElse(null);
    }

    public TrustedDeviceToken getCookie(KeycloakSession session) {
        String tokenString = session.getProvider(CookieProvider.class).get(CookieType.TRUSTED_DEVICE);

        if (tokenString == null) {
            return null;
        }

        TrustedDeviceToken decoded = session.tokens().decode(tokenString, TrustedDeviceToken.class);
        if (decoded != null && decoded.getExp() > Time.currentTime()) {
            return decoded;
        }

        return null;
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
    }

    @Override
    public void close() {

    }

    @Override
    public TrustedDeviceCredentialProvider getCredentialProvider(KeycloakSession session) {
        return (TrustedDeviceCredentialProvider) session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.PROVIDER_ID);
    }
}
