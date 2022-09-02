package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.broker.saml.mappers.XPathAttributeMapper;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.HardcodedAttributeMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.w3c.dom.Document;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

/**
 * Integration test for the {@link XPathAttributeMapper}.
 * Will add an extra attribute with an XML string to the provider's response,
 * extracting it using the {@link org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser},
 * and parsing it using {@link XPathAttributeMapper}, finally ending up in a user attribute in the database.
 *
 * This contains only the happy path. Have a look at <code>org.keycloak.test.broker.saml.XPathAttributeMapperTest</code>
 * for unit style tests and handling parsing errors.
 */
public class KcSamlXPathAttributeMapperTest extends AbstractInitializedBaseBrokerTest {

    @Override
    public void beforeBrokerTest() {
        super.beforeBrokerTest();

        RealmResource realm = adminClient.realm(bc.providerRealmName());
        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setProtocol("saml");
        protocolMapper.setName("Hardcoded XML");
        protocolMapper.setProtocolMapper(HardcodedAttributeMapper.PROVIDER_ID);
        protocolMapper.getConfig().put(HardcodedAttributeMapper.ATTRIBUTE_VALUE,
                "<firstName>Theo</firstName><lastName>Tester</lastName><email>test@example.org</email><xml-output>Some random text</xml-output>"
        );
        protocolMapper.getConfig().put(AttributeStatementHelper.FRIENDLY_NAME, "xml-friendlyName");
        protocolMapper.getConfig().put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "xml-name");
        protocolMapper.getConfig().put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC);

        ClientRepresentation clientRepresentation = realm.clients().findByClientId(bc.getIDPClientIdInProviderRealm())
                .get(0);
        realm.clients().get(clientRepresentation.getId()).getProtocolMappers().createMapper(protocolMapper).close();

        addXpathMapper("firstName");
        addXpathMapper("lastName");
        addXpathMapper("email");
        addXpathMapper("xml-output");
    }

    private void addXpathMapper(String field) {
        IdentityProviderMapperRepresentation xpathMapper = new IdentityProviderMapperRepresentation();
        xpathMapper.setName("xpath-mapper-" + field);
        xpathMapper.setIdentityProviderMapper(XPathAttributeMapper.PROVIDER_ID);
        xpathMapper.setIdentityProviderAlias(IDP_SAML_ALIAS);
        xpathMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, "INHERIT")
                .put(XPathAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "xml-friendlyName")
                .put(XPathAttributeMapper.ATTRIBUTE_XPATH, "//*[local-name()='" + field + "']")
                .put(XPathAttributeMapper.USER_ATTRIBUTE, field)
                .build());

        identityProviderResource
                .addMapper(xpathMapper).close();
    }


    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcSamlBrokerConfiguration(false) {
        };
    }

    @Test
    public void testXPathAttributeMapper() throws Exception {
        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST, getConsumerRoot() + "/sales-post/saml", null);

        Document doc = SAML2Request.convert(loginRep);

        new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, SamlClient.Binding.POST).build()   // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()

                .processSamlResponse(SamlClient.Binding.POST)    // AuthnRequest to producer IdP
                .targetAttributeSamlRequest()
                .build()

                .login().user(bc.getUserLogin(), bc.getUserPassword()).build()

                .processSamlResponse(SamlClient.Binding.POST)    // Response from producer IdP
                .transformDocument(document -> {
                    // this XML should contain the hardcoded extra attribute
                    log.infof("Document: %s", DocumentUtil.asString(document));
                })
                .build()

                .followOneRedirect()
                .followOneRedirect()

                .getSamlResponse(SamlClient.Binding.POST);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserRepresentation user = realm.users().search(bc.getUserLogin()).get(0);
        Assert.assertThat(user.getFirstName(), equalTo("Theo"));
        Assert.assertThat(user.getLastName(), equalTo("Tester"));
        Assert.assertThat(user.getEmail(), equalTo("test@example.org"));
        Assert.assertThat(user.getAttributes().get("xml-output"), equalTo(Collections.singletonList("Some random text")));
    }

}
