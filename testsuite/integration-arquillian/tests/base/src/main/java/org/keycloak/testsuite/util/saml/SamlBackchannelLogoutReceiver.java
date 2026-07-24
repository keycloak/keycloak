package org.keycloak.testsuite.util.saml;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.PublicKey;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.w3c.dom.Document;

public class SamlBackchannelLogoutReceiver implements AutoCloseable {

    private final HttpServer server;
    private LogoutRequestType logoutRequest;
    private final String url;
    private final ClientRepresentation samlClient;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public SamlBackchannelLogoutReceiver(int port, ClientRepresentation samlClient, String publicKeyStr, String privateKeyStr) {
        this.samlClient = samlClient;
        publicKey = publicKeyStr == null ? null : org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKeyStr);
        privateKey = privateKeyStr == null ? null : org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKeyStr);
        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("localhost"), port);
            server = HttpServer.create(address, 0);
            this.url = "http://" + address.getHostString() + ":" + port;
        } catch (IOException e) {
            throw new RuntimeException("Cannot create http server", e);
        }

        server.createContext("/", new SamlBackchannelLogoutReceiver.SamlBackchannelLogoutHandler());
        server.setExecutor(null);
        server.start();
    }

    public SamlBackchannelLogoutReceiver(int port, ClientRepresentation samlClient) {
        this(port, samlClient, null, null);
    }

    public String getUrl() {
        return url;
    }

    public boolean isLogoutRequestReceived() {
        return logoutRequest != null;
    }

    public LogoutRequestType getLogoutRequest() {
        return logoutRequest;
    }

    @Override
    public void close() throws Exception {
        server.stop(0);
    }

    private class SamlBackchannelLogoutHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            try {
                t.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "text/xml");
                t.sendResponseHeaders(200, 0);

                Document request = Soap.extractSoapMessage(t.getRequestBody());
                SAMLDocumentHolder samlDoc = SAML2Response.getSAML2ObjectFromDocument(request);
                if (!(samlDoc.getSamlObject() instanceof LogoutRequestType)) {
                    throw new RuntimeException("SamlBackchannelLogoutReceiver received a message that was not LogoutRequestType");
                }
                logoutRequest = (LogoutRequestType) samlDoc.getSamlObject();
                StatusResponseType logoutResponse = new SAML2LogoutResponseBuilder()
                        .issuer(samlClient.getClientId())
                        .logoutRequestID(logoutRequest.getID())
                        .buildModel();
                JaxrsSAML2BindingBuilder soapBinding = new JaxrsSAML2BindingBuilder(null);
                if (requiresClientSignature(samlClient)) {
                    soapBinding.signatureAlgorithm(getSignatureAlgorithm(samlClient))
                            .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey, null)
                            .signDocument();
                }
                Document doc = soapBinding.soapBinding(SAML2Response.convert(logoutResponse)).getDocument();

                // send logout response
                OutputStream os = t.getResponseBody();
                os.write(Soap.createMessage().addToBody(doc).getBytes());
                os.close();
            } catch (Exception ex) {
                t.sendResponseHeaders(500, 0);
            }
        }
    }

    private SignatureAlgorithm getSignatureAlgorithm(ClientRepresentation client) {
        String alg = client.getAttributes().get(SamlConfigAttributes.SAML_SIGNATURE_ALGORITHM);
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null)
                return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    public boolean requiresClientSignature(ClientRepresentation client) {
        return "true".equals(client.getAttributes().get(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE));
    }
}
