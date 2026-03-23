package org.keycloak.tests.broker;

import java.util.HashMap;
import java.util.List;
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
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

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
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.identityProvider(IdentityProviderBuilder.create()
                    .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .setAttribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .setAttribute(SAMLIdentityProviderConfig.ENTITY_ID, "http://localhost:8080/realms/default")
                    .setAttribute(SAMLIdentityProviderConfig.IDP_ENTITY_ID, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME)
                    .setAttribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .setAttribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml")
                    .setAttribute(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get())
                    .setAttribute(SAMLIdentityProviderConfig.SIGNATURE_ALGORITHM, SignatureAlgorithm.RSA_SHA256.name())
                    .setAttribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, Boolean.TRUE.toString())
                    .setAttribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, Boolean.TRUE.toString())
                    .setAttribute(IdentityProviderModel.METADATA_DESCRIPTOR_URL, "http://localhost:8080/realms/" + EXTERNAL_REALM_NAME + "/protocol/saml/descriptor")
                    .storeToken(true)
                    .addReadTokenRoleOnCreate(true)
                    .build());
            realm.identityProviderMapper(createMapper("email"))
                    .identityProviderMapper(createMapper("firstName"))
                    .identityProviderMapper(createMapper("lastName"));
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
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("http://localhost:8080/realms/default")
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
                    .protocolMappers(List.of(createMapper("email"), createMapper("firstName"), createMapper("lastName")));
            realm.addUser("testuser")
                    .name("Test", "User")
                    .email("test@localhost")
                    .emailVerified(Boolean.TRUE)
                    .password("password");
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
