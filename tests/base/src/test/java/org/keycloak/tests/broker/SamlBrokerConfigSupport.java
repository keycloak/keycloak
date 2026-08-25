package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.ARTIFACT_BINDING_RESPONSE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.ARTIFACT_RESOLUTION_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.FORCE_AUTHN;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.POST_BINDING_RESPONSE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_SIGNATURE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME;

public interface SamlBrokerConfigSupport extends BrokerConfigSupport {

    String IDP_SAML_ALIAS = "kc-saml-idp";
    String IDP_SAML_PROVIDER_ID = "saml";
    String SAML_CLIENT_ID_SALES_POST = "http://localhost:8080/sales-post/";
    String ATTRIBUTE_TO_MAP_NAME = "user-attribute";
    String ATTRIBUTE_TO_MAP_NAME_2 = "user-attribute-2";
    String ATTRIBUTE_TO_MAP_FRIENDLY_NAME = "user-attribute-friendly";

    // Placeholder SAML entity id for the provider-side client that represents the consumer realm. The
    // realm is built before its runtime base URL is known, so this is created with a default host/port
    // and then rewritten to the actual consumer base URL in configureSamlBrokerEndpoints().
    String CONSUMER_SAML_ENTITY_ID = "http://localhost:8080/realms/" + CONSUMER_REALM;

    @Override
    default String getIdpAlias() {
        return IDP_SAML_ALIAS;
    }

