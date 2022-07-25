package org.keycloak.testsuite.broker;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

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
    public static final String USER_INFO_CLAIM = "user-claim";
    public static final String HARDOCDED_CLAIM = "test";
    public static final String HARDOCDED_VALUE = "value";

    @Override
    public RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_PROV_NAME);
        realm.setEnabled(true);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        realm.setEventsEnabled(true);

        return realm;
    }

    @Override
    public RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_CONS_NAME);
        realm.setEnabled(true);
        realm.setResetPasswordAllowed(true);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));
        realm.setEventsEnabled(true);

        return realm;
    }

    @Override
    public List<ClientRepresentation> createProviderClients() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(getIDPClientIdInProviderRealm());
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);

        client.setRedirectUris(Collections.singletonList(getConsumerRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getConsumerRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setPostLogoutRedirectUris(Collections.singletonList("+"));

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

        ProtocolMapperRepresentation hardcodedJsonClaim = new ProtocolMapperRepresentation();
        hardcodedJsonClaim.setName("json-mapper");
        hardcodedJsonClaim.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        hardcodedJsonClaim.setProtocolMapper(HardcodedClaim.PROVIDER_ID);

        Map<String, String> hardcodedJsonClaimMapperConfig = hardcodedJsonClaim.getConfig();
        hardcodedJsonClaimMapperConfig.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, KcOidcBrokerConfiguration.USER_INFO_CLAIM);
        hardcodedJsonClaimMapperConfig.put(OIDCAttributeMapperHelper.JSON_TYPE, "JSON");
        hardcodedJsonClaimMapperConfig.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        hardcodedJsonClaimMapperConfig.put(HardcodedClaim.CLAIM_VALUE, "{\"" + HARDOCDED_CLAIM + "\": \"" + HARDOCDED_VALUE + "\"}");

        client.setProtocolMappers(Arrays.asList(emailMapper, userAttrMapper, userAttrMapper2, nestedAttrMapper, dottedAttrMapper, hardcodedJsonClaim));

        return Collections.singletonList(client);
    }

    @Override
    public List<ClientRepresentation> createConsumerClients() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("broker-app");
        client.setName("broker-app");
        client.setSecret("broker-app-secret");
        client.setEnabled(true);
        client.setDirectAccessGrantsEnabled(true);

        client.setRedirectUris(Collections.singletonList(getConsumerRoot() +
                "/auth/*"));

        client.setBaseUrl(getConsumerRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/app");

        OIDCAdvancedConfigWrapper.fromClientRepresentation(client).setPostLogoutRedirectUris(Collections.singletonList("+"));

        return Collections.singletonList(client);
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();
        applyDefaultConfiguration(config, syncMode);

        return idp;
    }

    protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
        config.put(IdentityProviderModel.SYNC_MODE, syncMode.toString());
        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");
        config.put(OIDCIdentityProviderConfig.JWKS_URL,
                getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
        config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "true");
        config.put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
    }

    @Override
    public String getUserLogin() {
        return USER_LOGIN;
    }

    @Override
    public String getIDPClientIdInProviderRealm() {
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
