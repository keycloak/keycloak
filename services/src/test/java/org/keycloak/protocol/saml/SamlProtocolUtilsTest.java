package org.keycloak.protocol.saml;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.MatcherAssert.assertThat;

public class SamlProtocolUtilsTest {

    private final byte[] bytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

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

    @Test
    public void testLimitedInputStream() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 3);

        for (int i = 1; i <= 3; i++) {
            int read = inputStream.read();
            assertThat(read, is(i));
        }

        Throwable e = Assertions.assertThrowsExactly(IOException.class, () -> {
            inputStream.read();
        });
        assertThat(e.getMessage(), containsString("Limit reached") );

    }

    @Test
    public void testLimitedInputStreamBufferMore() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 6);

        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 2);
            assertThat(read, is(2));
        }
        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 2);
            assertThat(read, is(2));
        }

        Throwable e = Assertions.assertThrowsExactly(IOException.class, () -> {
            byte[] buffer = new byte[10];
            int read = inputStream.read(buffer, 0, 10);
        });
        assertThat(e.getMessage(), containsString("Limit reached") );

    }

    @Test
    public void testLimitedInputStreamBufferReadLess() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 20);

        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 5);
            assertThat(read, is(5));
        }
        {
            byte[] buffer = new byte[5];
            int read = inputStream.read(buffer, 0, 3);
            assertThat(read, is(3));
        }
        {
            byte[] buffer = new byte[200];
            int read = inputStream.read(buffer, 0, 20);
            assertThat(read, is(2));
            assertThat(Byte.valueOf(buffer[0]), is(Byte.valueOf((byte)9)));
            assertThat(Byte.valueOf(buffer[1]), is(Byte.valueOf((byte)10)));
        }

    }

    @Test
    public void testLimitedInputStreamBufferRead2() throws IOException {

        InputStream inputStream = SamlProtocolUtils.limit(new ByteArrayInputStream(bytes), 20);
        byte[] buffer = new byte[10];

        int read = inputStream.read(buffer, 0, 5);
        read += inputStream.read(buffer, 5, 3);

        for (int i = 0; i < read; i++) {
            assertThat(bytes[i], is(buffer[i]));
        }

    }


}
