package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.*;

public class KcOidcBrokerTest extends AbstractBrokerTest {

    @Override
    protected RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_PROV_NAME);
        realm.setEnabled(true);

        return realm;
    }

    @Override
    protected RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_CONS_NAME);
        realm.setEnabled(true);

        return realm;
    }

    @Override
    protected List<ClientRepresentation> createProviderClients() {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(CLIENT_ID);
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        return Collections.singletonList(client);
    }

    @Override
    protected List<ClientRepresentation> createConsumerClients() {
        return null;
    }

    @Override
    protected IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    @Override
    protected String getUserLogin() {
        return USER_LOGIN;
    }

    @Override
    protected String getUserPassword() {
        return USER_PASSWORD;
    }

    @Override
    protected String getUserEmail() {
        return USER_EMAIL;
    }

    @Override
    protected String providerRealmName() {
        return REALM_PROV_NAME;
    }

    @Override
    protected String consumerRealmName() {
        return REALM_CONS_NAME;
    }

    @Override
    protected String getIDPAlias() {
        return IDP_OIDC_ALIAS;
    }

}
