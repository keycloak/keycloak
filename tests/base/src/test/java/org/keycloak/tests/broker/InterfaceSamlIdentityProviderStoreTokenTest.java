package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

/**
 *
 * @author rmartinc
 */
public interface InterfaceSamlIdentityProviderStoreTokenTest extends InterfaceIdentityProviderStoreTokenTest {

    @Override
    default boolean isRefreshTokenAllowed() {
        return false;
    }

    public static class IdpRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute(SAMLIdentityProviderConfig.ENTITY_ID, "http://localhost:8080/realms/default")
                    .attribute(SAMLIdentityProviderConfig.IDP_ENTITY_ID, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME)
                    .attribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                    .attribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, Boolean.TRUE.toString())
                    .attribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, Boolean.TRUE.toString())
                    .attribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())
                    .attribute(SAMLIdentityProviderConfig.SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name())
                    .attribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, Boolean.TRUE.toString())
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, Boolean.TRUE.toString())
                    .attribute(IdentityProviderModel.METADATA_DESCRIPTOR_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml/descriptor")
                    .storeToken(true)
                    .addReadTokenRoleOnCreate(true)
                    .build());
            realm.identityProviderMappers(createMapper("email"))
                    .identityProviderMappers(createMapper("firstName"))
                    .identityProviderMappers(createMapper("lastName"));
            return realm;
        }

        private IdentityProviderMapperRepresentation createMapper(String name) {
            IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
            mapper.setName(name);
            mapper.setIdentityProviderAlias(IDP_ALIAS);
            mapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
            Map<String, String> config = new HashMap<>();
            config.put(IdentityProviderModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.name());
            config.put(UserAttributeMapper.USER_ATTRIBUTE, name);
            config.put(UserAttributeMapper.ATTRIBUTE_NAME, name);
            config.put(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, name);
            config.put(UserAttributeMapper.ATTRIBUTE_NAME_FORMAT, JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name());
            mapper.setConfig(config);
            return mapper;
        }
    }

    public static class ExternalRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create("http://localhost:8080/realms/default")
                    .name("saml-client")
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .adminUrl("http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint")
                    .redirectUris("http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint/*")
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name())
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                    .attribute(SamlConfigAttributes.SAML_USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                    .attribute(SamlConfigAttributes.SAML_METADATA_DESCRIPTOR_URL, "http://localhost:8080/realms/default/broker/" + IDP_ALIAS + "/endpoint/descriptor")
                    .protocolMappers(createMapper("email"), createMapper("firstName"), createMapper("lastName")));
            realm.users(UserBuilder.create("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password"));
            return realm;
        }

        private ProtocolMapperRepresentation createMapper(String name) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);
            mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            Map<String, String> config = new HashMap<>();
            config.put(ProtocolMapperUtils.USER_ATTRIBUTE, name);
            config.put(AttributeStatementHelper.FRIENDLY_NAME, name);
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, name);
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
            mapper.setConfig(config);
            return mapper;
        }
    }
}
