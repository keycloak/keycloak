package org.keycloak.protocol.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.junit.Assert.*;

public class SamlProtocolTest {

    SamlProtocol protocol = new SamlProtocol();

    @Test
    public void testBuildArtifactResponse() throws ConfigurationException, ProcessingException, ParsingException {

        ResponseType response = new SAML2LoginResponseBuilder()
                .requestID(IDGenerator.create("ID_"))
                .destination("http://localhost:8180/auth/realms/demo/broker/saml-broker/endpoint")
                .issuer("http://saml.idp/saml")
                .assertionExpiration(1000000)
                .subjectExpiration(1000000)
                .requestIssuer("http://localhost:8180/auth/realms/demo")
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), "a@b.c")
                .authMethod(JBossSAMLURIConstants.AC_UNSPECIFIED.get())
                .sessionIndex("idp:" + UUID.randomUUID())
                .buildModel();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SAMLResponseWriter writer = new SAMLResponseWriter(StaxUtil.getXMLStreamWriter(bos));
        writer.write(response);
        Document responseDoc = DocumentUtil.getDocument(new ByteArrayInputStream(bos.toByteArray()));

        Document artifactResponseDoc = protocol.buildArtifactResponse(responseDoc);
        String artifactResponse = DocumentUtil.asString(artifactResponseDoc);

        assertTrue(artifactResponse.contains("samlp:ArtifactResponse"));
        assertTrue(artifactResponse.contains("samlp:Response"));
        assertTrue(artifactResponse.contains("saml:Assertion"));
        assertTrue(artifactResponse.indexOf("samlp:ArtifactResponse") < artifactResponse.indexOf("samlp:Response"));
        assertTrue(artifactResponse.indexOf("samlp:Response") < artifactResponse.indexOf("saml:Assertion"));
        assertEquals(4, artifactResponse.split("\\Q<saml:Issuer>http://saml.idp/saml</saml:Issuer>\\E").length);
        assertEquals(3, artifactResponse.split(
                "\\Q<samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\\E").length);
    }

}
