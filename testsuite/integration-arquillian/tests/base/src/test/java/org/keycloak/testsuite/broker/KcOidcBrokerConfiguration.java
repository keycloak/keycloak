package org.keycloak.testsuite.broker;

import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.*;

/**
 * @author hmlnarik
 */
public class KcOidcBrokerConfiguration implements BrokerConfiguration {

    public static final KcOidcBrokerConfiguration INSTANCE = new KcOidcBrokerConfiguration();

    protected static final String ATTRIBUTE_TO_MAP_NAME = "user-attribute";
    protected static final String ATTRIBUTE_TO_MAP_NAME_2 = "user-attribute-2";

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
        client.setClientId(getIDPClientIdInProviderRealm(suiteContext));
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot(suiteContext) +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot(suiteContext) +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        ProtocolMapperRepresentation emailMapper = new ProtocolMapperRepresentation();
        emailMapper.setName("email");
        emailMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        emailMapper.setProtocolMapper(UserPropertyMapper.PROVIDER_ID);

        Map<String, String> emailMapperConfig = emailMapper.getConfig();
        emailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "email");
        emailMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "email");
        emailMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        emailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        emailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        emailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");

        ProtocolMapperRepresentation nestedAttrMapper = new ProtocolMapperRepresentation();
        nestedAttrMapper.setName("attribute - nested claim");
        nestedAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        nestedAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

        Map<String, String> nestedEmailMapperConfig = nestedAttrMapper.getConfig();
        nestedEmailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "nested.email");
        nestedEmailMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "nested.email");
        nestedEmailMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        nestedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        nestedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        nestedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");

        ProtocolMapperRepresentation dottedAttrMapper = new ProtocolMapperRepresentation();
        dottedAttrMapper.setName("attribute - claim with dot in name");
        dottedAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        dottedAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

        Map<String, String> dottedEmailMapperConfig = dottedAttrMapper.getConfig();
        dottedEmailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "dotted.email");
        dottedEmailMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "dotted\\.email");
        dottedEmailMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        dottedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        dottedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        dottedEmailMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");

        ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
        userAttrMapper.setName("attribute - name");
        userAttrMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        userAttrMapper.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

        Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
        userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, ATTRIBUTE_TO_MAP_NAME);
        userAttrMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_NAME);
        userAttrMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        userAttrMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        userAttrMapperConfig.put(ProtocolMapperUtils.MULTIVALUED, "true");

        ProtocolMapperRepresentation userAttrMapper2 = new ProtocolMapperRepresentation();
        userAttrMapper2.setName("attribute - name - 2");
        userAttrMapper2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        userAttrMapper2.setProtocolMapper(UserAttributeMapper.PROVIDER_ID);

        Map<String, String> userAttrMapperConfig2 = userAttrMapper2.getConfig();
        userAttrMapperConfig2.put(ProtocolMapperUtils.USER_ATTRIBUTE, ATTRIBUTE_TO_MAP_NAME_2);
        userAttrMapperConfig2.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_NAME_2);
        userAttrMapperConfig2.put(OIDCAttributeMapperHelper.JSON_TYPE, ProviderConfigProperty.STRING_TYPE);
        userAttrMapperConfig2.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        userAttrMapperConfig2.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        userAttrMapperConfig2.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        userAttrMapperConfig2.put(ProtocolMapperUtils.MULTIVALUED, "true");

        client.setProtocolMappers(Arrays.asList(emailMapper, userAttrMapper, userAttrMapper2, nestedAttrMapper, dottedAttrMapper));

        return Collections.singletonList(client);
    }

    @Override
    public List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext) {
        ClientRepresentation client = new ClientRepresentation();
        client.setId("broker-app");
        client.setClientId("broker-app");
        client.setName("broker-app");
        client.setSecret("broker-app-secret");
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot(suiteContext) +
                "/auth/*"));

        client.setBaseUrl(getAuthRoot(suiteContext) +
                "/auth/realms/" + REALM_CONS_NAME + "/app");

        return Collections.singletonList(client);
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();
        applyDefaultConfiguration(suiteContext, config);

        return idp;
    }

    protected void applyDefaultConfiguration(final SuiteContext suiteContext, final Map<String, String> config) {
        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");
    }

    @Override
    public String getUserLogin() {
        return USER_LOGIN;
    }

    @Override
    public String getIDPClientIdInProviderRealm(SuiteContext suiteContext) {
        return CLIENT_ID;
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
