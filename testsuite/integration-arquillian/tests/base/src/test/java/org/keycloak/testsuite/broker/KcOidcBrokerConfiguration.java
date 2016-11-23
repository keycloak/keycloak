package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.*;

/**
 *
 * @author hmlnarik
 */
public class KcOidcBrokerConfiguration implements BrokerConfiguration {

    public static final KcOidcBrokerConfiguration INSTANCE = new KcOidcBrokerConfiguration();

    @Override
    public RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_PROV_NAME);
        realm.setEnabled(true);

        return realm;
    }

    @Override
    public RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_CONS_NAME);
        realm.setEnabled(true);

        return realm;
    }

    @Override
    public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(CLIENT_ID);
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot(suiteContext) +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot(suiteContext) +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        return Collections.singletonList(client);
    }

    @Override
    public List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext) {
        return null;
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    @Override
    public String getUserLogin() {
        return USER_LOGIN;
    }

    @Override
    public String getUserPassword() {
        return USER_PASSWORD;
    }

    @Override
    public String getUserEmail() {
        return USER_EMAIL;
    }

    @Override
    public String providerRealmName() {
        return REALM_PROV_NAME;
    }

    @Override
    public String consumerRealmName() {
        return REALM_CONS_NAME;
    }

    @Override
    public String getIDPAlias() {
        return IDP_OIDC_ALIAS;
    }

}
