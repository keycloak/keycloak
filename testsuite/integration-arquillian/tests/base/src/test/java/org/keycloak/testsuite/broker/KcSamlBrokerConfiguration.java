/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.broker;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME);
        client.setEnabled(true);
        client.setProtocol(IDP_SAML_PROVIDER_ID);
        client.setRedirectUris(Collections.singletonList(
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint"
        ));

        Map<String, String> attributes = new HashMap<>();

        attributes.put("saml.authnstatement", "true");
        attributes.put("saml_single_logout_service_url_post",
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put("saml_assertion_consumer_url_post",
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_SAML_ALIAS + "/endpoint");
        attributes.put("saml_force_name_id_format", "true");
        attributes.put("saml_name_id_format", "username");
        attributes.put("saml.assertion.signature", "false");
        attributes.put("saml.server.signature", "false");
        attributes.put("saml.client.signature", "false");
        attributes.put("saml.encrypt", "false");

        client.setAttributes(attributes);

        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("email");
        mapper.setProtocol("saml");
        mapper.setProtocolMapper("saml-user-property-mapper");
        mapper.setConsentRequired(false);

        Map<String, String> mapperConfig = mapper.getConfig();
        mapperConfig.put("user.attribute", "email");
        mapperConfig.put("attribute.name", "urn:oid:1.2.840.113549.1.9.1");
        mapperConfig.put("attribute.nameformat", "urn:oasis:names:tc:SAML:2.0:attrname-format:uri");
        mapperConfig.put("friendly.name", "email");

        client.setProtocolMappers(Collections.singletonList(
                mapper
        ));

        return Collections.singletonList(client);
    }

    @Override
    public List<ClientRepresentation> createConsumerClients(SuiteContext suiteContext) {
        return null;
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_SAML_ALIAS, IDP_SAML_PROVIDER_ID);

        idp.setTrustEmail(true);
        idp.setAddReadTokenRoleOnCreate(true);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put("singleSignOnServiceUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put("singleLogoutServiceUrl", getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml");
        config.put("nameIDPolicyFormat", "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress");
        config.put("forceAuthn", "true");
        config.put("postBindingResponse", "true");
        config.put("postBindingAuthnRequest", "true");
        config.put("validateSignature", "false");
        config.put("wantAuthnRequestsSigned", "false");
        config.put("backchannelSupported", "true");

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
