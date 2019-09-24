/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.broker;

import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.ClientBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.*;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.*;

public class KcSamlBrokerConfiguration implements BrokerConfiguration {

    public static final KcSamlBrokerConfiguration INSTANCE = new KcSamlBrokerConfiguration();

    @Override
    public RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_PROV_NAME);

        return realm;
    }

    @Override
    public RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_CONS_NAME);

        return realm;
    }

    @Override
    public List<ClientRepresentation> createProviderClients(SuiteContext suiteContext) {
        String clientId = getIDPClientIdInProviderRealm(suiteContext);
        return Arrays.asList(createProviderClient(suiteContext, clientId));
    }

    private ClientRepresentation createProviderClient(SuiteContext suiteContext, String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setEnabled(true);
        client.setProtocol(IDP_SAML_PROVIDER_ID);
        client.setRedirectUris(Collections.singletonList(
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint"
        ));

        Map<String, String> attributes = new HashMap<>();

        attributes.put(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        attributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true");
        attributes.put(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        attributes.put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false");
        attributes.put(SamlConfigAttributes.SAML_ENCRYPT, "false");

        client.setAttributes(attributes);

        ProtocolMapperRepresentation emailMapper = new ProtocolMapperRepresentation();
        emailMapper.setName("email");
        emailMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        emailMapper.setProtocolMapper(UserPropertyAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> emailMapperConfig = emailMapper.getConfig();
        emailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "email");
        emailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "urn:oid:1.2.840.113549.1.9.1");
        emailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        emailMapperConfig.put(AttributeStatementHelper.FRIENDLY_NAME, "email");

        ProtocolMapperRepresentation dottedAttrMapper = new ProtocolMapperRepresentation();
        dottedAttrMapper.setName("email - dotted");
        dottedAttrMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        dottedAttrMapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> dottedEmailMapperConfig = dottedAttrMapper.getConfig();
        dottedEmailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "dotted.email");
        dottedEmailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "dotted.email");
        dottedEmailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");

        ProtocolMapperRepresentation nestedAttrMapper = new ProtocolMapperRepresentation();
        nestedAttrMapper.setName("email - nested");
        nestedAttrMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        nestedAttrMapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> nestedEmailMapperConfig = nestedAttrMapper.getConfig();
        nestedEmailMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, "nested.email");
        nestedEmailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "nested.email");
        nestedEmailMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");

        ProtocolMapperRepresentation userAttrMapper = new ProtocolMapperRepresentation();
        userAttrMapper.setName("attribute - name");
        userAttrMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        userAttrMapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> userAttrMapperConfig = userAttrMapper.getConfig();
        userAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME);
        userAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME);
        userAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        userAttrMapperConfig.put(AttributeStatementHelper.FRIENDLY_NAME, "");

        ProtocolMapperRepresentation userFriendlyAttrMapper = new ProtocolMapperRepresentation();
        userFriendlyAttrMapper.setName("attribute - friendly name");
        userFriendlyAttrMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        userFriendlyAttrMapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> userFriendlyAttrMapperConfig = userFriendlyAttrMapper.getConfig();
        userFriendlyAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, AbstractUserAttributeMapperTest.ATTRIBUTE_TO_MAP_FRIENDLY_NAME);
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "urn:oid:1.2.3.4.5.6.7");
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.FRIENDLY_NAME, AbstractUserAttributeMapperTest.ATTRIBUTE_TO_MAP_FRIENDLY_NAME);

        client.setProtocolMappers(Arrays.asList(emailMapper, dottedAttrMapper, nestedAttrMapper, userAttrMapper, userFriendlyAttrMapper));

        return client;
    }

    @Override
    public List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext) {
        return Arrays.asList(
          ClientBuilder.create()
            .clientId(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST)
            .enabled(true)
            .fullScopeEnabled(true)
            .protocol(SamlProtocol.LOGIN_PROTOCOL)
            .baseUrl("http://localhost:8080/sales-post")
            .addRedirectUri("http://localhost:8180/sales-post/*")
            .addRedirectUri("https://localhost:8543/sales-post/*")
            .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
            .build(),
          ClientBuilder.create()
            .clientId(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted")
            .enabled(true)
            .fullScopeEnabled(true)
            .protocol(SamlProtocol.LOGIN_PROTOCOL)
            .baseUrl("http://localhost:8080/sales-post")
            .addRedirectUri("http://localhost:8180/sales-post/*")
            .addRedirectUri("https://localhost:8543/sales-post/*")
            .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
            .attribute(SAML_IDP_INITIATED_SSO_URL_NAME, "sales-post")
            .attribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "https://localhost:8180/sales-post/saml")
            .build(),
          ClientBuilder.create()
            .id("broker-app")
            .clientId("broker-app")
            .name("broker-app")
            .secret("broker-app-secret")
            .enabled(true)
            .directAccessGrants()
            .addRedirectUri(getAuthRoot(suiteContext) + "/auth/*")
            .baseUrl(getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/app")
            .build()
        );
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_SAML_ALIAS, IDP_SAML_PROVIDER_ID);

        idp.setTrustEmail(true);
        idp.setAddReadTokenRoleOnCreate(true);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put(SINGLE_SIGN_ON_SERVICE_URL, getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put(SINGLE_LOGOUT_SERVICE_URL, getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put(NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        config.put(FORCE_AUTHN, "false");
        config.put(POST_BINDING_RESPONSE, "true");
        config.put(POST_BINDING_AUTHN_REQUEST, "true");
        config.put(VALIDATE_SIGNATURE, "false");
        config.put(WANT_AUTHN_REQUESTS_SIGNED, "false");
        config.put(BACKCHANNEL_SUPPORTED, "false");

        return idp;
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
    public String getIDPClientIdInProviderRealm(SuiteContext suiteContext) {
        return getAuthRoot(suiteContext) + "/auth/realms/" + consumerRealmName();
    }

    @Override
    public String getUserLogin() {
        return USER_LOGIN;
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
    public String getIDPAlias() {
        return IDP_SAML_ALIAS;
    }
}
