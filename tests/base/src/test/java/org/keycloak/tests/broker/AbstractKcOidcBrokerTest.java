package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;

/**
 * Shared broker configuration that mirrors the legacy {@code KcOidcBrokerConfiguration}: the consumer
 * realm brokers to the provider through the Keycloak-specific {@code keycloak-oidc} identity provider.
 * This is the default for the OIDC broker tests. Tests that need the generic {@code oidc} provider
 * (as the legacy suite did in a couple of cases) can extend this with a generic-provider variant.
 */
public abstract class AbstractKcOidcBrokerTest extends AbstractBrokerLoginTest {

    protected static final String IDP_OIDC_ALIAS = "kc-oidc-idp";
    protected static final String IDP_OIDC_PROVIDER_ID = KeycloakOIDCIdentityProviderFactory.PROVIDER_ID;
    protected static final String CLIENT_ID = "brokerapp";
    protected static final String CLIENT_SECRET = "secret";
    protected static final String ATTRIBUTE_TO_MAP_NAME = "user-attribute";
    protected static final String ATTRIBUTE_TO_MAP_NAME_2 = "user-attribute-2";
    protected static final String USER_INFO_CLAIM = "user-claim";
    protected static final String HARDCODED_CLAIM = "test";
    protected static final String HARDCODED_VALUE = "value";

    // Default provider/consumer realms for the common case. Tests that need a different config shadow just
    // the field they vary (same name/type, different @InjectRealm#config) - the framework resolves both the
    // shadowed and inherited field to the same deployed realm, so no getter override is needed either way.
    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = OidcProviderRealmConfig.class)
    protected ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = OidcConsumerRealmConfig.class)
    protected ManagedRealm consumerRealm;

    @Override
    protected ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected String getIdpAlias() {
        return IDP_OIDC_ALIAS;
    }

    @BeforeEach
    void configureBrokerEndpoints() {
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

        // Provider-side client represents the consumer: its broker callback endpoints
        // (redirect/admin/backchannel-logout) must point at the consumer's actual base URL, mirroring the
        // legacy KcOidcBrokerConfiguration and the SAML base's configureSamlBrokerEndpoints(). Matched by
        // its seeded placeholder redirect URI rather than by CLIENT_ID ("brokerapp"), since some tests
        // (e.g. colon-alias client IDs) create the provider client under a different clientId.
        String consumerBaseUrl = getConsumerRealm().getBaseUrl();
        String consumerBrokerEndpoint = consumerBaseUrl + "/broker/" + getIdpAlias() + "/endpoint";
        String placeholderRedirectUri = "http://localhost:8080/broker/" + getIdpAlias() + "/endpoint/*";
        ClientsResource providerClients = getProviderRealm().admin().clients();
        List<ClientRepresentation> found = providerClients.findAll().stream()
                .filter(c -> c.getRedirectUris() != null && c.getRedirectUris().contains(placeholderRedirectUri))
                .toList();
        if (!found.isEmpty()) {
            ClientRepresentation client = found.get(0);
            client.setRedirectUris(List.of(consumerBrokerEndpoint + "/*"));
            client.setAdminUrl(consumerBrokerEndpoint);
            Map<String, String> attributes = client.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
                client.setAttributes(attributes);
            }
            attributes.put(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
                    consumerBaseUrl + "/protocol/openid-connect/logout/backchannel-logout");
            providerClients.get(client.getId()).update(client);
        }
    }

    protected static IdentityProviderBuilder createOidcIdentityProvider() {
        return IdentityProviderBuilder.create()
                .providerId(IDP_OIDC_PROVIDER_ID)
                .alias(IDP_OIDC_ALIAS)
                .displayName("kc-oidc-idp")
                .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .attribute("clientId", CLIENT_ID)
                .attribute("clientSecret", CLIENT_SECRET)
                .attribute("prompt", "login")
                .attribute("loginHint", "true")
                .attribute("backchannelSupported", "true")
                .attribute("defaultScope", "email profile");
    }

    protected static RealmBuilder configureConsumerRealm(RealmBuilder realm, IdentityProviderBuilder idpBuilder) {
        // The broker-app client is created by the injected OAuthClient (see BrokerAppClientConfig), so it is
        // not declared here.
        return realm.name(CONSUMER_REALM)
                .eventsListeners("jboss-logging")
                .resetPasswordAllowed(true)
                .identityProviders(idpBuilder.build());
    }

    static class OidcProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    // The provider user has no first/last name on purpose: the imported consumer user is
                    // then incomplete, so the consumer's first-broker-login review-profile page appears
                    // (mirroring the legacy suite, which also created the provider user without those names).
                    .users(UserBuilder.create(USER_LOGIN)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .password(USER_PASSWORD)
                            .enabled(true))
                    // The broker callback endpoints (redirect/admin/backchannel-logout) are seeded here with
                    // a placeholder host/port; configureBrokerEndpoints() rewrites them to the consumer
                    // realm's actual base URL once it is known. post.logout.redirect.uris="+" needs no rewrite.
                    .clients(ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("http://localhost:8080/broker/" + IDP_OIDC_ALIAS + "/endpoint/*")
                            .adminUrl("http://localhost:8080/broker/" + IDP_OIDC_ALIAS + "/endpoint")
                            .attribute(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
                                    "http://localhost:8080/protocol/openid-connect/logout/backchannel-logout")
                            .attribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+")
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
                                            .config(ProtocolMapperUtils.MULTIVALUED, "true")
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
                                            .config(ProtocolMapperUtils.MULTIVALUED, "true")
                                            .build(),
                                    // Legacy KcOidcBrokerConfiguration's "json-mapper": a hardcoded claim whose
                                    // value is a JSON object ({"test":"value"}) exposed under the "user-claim"
                                    // claim, included only in the ID token. Broker mapper tests rely on this
                                    // exact shape, so mirror it rather than a scalar hardcoded string.
                                    ProtocolMapperBuilder.create().name("json-mapper")
                                            .protocolMapper(HardcodedClaim.PROVIDER_ID)
                                            .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                            .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, USER_INFO_CLAIM)
                                            .config(OIDCAttributeMapperHelper.JSON_TYPE, "JSON")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                            .config(HardcodedClaim.CLAIM_VALUE,
                                                    "{\"" + HARDCODED_CLAIM + "\": \"" + HARDCODED_VALUE + "\"}")
                                            .build(),
                                    ProtocolMapperBuilder.create().name("audience")
                                            .protocolMapper(AudienceProtocolMapper.PROVIDER_ID)
                                            .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                            .config("included.custom.audience", CLIENT_ID)
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true")
                                            .build()));
        }
    }

    static class OidcConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm, createOidcIdentityProvider());
        }
    }
}
