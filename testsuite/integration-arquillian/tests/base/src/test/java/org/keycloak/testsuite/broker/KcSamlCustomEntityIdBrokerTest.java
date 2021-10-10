package org.keycloak.testsuite.broker;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.Closeable;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

/**
 * Final class as it's not intended to be overriden.
 */
public final class KcSamlCustomEntityIdBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testCustomEntityNotSet() throws Exception {
        // No comparison type, no classrefs, no declrefs -> No RequestedAuthnContext
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .update())
        {
            // Build the login request document
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getConsumerRoot() + "/sales-post/saml", null);
            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST)
                .build()   // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
                  .targetAttributeSamlRequest()
                  .transformDocument((document) -> {
                    try
                    {
                        log.infof("Document: %s", DocumentUtil.asString(document));

                        // Find the Issuer element
                        Element issuerElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), ASSERTION_NSURI.get(), "Issuer");
                        Assert.assertEquals("Unexpected Issuer element value", getAuthServerRoot() + "realms/consumer", issuerElement.getTextContent());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                  })
                  .build()
                .execute();
        }
    }

    @Test
    public void testCustomEntityIdSet() throws Exception {
        // Comparison type set, no classrefs, no declrefs -> No RequestedAuthnContext
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.ENTITY_ID, "http://my.custom.entity.id")
            .update())
        {
            // Build the login request document
            AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(AbstractSamlTest.SAML_CLIENT_ID_SALES_POST + ".dot/ted", getConsumerRoot() + "/sales-post/saml", null);
            Document doc = SAML2Request.convert(loginRep);
            new SamlClientBuilder()
                .authnRequest(getConsumerSamlEndpoint(bc.consumerRealmName()), doc, Binding.POST)
                .build()   // Request to consumer IdP
                .login().idp(bc.getIDPAlias()).build()
                .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
                  .targetAttributeSamlRequest()
                  .transformDocument((document) -> {
                    try
                    {
                        log.infof("Document: %s", DocumentUtil.asString(document));

                        // Find the Issuer element
                        Element issuerElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), ASSERTION_NSURI.get(), "Issuer");
                        Assert.assertEquals("Unexpected Issuer element value", "http://my.custom.entity.id", issuerElement.getTextContent());
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                  })
                  .build()
                .execute();
        }
    }
}
