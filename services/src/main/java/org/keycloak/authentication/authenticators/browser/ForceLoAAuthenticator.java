package org.keycloak.authentication.authenticators.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.Map;

import static org.keycloak.models.Constants.NO_LOA;

public class ForceLoAAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        int configuredMinLoa = new ForceLoAAuthenticatorConfig(
            authenticationFlowContext.getAuthenticatorConfig()).levelOfAuthentication();

        ClientModel client = authenticationFlowContext.getAuthenticationSession().getClient();

        int maxDefaultLoa = getMaxDefaultLoa(client);
        int enforcedLoA = Math.max(configuredMinLoa, maxDefaultLoa);

        AuthenticationSessionModel authenticationSession = authenticationFlowContext.getAuthenticationSession();
        AcrStore acrStore = new AcrStore(authenticationSession);
        int requestedLevelOfAuthentication = acrStore.getRequestedLevelOfAuthentication();
        if (requestedLevelOfAuthentication < enforcedLoA) {
            authenticationSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION,
                String.valueOf(enforcedLoA));
            authenticationSession.setClientNote(Constants.FORCE_LEVEL_OF_AUTHENTICATION, Boolean.TRUE.toString());
        }
        authenticationFlowContext.success();
    }

    private int getMaxDefaultLoa(ClientModel client) {
        int defaultLoa = NO_LOA;
        List<String> defaultAcrValues = AcrUtils.getDefaultAcrValues(client);
        Map<String, Integer> acrToLoaMap = AcrUtils.getAcrLoaMap(client);
        if (acrToLoaMap.isEmpty()) {
            acrToLoaMap = AcrUtils.getAcrLoaMap(client.getRealm());
        }
        for (String configuredAcr : defaultAcrValues) {
            int loa;
            if (acrToLoaMap.containsKey(configuredAcr)) {
                loa = acrToLoaMap.get(configuredAcr);
            } else {
                try {
                    loa = Integer.parseInt(configuredAcr);
                } catch(NumberFormatException ex) {
                    loa = NO_LOA;
                }
            }
            defaultLoa = Math.max(defaultLoa, loa);
        }
        return defaultLoa;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
    }

    @Override
    public void close() {

    }
}
