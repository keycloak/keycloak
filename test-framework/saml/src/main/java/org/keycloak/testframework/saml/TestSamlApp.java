package org.keycloak.testframework.saml;

import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;

import com.sun.net.httpserver.HttpServer;
import org.w3c.dom.Document;

/**
 * Mock SAML Service Provider (SP) that receives SAML responses via HTTP server callbacks.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class TestSamlApp {

    public static final String ACS_PATH = "/saml/acs";
    public static final String BACKCHANNEL_LOGOUT_PATH = "/saml/backchannel-logout";
    public static final String POST_FORM_PATH = "/saml/post-form";

    private final HttpServer httpServer;
    private final String assertionConsumerServiceUrl;
    private final String backchannelLogoutUrl;
    private final String postFormUrl;
    private volatile String lastArtifact;
    private volatile String pendingPostFormHtml;
    private volatile String backchannelLogoutIssuer;
    private final AtomicReference<Document> lastBackchannelLogoutDocument = new AtomicReference<>();

    public TestSamlApp(HttpServer httpServer) {
        this.httpServer = httpServer;

        String baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();
        assertionConsumerServiceUrl = baseUrl + ACS_PATH;
        backchannelLogoutUrl = baseUrl + BACKCHANNEL_LOGOUT_PATH;
        postFormUrl = baseUrl + POST_FORM_PATH;

        // ACS endpoint - captures SAMLResponse or SAMLart parameter and returns HTML for WebDriver
        httpServer.createContext(ACS_PATH, exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String query = exchange.getRequestURI().getQuery();

            // Check for artifact in query string (GET redirect) or POST body
            String artifact = null;
            if (query != null) {
                artifact = extractParam(query, "SAMLart");
            }
            if (artifact == null || artifact.isEmpty()) {
                artifact = extractParam(body, "SAMLart");
            }

            String samlResponse = extractParam(body, "SAMLResponse");

            if (artifact != null && !artifact.isEmpty()) {
                lastArtifact = artifact;
                String html = "<html><body>Artifact received: " + artifact + "</body></html>";
                byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                exchange.getResponseBody().write(htmlBytes);
            } else {
                String html = """
                        <html>
                            <body>
                                <input type="hidden" name="SAMLResponse" value="%s"/>
                            </body>
                        </html>
                        """.formatted(samlResponse);
                byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                exchange.getResponseBody().write(htmlBytes);
            }
            exchange.close();
        });

        // Backchannel logout endpoint - receives SOAP LogoutRequest and returns LogoutResponse
        httpServer.createContext(BACKCHANNEL_LOGOUT_PATH, exchange -> {
            try {
                exchange.getResponseHeaders().add("Content-Type", "text/xml");
                exchange.sendResponseHeaders(200, 0);

                Document request = Soap.extractSoapMessage(exchange.getRequestBody());
                SAMLDocumentHolder samlDoc = SAML2Response.getSAML2ObjectFromDocument(request);
                LogoutRequestType logoutRequest = (LogoutRequestType) samlDoc.getSamlObject();
                lastBackchannelLogoutDocument.set(request);

                String issuer = backchannelLogoutIssuer != null ? backchannelLogoutIssuer : "unknown";
                StatusResponseType logoutResponse = new SAML2LogoutResponseBuilder()
                        .issuer(issuer)
                        .logoutRequestID(logoutRequest.getID())
                        .buildModel();
                Document responseDoc = SAML2Response.convert(logoutResponse);
                OutputStream os = exchange.getResponseBody();
                os.write(Soap.createMessage().addToBody(responseDoc).getBytes());
                os.close();
            } catch (Exception ex) {
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
            }
        });

        // POST form endpoint - serves an auto-submit form for POST binding requests (e.g. LogoutRequest)
        httpServer.createContext(POST_FORM_PATH, exchange -> {
            String html = pendingPostFormHtml != null ? pendingPostFormHtml : "<html><body>No pending form</body></html>";
            byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, htmlBytes.length);
            exchange.getResponseBody().write(htmlBytes);
            exchange.close();
        });
    }

    /**
     * Set an auto-submit form to be served at the POST form endpoint.
     * Used by SamlClient to send POST binding requests via WebDriver.
     */
    public void servePostForm(String actionUrl, String paramName, String encodedValue) {
        pendingPostFormHtml = """
                <html>
                    <body onload="document.forms[0].submit()">
                        <form method="POST" action="%s">
                            <input type="hidden" name="%s" value="%s"/>
                        </form>
                    </body>
                </html>
                """.formatted(actionUrl, paramName, encodedValue);
    }

    public String getPostFormUrl() {
        return postFormUrl;
    }

    public String getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public String getBackchannelLogoutUrl() {
        return backchannelLogoutUrl;
    }

    public String getLastArtifact() {
        return lastArtifact;
    }

    public void clearLastArtifact() {
        lastArtifact = null;
    }

    public Document getLastBackchannelLogoutDocument() {
        return lastBackchannelLogoutDocument.get();
    }

    public void clearLastBackchannelLogoutDocument() {
        lastBackchannelLogoutDocument.set(null);
    }

    public void setBackchannelLogoutIssuer(String issuer) {
        this.backchannelLogoutIssuer = issuer;
    }

    public void close() {
        httpServer.removeContext(ACS_PATH);
        httpServer.removeContext(BACKCHANNEL_LOGOUT_PATH);
        httpServer.removeContext(POST_FORM_PATH);
    }

    private static String extractParam(String body, String paramName) {
        for (String param : body.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && paramName.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