    // Mirrors the OIDC support's configureBrokerEndpoints(): the SAML broker endpoints and the consumer's
    // SAML entity id depend on the runtime realm base URLs, which are only available once the realms exist.
    // Rewrite them here from ManagedRealm#getBaseUrl() instead of hard-coding localhost:8080, so the tests
    // hold under non-default host/port/scheme (as the legacy suite did via getConsumerRoot()/getProviderRoot()).
    @BeforeEach
    default void configureSamlBrokerEndpoints() {
        String providerSamlEndpoint = getProviderRealm().getBaseUrl() + "/protocol/saml";
        String consumerBaseUrl = getConsumerRealm().getBaseUrl();
        String consumerBrokerEndpoint = consumerBaseUrl + "/broker/" + IDP_SAML_ALIAS + "/endpoint";

        // Consumer IdP -> provider realm SAML endpoint.
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        Map<String, String> idpConfig = idp.getConfig();
        idpConfig.put(SINGLE_SIGN_ON_SERVICE_URL, providerSamlEndpoint);
        idpConfig.put(ARTIFACT_RESOLUTION_SERVICE_URL, providerSamlEndpoint);
        idpConfig.put(SINGLE_LOGOUT_SERVICE_URL, providerSamlEndpoint);
        idpResource.update(idp);

        // Provider-side client representing the consumer: its SAML entity id and broker endpoints must
        // match the consumer's actual base URL.
        ClientsResource providerClients = getProviderRealm().admin().clients();
        List<ClientRepresentation> found = providerClients.findByClientId(CONSUMER_SAML_ENTITY_ID);
        if (!found.isEmpty()) {
            ClientRepresentation client = found.get(0);
            client.setClientId(consumerBaseUrl);
            client.setRedirectUris(List.of(consumerBrokerEndpoint));
            Map<String, String> attributes = client.getAttributes();
            attributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, consumerBrokerEndpoint);
            attributes.put(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, consumerBrokerEndpoint);
            providerClients.get(client.getId()).update(client);
        }
    }

    static ProtocolMapperRepresentation createSamlProtocolMapper(String name, String protocolMapper,
            String userAttribute, String samlAttributeName, String nameFormat, String friendlyName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(protocolMapper);
        Map<String, String> config = mapper.getConfig();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        if (friendlyName != null) {
            config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        }
        return mapper;
    }

    class SamlProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // Created with the placeholder entity id/endpoints; configureSamlBrokerEndpoints() rewrites
            // them to the consumer realm's actual base URL once it is known.
            String samlClientId = CONSUMER_SAML_ENTITY_ID;
            String consumerBrokerEndpoint = CONSUMER_SAML_ENTITY_ID + "/broker/" + IDP_SAML_ALIAS + "/endpoint";

            Map<String, String> clientAttributes = new HashMap<>();
            clientAttributes.put(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
            clientAttributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, consumerBrokerEndpoint);
            clientAttributes.put(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, consumerBrokerEndpoint);
            clientAttributes.put(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true");
            clientAttributes.put(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
            clientAttributes.put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
            clientAttributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
            clientAttributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false");
            clientAttributes.put(SamlConfigAttributes.SAML_ENCRYPT, "false");

            ClientBuilder samlClient = ClientBuilder.create(samlClientId)
                    .enabled(true)
                    .protocol(IDP_SAML_PROVIDER_ID)
                    .redirectUris(consumerBrokerEndpoint)
                    .protocolMappers(
                            createSamlProtocolMapper("email", UserPropertyAttributeStatementMapper.PROVIDER_ID,
                                    "email", "urn:oid:1.2.840.113549.1.9.1",
                                    "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", "email"),
                            createSamlProtocolMapper("email - dotted", UserAttributeStatementMapper.PROVIDER_ID,
                                    "dotted.email", "dotted.email",
                                    "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", null),
                            createSamlProtocolMapper("email - nested", UserAttributeStatementMapper.PROVIDER_ID,
                                    "nested.email", "nested.email",
                                    "urn:oasis:names:tc:SAML:2.0:attrname-format:uri", null),
                            createSamlProtocolMapper("attribute - name", UserAttributeStatementMapper.PROVIDER_ID,
                                    ATTRIBUTE_TO_MAP_NAME, ATTRIBUTE_TO_MAP_NAME,
                                    AttributeStatementHelper.BASIC, ""),
                            createSamlProtocolMapper("attribute - name 2", UserAttributeStatementMapper.PROVIDER_ID,
                                    ATTRIBUTE_TO_MAP_NAME_2, ATTRIBUTE_TO_MAP_NAME_2,
                                    AttributeStatementHelper.BASIC, ""),
                            createSamlProtocolMapper("attribute - friendly name", UserAttributeStatementMapper.PROVIDER_ID,
                                    ATTRIBUTE_TO_MAP_FRIENDLY_NAME, "urn:oid:1.2.3.4.5.6.7",
                                    AttributeStatementHelper.BASIC, ATTRIBUTE_TO_MAP_FRIENDLY_NAME));

            for (Map.Entry<String, String> attr : clientAttributes.entrySet()) {
                samlClient.attribute(attr.getKey(), attr.getValue());
            }

            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    // No first/last name on the provider user: the imported consumer user stays incomplete
                    // so the consumer's first-broker-login review-profile page appears, as in the legacy suite.
                    .users(UserBuilder.create(USER_LOGIN)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .password(USER_PASSWORD)
                            .enabled(true))
                    .clients(samlClient);
        }
    }

    class SamlConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            IdentityProviderBuilder idpBuilder = IdentityProviderBuilder.create()
                    .providerId(IDP_SAML_PROVIDER_ID)
                    .alias(IDP_SAML_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute(SINGLE_SIGN_ON_SERVICE_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(ARTIFACT_RESOLUTION_SERVICE_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(SINGLE_LOGOUT_SERVICE_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
                    .attribute(FORCE_AUTHN, "false")
                    .attribute(POST_BINDING_RESPONSE, "true")
                    .attribute(POST_BINDING_AUTHN_REQUEST, "true")
                    .attribute(VALIDATE_SIGNATURE, "false")
                    .attribute(WANT_AUTHN_REQUESTS_SIGNED, "false")
                    .attribute(BACKCHANNEL_SUPPORTED, "false")
                    .attribute(ARTIFACT_BINDING_RESPONSE, "false")
                    .trustEmail(true)
                    .storeToken(true);

            return realm.name(CONSUMER_REALM)
                    .eventsListeners("jboss-logging")
                    .resetPasswordAllowed(true)
                    .identityProviders(idpBuilder.build())
                    .clients(
                            ClientBuilder.create(SAML_CLIENT_ID_SALES_POST)
                                    .enabled(true)
                                    .fullScopeEnabled(true)
                                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                                    .baseUrl("http://localhost:8080/sales-post")
                                    .redirectUris("http://localhost:8080/sales-post/*")
                                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
                                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE),
                            ClientBuilder.create(SAML_CLIENT_ID_SALES_POST + ".dot/ted")
                                    .enabled(true)
                                    .fullScopeEnabled(true)
                                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                                    .baseUrl("http://localhost:8080/sales-post")
                                    .redirectUris("http://localhost:8080/sales-post/*")
                                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
                                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                                    .attribute(SAML_IDP_INITIATED_SSO_URL_NAME, "sales-post")
                                    .attribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://localhost:8080/sales-post/saml"),
                            ClientBuilder.create(CONSUMER_BROKER_APP_CLIENT_ID)
                                    .name(CONSUMER_BROKER_APP_CLIENT_ID)
                                    .secret(CONSUMER_BROKER_APP_SECRET)
                                    .directAccessGrantsEnabled()
                                    .redirectUris("*"));
        }
    }
}
