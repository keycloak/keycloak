package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ImpersonationConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrateTo1_5_0 {
    public static final ModelVersion VERSION = new ModelVersion("1.5.0");

    public void migrate(KeycloakSession session) {
        List<RealmModel> realms = session.realms().getRealms();
        for (RealmModel realm : realms) {
            DefaultAuthenticationFlows.migrateFlows(realm); // add reset credentials flo
            realm.setOTPPolicy(OTPPolicy.DEFAULT_POLICY);
            realm.setBrowserFlow(realm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW));
            realm.setRegistrationFlow(realm.getFlowByAlias(DefaultAuthenticationFlows.REGISTRATION_FLOW));
            realm.setDirectGrantFlow(realm.getFlowByAlias(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW));

            AuthenticationFlowModel resetFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
            if (resetFlow == null) {
                DefaultAuthenticationFlows.resetCredentialsFlow(realm);
            } else {
                realm.setResetCredentialsFlow(resetFlow);
            }

            AuthenticationFlowModel clientAuthFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.CLIENT_AUTHENTICATION_FLOW);
            if (clientAuthFlow == null) {
                DefaultAuthenticationFlows.clientAuthFlow(realm);
            } else {
                realm.setClientAuthenticationFlow(clientAuthFlow);
            }

            for (ClientModel client : realm.getClients()) {
                client.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
            }
        }

    }
}
