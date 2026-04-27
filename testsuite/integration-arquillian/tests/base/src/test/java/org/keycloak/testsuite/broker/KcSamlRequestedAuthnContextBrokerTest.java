package org.keycloak.testsuite.broker;

import java.io.Closeable;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.updaters.IdentityProviderAttributeUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.AC_PASSWORD_PROTECTED_TRANSPORT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Final class as it's not intended to be overriden.
 */
public final class KcSamlRequestedAuthnContextBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testNoComparisonTypeNoClassRefsAndNoDeclRefs() throws Exception {
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

                        // Find the RequestedAuthnContext element
                        Element requestedAuthnContextElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), PROTOCOL_NSURI.get(), "RequestedAuthnContext");
                        assertThat("RequestedAuthnContext element found in request document, but was not necessary as ClassRef/DeclRefs were not specified", requestedAuthnContextElement, Matchers.nullValue());
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
    public void testComparisonTypeSetNoClassRefsAndNoDeclRefs() throws Exception {
        // Comparison type set, no classrefs, no declrefs -> No RequestedAuthnContext
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.MINIMUM.value())
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

                        // Find the RequestedAuthnContext element
                        Element requestedAuthnContextElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), PROTOCOL_NSURI.get(), "RequestedAuthnContext");
                        assertThat("RequestedAuthnContext element found in request document, but was not necessary as ClassRef/DeclRefs were not specified", requestedAuthnContextElement, Matchers.nullValue());
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
    public void testComparisonTypeSetClassRefsSetNoDeclRefs() throws Exception {
        // Comparison type set, classref present, no declrefs -> RequestedAuthnContext with AuthnContextClassRef
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.EXACT.value())
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_CLASS_REFS, "[\"" + AC_PASSWORD_PROTECTED_TRANSPORT.get() + "\"]")
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

                        // Find the RequestedAuthnContext element
                        Element requestedAuthnContextElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), PROTOCOL_NSURI.get(), "RequestedAuthnContext");
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement, Matchers.notNullValue());

                        // Verify the ComparisonType attribute
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement.getAttribute("Comparison"), Matchers.is(AuthnContextComparisonType.EXACT.value()));

                        // Find the RequestedAuthnContext/ClassRef element
                        Element requestedAuthnContextClassRefElement = DocumentUtil.getDirectChildElement(requestedAuthnContextElement, ASSERTION_NSURI.get(), "AuthnContextClassRef");
                        assertThat("RequestedAuthnContext/AuthnContextClassRef element not found in request document", requestedAuthnContextClassRefElement, Matchers.notNullValue());

                        // Make sure the RequestedAuthnContext/ClassRef element has the requested value
                        assertThat("RequestedAuthnContext/AuthnContextClassRef element does not have the expected value", requestedAuthnContextClassRefElement.getTextContent(), Matchers.is(AC_PASSWORD_PROTECTED_TRANSPORT.get()));
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
    public void testComparisonTypeSetNoClassRefsDeclRefsSet() throws Exception {
        // Comparison type set, no classref present, declrefs set -> RequestedAuthnContext with AuthnContextDeclRef
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.MINIMUM.value())
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_DECL_REFS, "[\"secure/name/password/icmaolr/uri\"]")
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

                        // Find the RequestedAuthnContext element
                        Element requestedAuthnContextElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), PROTOCOL_NSURI.get(), "RequestedAuthnContext");
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement, Matchers.notNullValue());

                        // Verify the ComparisonType attribute
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement.getAttribute("Comparison"), Matchers.is(AuthnContextComparisonType.MINIMUM.value()));

                        // Find the RequestedAuthnContext/DeclRef element
                        Element requestedAuthnContextDeclRefElement = DocumentUtil.getDirectChildElement(requestedAuthnContextElement, ASSERTION_NSURI.get(), "AuthnContextDeclRef");
                        assertThat("RequestedAuthnContext/AuthnContextDeclRef element not found in request document", requestedAuthnContextDeclRefElement, Matchers.notNullValue());

                        // Make sure the RequestedAuthnContext/DeclRef element has the requested value
                        assertThat("RequestedAuthnContext/AuthnContextDeclRef element does not have the expected value", requestedAuthnContextDeclRefElement.getTextContent(), Matchers.is("secure/name/password/icmaolr/uri"));
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
    public void testNoComparisonTypeClassRefsSetNoDeclRefs() throws Exception {
        // Comparison type set, no classref present, declrefs set -> RequestedAuthnContext with comparison Exact and AuthnContextClassRef
        try (Closeable idpUpdater = new IdentityProviderAttributeUpdater(identityProviderResource)
            .setAttribute(SAMLIdentityProviderConfig.AUTHN_CONTEXT_CLASS_REFS, "[\"" + AC_PASSWORD_PROTECTED_TRANSPORT.get() + "\"]")
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

                        // Find the RequestedAuthnContext element
                        Element requestedAuthnContextElement = DocumentUtil.getDirectChildElement(document.getDocumentElement(), PROTOCOL_NSURI.get(), "RequestedAuthnContext");
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement, Matchers.notNullValue());

                        // Verify the ComparisonType attribute
                        assertThat("RequestedAuthnContext element not found in request document", requestedAuthnContextElement.getAttribute("Comparison"), Matchers.is(AuthnContextComparisonType.EXACT.value()));

                        // Find the RequestedAuthnContext/ClassRef element
                        Element requestedAuthnContextClassRefElement = DocumentUtil.getDirectChildElement(requestedAuthnContextElement, ASSERTION_NSURI.get(), "AuthnContextClassRef");
                        assertThat("RequestedAuthnContext/AuthnContextClassRef element not found in request document", requestedAuthnContextClassRefElement, Matchers.notNullValue());

                        // Make sure the RequestedAuthnContext/ClassRef element has the requested value
                        assertThat("RequestedAuthnContext/AuthnContextClassRef element does not have the expected value", requestedAuthnContextClassRefElement.getTextContent(), Matchers.is(AC_PASSWORD_PROTECTED_TRANSPORT.get()));
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
