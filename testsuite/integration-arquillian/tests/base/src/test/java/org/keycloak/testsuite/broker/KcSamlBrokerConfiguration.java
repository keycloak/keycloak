/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.broker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
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
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.ClientBuilder;

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
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_CONS_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_LOGIN;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_PASSWORD;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;

public class KcSamlBrokerConfiguration implements BrokerConfiguration {

    public static final KcSamlBrokerConfiguration INSTANCE = new KcSamlBrokerConfiguration();
    public static final String ATTRIBUTE_TO_MAP_FRIENDLY_NAME = "user-attribute-friendly";

    private final boolean loginHint;

    public KcSamlBrokerConfiguration() {
        this(false);
    }

    public KcSamlBrokerConfiguration(boolean loginHint) {
        this.loginHint = loginHint;
    }

    @Override
    public RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_PROV_NAME);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));

        return realm;
    }

    @Override
    public RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setEnabled(true);
        realm.setRealm(REALM_CONS_NAME);
        realm.setResetPasswordAllowed(true);
        realm.setEventsListeners(Arrays.asList("jboss-logging", "event-queue"));

        return realm;
    }

    @Override
    public List<ClientRepresentation> createProviderClients() {
        String clientId = getIDPClientIdInProviderRealm();
        return new LinkedList<>(Collections.singleton(createProviderClient(clientId)));
    }

    private ClientRepresentation createProviderClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setEnabled(true);
        client.setProtocol(IDP_SAML_PROVIDER_ID);
        client.setRedirectUris(Collections.singletonList(
                getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint"
        ));

        Map<String, String> attributes = new HashMap<>();

        attributes.put(SamlConfigAttributes.SAML_AUTHNSTATEMENT, "true");
        attributes.put(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE,
                getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE,
                getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put(SamlConfigAttributes.SAML_FORCE_NAME_ID_FORMAT_ATTRIBUTE, "true");
        attributes.put(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, "username");
        attributes.put(SamlConfigAttributes.SAML_ASSERTION_SIGNATURE, "false");
        attributes.put(SamlConfigAttributes.SAML_SERVER_SIGNATURE, "false");
        attributes.put(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, "false");
        attributes.put(SamlConfigAttributes.SAML_ENCRYPT, "false");
        attributes.put(IdentityProviderModel.LOGIN_HINT, String.valueOf(loginHint));

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

        ProtocolMapperRepresentation userAttrMapper2 = new ProtocolMapperRepresentation();
        userAttrMapper2.setName("attribute - name 2");
        userAttrMapper2.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        userAttrMapper2.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> userAttrMapper2Config = userAttrMapper2.getConfig();
        userAttrMapper2Config.put(ProtocolMapperUtils.USER_ATTRIBUTE, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2);
        userAttrMapper2Config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2);
        userAttrMapper2Config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        userAttrMapper2Config.put(AttributeStatementHelper.FRIENDLY_NAME, "");

        ProtocolMapperRepresentation userFriendlyAttrMapper = new ProtocolMapperRepresentation();
        userFriendlyAttrMapper.setName("attribute - friendly name");
        userFriendlyAttrMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        userFriendlyAttrMapper.setProtocolMapper(UserAttributeStatementMapper.PROVIDER_ID);

        Map<String, String> userFriendlyAttrMapperConfig = userFriendlyAttrMapper.getConfig();
        userFriendlyAttrMapperConfig.put(ProtocolMapperUtils.USER_ATTRIBUTE, ATTRIBUTE_TO_MAP_FRIENDLY_NAME);
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "urn:oid:1.2.3.4.5.6.7");
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);
        userFriendlyAttrMapperConfig.put(AttributeStatementHelper.FRIENDLY_NAME, ATTRIBUTE_TO_MAP_FRIENDLY_NAME);

        client.setProtocolMappers(Arrays.asList(emailMapper, dottedAttrMapper, nestedAttrMapper, userAttrMapper, userAttrMapper2, userFriendlyAttrMapper));

        return client;
    }

    @Override
    public List<ClientRepresentation> createConsumerClients() {
        return Arrays.asList(
          ClientBuilder.create()
            .clientId(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST)
            .enabled(true)
            .fullScopeEnabled(true)
            .protocol(SamlProtocol.LOGIN_PROTOCOL)
            .baseUrl(getConsumerRoot() + "/sales-post")
            .addRedirectUri(getConsumerRoot() + "/sales-post/*")
            .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
            .build(),
          ClientBuilder.create()
            .clientId(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted")
            .enabled(true)
            .fullScopeEnabled(true)
            .protocol(SamlProtocol.LOGIN_PROTOCOL)
            .baseUrl(getConsumerRoot() + "/sales-post")
            .addRedirectUri(getConsumerRoot() + "/sales-post/*")
            .attribute(SamlConfigAttributes.SAML_AUTHNSTATEMENT, SamlProtocol.ATTRIBUTE_TRUE_VALUE)
            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
            .attribute(SAML_IDP_INITIATED_SSO_URL_NAME, "sales-post")
            .attribute(SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, getConsumerRoot() + "/sales-post/saml")
            .build(),
          ClientBuilder.create()
            .clientId("broker-app")
            .name("broker-app")
            .secret("broker-app-secret")
            .enabled(true)
            .directAccessGrants()
            .addRedirectUri(getConsumerRoot() + "/auth/*")
            .baseUrl(getConsumerRoot() + "/auth/realms/" + REALM_CONS_NAME + "/app")
            .build()
        );
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_SAML_ALIAS, IDP_SAML_PROVIDER_ID);

        idp.setTrustEmail(true);
        idp.setAddReadTokenRoleOnCreate(true);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put(IdentityProviderModel.SYNC_MODE, syncMode.toString());
        config.put(SINGLE_SIGN_ON_SERVICE_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put(ARTIFACT_RESOLUTION_SERVICE_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put(SINGLE_LOGOUT_SERVICE_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put(NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        config.put(FORCE_AUTHN, "false");
        config.put(IdentityProviderModel.LOGIN_HINT, String.valueOf(loginHint));
        config.put(POST_BINDING_RESPONSE, "true");
        config.put(POST_BINDING_AUTHN_REQUEST, "true");
        config.put(VALIDATE_SIGNATURE, "false");
        config.put(WANT_AUTHN_REQUESTS_SIGNED, "false");
        config.put(BACKCHANNEL_SUPPORTED, "false");
        config.put(ARTIFACT_BINDING_RESPONSE, "false");

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
    public String getIDPClientIdInProviderRealm() {
        return getConsumerRoot() + "/auth/realms/" + consumerRealmName();
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
