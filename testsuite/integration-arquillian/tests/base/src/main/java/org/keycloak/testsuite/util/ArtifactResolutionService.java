package org.keycloak.testsuite.util;

import org.apache.commons.io.Charsets;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.ws.*;
import javax.xml.ws.http.*;
import javax.xml.transform.stream.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class ArtifactResolutionService implements Provider<Source>, Runnable {

    private Document responseDocument;
    private final String endpointAddress;
    private ArtifactResolveType lastArtifactResolve;
    private boolean running = true;

    public ArtifactResolutionService(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    public ArtifactResolutionService setResponseDocument(Document responseDocument){
        this.responseDocument = responseDocument;
        return this;
    }

    public ArtifactResolveType getLastArtifactResolve() {
        return lastArtifactResolve;
    }

    @Override
    public Source invoke(Source msg) {
        byte[] response;

        try (StringWriter w = new StringWriter()){
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.transform(msg, new StreamResult(w));
            String s = w.toString();
            Document doc = Soap.extractSoapMessage(new ByteArrayInputStream(s.getBytes(Charsets.UTF_8)));
            SAMLDocumentHolder samlDoc = SAML2Request.getSAML2ObjectFromDocument(doc);
            if (samlDoc.getSamlObject() instanceof ArtifactResolveType) {
                lastArtifactResolve = (ArtifactResolveType) samlDoc.getSamlObject();
            } else {
                lastArtifactResolve = null;
            }
            Document artifactResponse = SamlProtocolUtils.buildArtifactResponse(responseDocument);
            response = Soap.createMessage().addToBody(artifactResponse).getBytes();
        } catch (ProcessingException | ConfigurationException | TransformerException | ParsingException | IOException e) {
            throw new RuntimeException(e);
        }

        return new StreamSource(new ByteArrayInputStream(response));
    }

    @Override
    public void run() {
        Endpoint endpoint;
        synchronized (this) {
            String address = endpointAddress;
            endpoint = Endpoint.create(HTTPBinding.HTTP_BINDING, this);
            endpoint.publish(address);
            notify();
        }
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    public void stop() {
        running = false;
    }
}
