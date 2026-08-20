package org.keycloak.tests.broker;

import java.util.Map;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;

public interface OidcBrokerConfigSupport extends BrokerConfigSupport {

    String IDP_OIDC_ALIAS = "kc-oidc-idp";
    String IDP_OIDC_PROVIDER_ID = OIDCIdentityProviderFactory.PROVIDER_ID;
    String CLIENT_ID = "brokerapp";
    String CLIENT_SECRET = "secret";
    String ATTRIBUTE_TO_MAP_NAME = "user-attribute";
    String ATTRIBUTE_TO_MAP_NAME_2 = "user-attribute-2";

    @Override
    default String getIdpAlias() {
        return IDP_OIDC_ALIAS;
    }

    @BeforeEach
    default void configureBrokerEndpoints() {
        String providerBaseUrl = getProviderRealm().getBaseUrl();
        IdentityProviderRepresentation idp = getConsumerRealm().admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        Map<String, String> config = idp.getConfig();
        config.put(OIDCIdentityProviderConfig.ISSUER, providerBaseUrl);
        config.put("authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth");
        config.put(TOKEN_ENDPOINT_URL, providerBaseUrl + "/protocol/openid-connect/token");
        config.put("logoutUrl", providerBaseUrl + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo");
        config.put(OIDCIdentityProviderConfig.JWKS_URL, providerBaseUrl + "/protocol/openid-connect/certs");
        config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "true");
        config.put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        getConsumerRealm().admin().identityProviders().get(getIdpAlias()).update(idp);
    }

    static IdentityProviderBuilder createOidcIdentityProvider() {
        return IdentityProviderBuilder.create()
                .providerId(IDP_OIDC_PROVIDER_ID)
                .alias(IDP_OIDC_ALIAS)
                .displayName("kc-oidc-idp")
                .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .attribute("clientId", CLIENT_ID)
                .attribute("clientSecret", CLIENT_SECRET)
                .attribute("backchannelSupported", "true")
                .attribute("defaultScope", "email profile");
    }

    static ClientBuilder createDefaultProviderClient() {
        return ClientBuilder.create(CLIENT_ID)
                .secret(CLIENT_SECRET)
                .redirectUris("*")
                .protocolMappers(
                        ProtocolMapperBuilder.create().name("email")
                                .protocolMapper(UserAttributeMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "email")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("user.attribute", "email")
                                .build(),
                        ProtocolMapperBuilder.create().name("nested.email")
                                .protocolMapper(UserAttributeMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "nested.email")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("user.attribute", "nested.email")
                                .build(),
                        ProtocolMapperBuilder.create().name("dotted.email")
                                .protocolMapper(UserAttributeMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "dotted\\.email")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("user.attribute", "dotted.email")
                                .build(),
                        ProtocolMapperBuilder.create().name(ATTRIBUTE_TO_MAP_NAME)
                                .protocolMapper(UserAttributeMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_NAME)
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("user.attribute", ATTRIBUTE_TO_MAP_NAME)
                                .build(),
                        ProtocolMapperBuilder.create().name(ATTRIBUTE_TO_MAP_NAME_2)
                                .protocolMapper(UserAttributeMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, ATTRIBUTE_TO_MAP_NAME_2)
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("user.attribute", ATTRIBUTE_TO_MAP_NAME_2)
                                .build(),
                        ProtocolMapperBuilder.create().name("hardcoded-attribute")
                                .protocolMapper(HardcodedClaim.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "hardcoded-attribute")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                .config("claim.value", "hardcoded-value")
                                .build(),
                        ProtocolMapperBuilder.create().name("audience")
                                .protocolMapper(AudienceProtocolMapper.PROVIDER_ID)
                                .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                .config("included.custom.audience", CLIENT_ID)
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                .build());
    }

    static RealmBuilder configureProviderRealm(RealmBuilder realm, ClientBuilder providerClient) {
        UserBuilder user = UserBuilder.create(USER_LOGIN)
                .email(USER_EMAIL)
                .emailVerified(true)
                .password(USER_PASSWORD)
                .enabled(true)
                .firstName("First")
                .lastName("Last");
        return realm.name(PROVIDER_REALM)
                .eventsListeners("jboss-logging")
                .users(user)
                .clients(providerClient);
    }

    static RealmBuilder configureConsumerRealm(RealmBuilder realm, IdentityProviderBuilder idpBuilder) {
        return realm.name(CONSUMER_REALM)
                .eventsListeners("jboss-logging")
                .resetPasswordAllowed(true)
                .identityProviders(idpBuilder.build())
                .clients(ClientBuilder.create(CONSUMER_BROKER_APP_CLIENT_ID)
                        .name(CONSUMER_BROKER_APP_CLIENT_ID)
                        .secret(CONSUMER_BROKER_APP_SECRET)
                        .directAccessGrantsEnabled()
                        .redirectUris("*"));
    }

    class OidcProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureProviderRealm(realm, createDefaultProviderClient());
        }
    }

    class OidcConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm, createOidcIdentityProvider());
        }
    }
}
