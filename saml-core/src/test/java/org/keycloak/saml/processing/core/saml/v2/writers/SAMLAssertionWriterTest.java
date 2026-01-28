package org.keycloak.saml.processing.core.saml.v2.writers;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextDeclType;
import org.keycloak.dom.saml.v2.assertion.AuthnContextType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.junit.Assert;
import org.junit.Test;

public class SAMLAssertionWriterTest {
    @Test
    public void testAuthnStatementSessionNotOnOrAfterExists() throws ProcessingException {
        long sessionLengthInSeconds = 3600;

        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();
        XMLGregorianCalendar sessionExpirationDate = XMLTimeUtil.add(issueInstant, sessionLengthInSeconds);

        AuthnStatementType authnStatementType = new AuthnStatementType(issueInstant);

        authnStatementType.setSessionIndex("9b3cf799-225b-424a-8e5e-ee3c38e06ded::24b2f572-163c-43ad-8011-de6cd3803f76");
        authnStatementType.setSessionNotOnOrAfter(sessionExpirationDate);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SAMLAssertionWriter samlAssertionWriter = new SAMLAssertionWriter(StaxUtil.getXMLStreamWriter(byteArrayOutputStream));

        samlAssertionWriter.write(authnStatementType, true);

        String serializedAssertion = new String(byteArrayOutputStream.toByteArray(), GeneralConstants.SAML_CHARSET);
        String expectedXMLAttribute = "SessionNotOnOrAfter=\"" + sessionExpirationDate.toString() + "\"";

        Assert.assertTrue(serializedAssertion.contains(expectedXMLAttribute));
    }

    @Test
    public void testAuthnContextTypeWithAuthnContextClassRefAndAuthnContextDecl() throws ProcessingException {
        String uriSmartCard = "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI";
        String expectedAuthnContextDecl = "AuthnContextDecl>"+uriSmartCard+"<";
        String expectedAuthnContextClassRef = "AuthnContextClassRef>"+uriSmartCard+"<";

        AuthnContextClassRefType authnContextClassRef = new AuthnContextClassRefType(URI.create(uriSmartCard));
        AuthnContextDeclType authnContextDecl = new AuthnContextDeclType(URI.create(uriSmartCard));

        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();
        AuthnStatementType authnStatementType = new AuthnStatementType(issueInstant);
        AuthnContextType authnContextType = new AuthnContextType();
        AuthnContextType.AuthnContextTypeSequence sequence = new AuthnContextType.AuthnContextTypeSequence();
        sequence.setAuthnContextDecl(authnContextDecl);
        sequence.setClassRef(authnContextClassRef);
        authnContextType.setSequence(sequence);
        authnStatementType.setAuthnContext(authnContextType);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SAMLAssertionWriter samlAssertionWriter = new SAMLAssertionWriter(StaxUtil.getXMLStreamWriter(byteArrayOutputStream));

        samlAssertionWriter.write(authnStatementType, true);

        String serializedAssertion = new String(byteArrayOutputStream.toByteArray(), GeneralConstants.SAML_CHARSET);

        Assert.assertTrue(serializedAssertion.contains(expectedAuthnContextClassRef));
        Assert.assertTrue(serializedAssertion.contains(expectedAuthnContextDecl));
    }
}
