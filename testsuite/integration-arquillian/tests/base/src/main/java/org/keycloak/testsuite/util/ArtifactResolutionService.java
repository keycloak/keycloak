package org.keycloak.testsuite.util;

import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.SAML2NameIDBuilder;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.w3c.dom.Document;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.http.HTTPBinding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * This class simulates a service provider's (clients's) Artifact Resolution Service. It is a webservice provider
 * that can accept an artifact resolve message, and return an artifact response (all via SOAP).
 *
 * The class in runnable, and must be run in a thread before being used. The calling test SHOULD perform a wait
 * on its instance of the class before proceeding, as this class will notify when it as finished setting up the endpoint
 */
@WebServiceProvider
@ServiceMode(value = Service.Mode.MESSAGE)
public class ArtifactResolutionService implements Provider<Source>, Runnable {

    private ArtifactResponseType artifactResponseType;
    private final String endpointAddress;
    private ArtifactResolveType lastArtifactResolve;
    private boolean running = true;

    /**
     * Standard constructor
     * @param endpointAddress full address on which this endpoint will listen for SOAP messages (e.g. http://127.0.0.1:8082/)
     */
    public ArtifactResolutionService(String endpointAddress) {
        this.endpointAddress = endpointAddress;
    }

    /**
     * Sets the SAML message that will be integrated into the artifact response
     * @param responseDocument a Document of the SAML message
     * @return this ArtifactResolutionService
     */
    public ArtifactResolutionService setResponseDocument(Document responseDocument){
        try {
            this.artifactResponseType = SamlProtocolUtils.buildArtifactResponse(responseDocument);
        } catch (ParsingException | ProcessingException | ConfigurationException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public ArtifactResolutionService setEmptyArtifactResponse(String issuer) {
        try {
            this.artifactResponseType = SamlProtocolUtils.buildArtifactResponse(null, SAML2NameIDBuilder.value(issuer).build());
        } catch (ConfigurationException | ProcessingException e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * Returns the ArtifactResolve message that was last received by the service. If received data was not an
     * ArtifactResolve message, the value returned will be null
     * @return the last received ArtifactResolve message
     */
    public ArtifactResolveType getLastArtifactResolve() {
        return lastArtifactResolve;
    }

    /**
     * This is the method called when a message is received by the endpoint.
     * It gets the message, extracts the ArtifactResolve message from the SOAP, creates a SOAP message containing
     * an ArtifactResponse message with the configured SAML message, and returns it.
     * @param msg The SOAP message received by the endpoint, in Source format
     * @return A StreamSource containing the ArtifactResponse
     */
    @Override
    public Source invoke(Source msg) {
        byte[] response;

        try (StringWriter w = new StringWriter()){
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.transform(msg, new StreamResult(w));
            String s = w.toString();
            Document doc = Soap.extractSoapMessage(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
            SAMLDocumentHolder samlDoc = SAML2Request.getSAML2ObjectFromDocument(doc);
            if (samlDoc.getSamlObject() instanceof ArtifactResolveType) {
                lastArtifactResolve = (ArtifactResolveType) samlDoc.getSamlObject();
            } else {
                lastArtifactResolve = null;
            }
            Document artifactResponse = SamlProtocolUtils.convert(artifactResponseType);
            response = Soap.createMessage().addToBody(artifactResponse).getBytes();
        } catch (ProcessingException | ConfigurationException | TransformerException | ParsingException | IOException e) {
            throw new RuntimeException(e);
        }

        return new StreamSource(new ByteArrayInputStream(response));
    }

    /**
     * Main method of the class. Creates the endpoint, and will keep it running until the "stop()" method is called.
     */
    @Override
    public void run() {
        Endpoint endpoint;
        synchronized (this) {
            endpoint = Endpoint.create(HTTPBinding.HTTP_BINDING, this);
            endpoint.publish(endpointAddress);
            notify();
        }
        while (running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        endpoint.stop();
    }

    /**
     * Calling this method will allow the run method to shutdown gracefully
     */
    public void stop() {
        running = false;
    }
}
