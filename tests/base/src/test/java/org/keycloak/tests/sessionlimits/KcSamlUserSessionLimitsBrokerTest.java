package org.keycloak.tests.sessionlimits;

import java.util.Map;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;

import org.junit.jupiter.api.Assertions;


@KeycloakIntegrationTest
public class KcSamlUserSessionLimitsBrokerTest extends AbstractUserSessionLimitsBrokerTest {

    private static final String IDP_ALIAS = "kc-saml-idp";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = ProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = ConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectPage
    IdpReviewUserProfilePage idpReviewUserProfilePage;

    @Override
    protected String getIdpAlias() {
        return IDP_ALIAS;
    }

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        idpReviewUserProfilePage.assertCurrent();
        idpReviewUserProfilePage.update("Firstname", "Lastname");
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    private static ProtocolMapperRepresentation createSamlUserPropertyMapper(
            String name, String userAttribute, String samlAttributeName, String friendlyName) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID);
        Map<String, String> config = mapper.getConfig();
        config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        return mapper;
    }

    public static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(PROVIDER_REALM);
            realm.users(UserBuilder.create(USER_LOGIN)
                    .name("Firstname", "Lastname")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD)
                    .enabled(true));

            ProtocolMapperRepresentation emailMapper = createSamlUserPropertyMapper(
                    "email", "email", "urn:oid:1.2.840.113549.1.9.1", "email");
            ProtocolMapperRepresentation firstNameMapper = createSamlUserPropertyMapper(
                    "firstName", "firstName", "urn:oid:2.5.4.42", "givenName");
            ProtocolMapperRepresentation lastNameMapper = createSamlUserPropertyMapper(
                    "lastName", "lastName", "urn:oid:2.5.4.4", "sn");

            String samlClientId = "http://localhost:8080/realms/" + CONSUMER_REALM;
            realm.clients(ClientBuilder.create(samlClientId)
                    .protocol(SamlProtocol.LOGIN_PROTOCOL)
                    .redirectUris("http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true")
                    .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                            "http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                            "http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint")
                    .attribute(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true")
                    .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username")
                    .attribute(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false")
                    .attribute(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false")
                    .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false")
                    .attribute(SamlConfigAttributes.SAML_ENCRYPT, "false")
                    .protocolMappers(emailMapper, firstNameMapper, lastNameMapper));
            return realm;
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(CONSUMER_REALM);
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL,
                            "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.SINGLE_LOGOUT_SERVICE_URL,
                            "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/saml")
                    .attribute(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT,
                            "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_RESPONSE, "true")
                    .attribute(SAMLIdentityProviderConfig.POST_BINDING_AUTHN_REQUEST, "true")
                    .attribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, "false")
                    .attribute(SAMLIdentityProviderConfig.WANT_AUTHN_REQUESTS_SIGNED, "false")
                    .attribute(SAMLIdentityProviderConfig.BACKCHANNEL_SUPPORTED, "false")
                    .attribute(SAMLIdentityProviderConfig.FORCE_AUTHN, "false")
                    .build());
            realm.clients(ClientBuilder.create(CONSUMER_CLIENT_ID)
                    .secret(CONSUMER_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*"));
            return realm;
        }
    }
}
