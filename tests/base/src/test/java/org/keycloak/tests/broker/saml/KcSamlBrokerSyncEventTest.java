package org.keycloak.tests.broker.saml;

import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.IdentityProviderMapperBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.broker.AbstractBrokerSyncEventTest;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.POST_BINDING_RESPONSE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.VALIDATE_SIGNATURE;
import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED;

/**
 * Unlike OIDC, SAML does not send the first and last name in standard attributes, so the consumer maps them with
 * identity provider mappers, like the {@code department} attribute.
 */
@KeycloakIntegrationTest
public class KcSamlBrokerSyncEventTest extends AbstractBrokerSyncEventTest {

    private static final String IDP_ALIAS = "kc-saml-idp";
    private static final String CONSUMER_ENTITY_ID = SERVER_ROOT + "/realms/" + CONSUMER_REALM;
    private static final String CONSUMER_BROKER_ENDPOINT = CONSUMER_ENTITY_ID + "/broker/" + IDP_ALIAS + "/endpoint";

    @InjectRealm(ref = PROVIDER_REALM, config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = CONSUMER_REALM, config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

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
        return IDP_ALIAS;
    }

    private static ProtocolMapperRepresentation createAttributeStatementMapper(String protocolMapper, String userAttribute,
            String samlAttributeName, String nameFormat) {
        return ProtocolMapperBuilder.create()
                .name(userAttribute)
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .protocolMapper(protocolMapper)
                .config(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute)
                .config(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName)
                .config(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat)
                .build();
    }

    private static IdentityProviderMapperBuilder createAttributeImporter(String attribute) {
        return createIdpMapper(IDP_ALIAS, attribute, UserAttributeMapper.PROVIDER_ID)
                .attribute(UserAttributeMapper.ATTRIBUTE_NAME, attribute)
                .attribute(UserAttributeMapper.USER_ATTRIBUTE, attribute);
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .clients(ClientBuilder.create(CONSUMER_ENTITY_ID)
                            .protocol(SamlProtocol.LOGIN_PROTOCOL)
                            .redirectUris(CONSUMER_BROKER_ENDPOINT)
                            .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, CONSUMER_BROKER_ENDPOINT)
                            .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, CONSUMER_BROKER_ENDPOINT)
                            .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                            .attribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true")
                            .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                            .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
                            .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                            .attribute(SamlConfigAttributes.SAML_ENCRYPT, "false")
                            .protocolMappers(
                                    createAttributeStatementMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID,
                                            UserModel.EMAIL, "urn:oid:1.2.840.113549.1.9.1",
                                            "urn:oasis:names:tc:SAML:2.0:attrname-format:uri"),
                                    createAttributeStatementMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID,
                                            UserModel.FIRST_NAME, UserModel.FIRST_NAME, AttributeStatementHelper.BASIC),
                                    createAttributeStatementMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID,
                                            UserModel.LAST_NAME, UserModel.LAST_NAME, AttributeStatementHelper.BASIC),
                                    createAttributeStatementMapper(UserAttributeStatementMapper.PROVIDER_ID,
                                            DEPARTMENT, DEPARTMENT, AttributeStatementHelper.BASIC)))
                    .users(createProviderUser());
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            String providerSamlEndpoint = SERVER_ROOT + "/realms/" + PROVIDER_REALM + "/protocol/saml";

            return realm.name(CONSUMER_REALM)
                    .identityProviders(IdentityProviderBuilder.create()
                            .alias(IDP_ALIAS)
                            .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                            .enabled(true)
                            .trustEmail(true)
                            .attribute(SINGLE_SIGN_ON_SERVICE_URL, providerSamlEndpoint)
                            .attribute(SINGLE_LOGOUT_SERVICE_URL, providerSamlEndpoint)
                            .attribute(NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified")
                            .attribute(POST_BINDING_RESPONSE, "true")
                            .attribute(POST_BINDING_AUTHN_REQUEST, "true")
                            .attribute(VALIDATE_SIGNATURE, "false")
                            .attribute(WANT_AUTHN_REQUESTS_SIGNED, "false")
                            .attribute(BACKCHANNEL_SUPPORTED, "false")
                            .attribute(IdentityProviderModel.SYNC_MODE, IdentityProviderSyncMode.FORCE.name()))
                    .identityProviderMappers(
                            createAttributeImporter(UserModel.FIRST_NAME),
                            createAttributeImporter(UserModel.LAST_NAME),
                            createAttributeImporter(DEPARTMENT));
        }
    }
}
