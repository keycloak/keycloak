package org.keycloak.protocol.saml;

import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class SamlProtocolUtilsTest {

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

        ArtifactResponseType artifactResponseType = SamlProtocolUtils.buildArtifactResponse(responseDoc);
        Document doc = SamlProtocolUtils.convert(artifactResponseType);
        String artifactResponse = DocumentUtil.asString(doc);

        assertThat(artifactResponse, containsString("samlp:ArtifactResponse"));
        assertThat(artifactResponse, containsString("samlp:Response"));
        assertThat(artifactResponse, containsString("saml:Assertion"));
        assertThat(artifactResponse.indexOf("samlp:ArtifactResponse"), lessThan(artifactResponse.indexOf("samlp:Response")));
        assertThat(artifactResponse.indexOf("samlp:Response"), lessThan(artifactResponse.indexOf("saml:Assertion")));
        assertThat(artifactResponse.split("\\Q<saml:Issuer>http://saml.idp/saml</saml:Issuer>\\E").length, is(4));
        assertThat(artifactResponse.split(
                "\\Q<samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/>\\E").length, is(3));
    }

}
