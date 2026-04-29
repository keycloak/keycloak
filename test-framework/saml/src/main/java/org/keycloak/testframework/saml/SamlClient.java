package org.keycloak.testframework.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.support.PageFactory;
import org.w3c.dom.Document;

/**
 * SAML client for sending SAML requests and handling responses.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class SamlClient {

    private final String baseUrl;
    private final CloseableHttpClient httpClient;
    private final HttpClientContext httpContext;
    private final ManagedWebDriver managedWebDriver;
    private final TestSamlApp testSamlApp;
    private final ClientResource clientResource;

    private String realmName;
    private String clientId;
    private String assertionConsumerServiceUrl;

    private SAMLDocumentHolder lastResponse;

    public SamlClient(String baseUrl, CloseableHttpClient httpClient, ManagedWebDriver managedWebDriver, TestSamlApp testSamlApp,
                      ClientResource clientResource) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.httpContext = HttpClientContext.create();
        this.managedWebDriver = managedWebDriver;
        this.testSamlApp = testSamlApp;
        this.clientResource = clientResource;
    }

    /**
     * Send AuthnRequest via Redirect Binding and display login form.
     */
    public void openLoginForm() {
        openLoginForm(false);
    }

    /**
     * Send AuthnRequest via Redirect Binding.
     * If passive is true, Keycloak returns an error response without showing the login form.
     */
    public void openLoginForm(boolean passive) {
        try {
            AuthnRequestType authnRequest = createAuthnRequest(passive);
            Document samlDoc = SAML2Request.convert(authnRequest);

            URI redirectUri = new BaseSAML2BindingBuilder()
                    .redirectBinding(samlDoc)
                    .requestURI(getSamlEndpoint());

            managedWebDriver.driver().navigate().to(redirectUri.toString());
        } catch (ConfigurationException | ProcessingException | IOException | ParsingException e) {
            throw new RuntimeException("Failed to create AuthnRequest", e);
        }
    }

    /**
     * Perform full login: send AuthnRequest, fill credentials, parse SAMLResponse.
     */
    public SAMLDocumentHolder doLogin(String username, String password) {
        openLoginForm();
        String currentUrl = managedWebDriver.driver().getCurrentUrl();
        if (!currentUrl.contains("/protocol/saml") || currentUrl.contains(testSamlApp.getAssertionConsumerServiceUrl())) {
            // Already redirected to ACS (SSO session active)
        } else {
            fillLoginForm(username, password);
        }
        return parseLoginResponse();
    }

    /**
     * Parse SAMLResponse from the current page (POST binding).
     */
    public SAMLDocumentHolder parseLoginResponse() {
        String pageSource = managedWebDriver.driver().getPageSource();
        Matcher matcher = SAML_INPUT_PATTERN.matcher(pageSource);
        if (matcher.find()) {
            lastResponse = SAMLRequestParser.parseResponsePostBinding(matcher.group(2));
            return lastResponse;
        }

        throw new RuntimeException("No SAMLResponse found in page: " + pageSource);
    }

    /**
     * Send LogoutRequest via POST Binding using WebDriver.
     * This preserves SSO cookies, matching the browser-based logout flow.
     */
    public SAMLDocumentHolder doLogout(String nameId, String sessionIndex) {
        try {
            Document logoutDoc = createLogoutRequestDocument(nameId, sessionIndex);
            String encoded = new BaseSAML2BindingBuilder()
                    .postBinding(logoutDoc)
                    .encoded();

            testSamlApp.servePostForm(getSamlEndpoint(), GeneralConstants.SAML_REQUEST_KEY, encoded);
            managedWebDriver.driver().navigate().to(testSamlApp.getPostFormUrl());
            return parseLoginResponse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform logout", e);
        }
    }

    /**
     * Extract NameID from the login response.
     */
    public NameIDType extractNameId(SAMLDocumentHolder holder) {
        ResponseType responseType = (ResponseType) holder.getSamlObject();
        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();
        return (NameIDType) assertion.getSubject().getSubType().getBaseID();
    }

    /**
     * Extract SessionIndex from the login response.
     */
    public String extractSessionIndex(SAMLDocumentHolder holder) {
        ResponseType responseType = (ResponseType) holder.getSamlObject();
        AssertionType assertion = responseType.getAssertions().get(0).getAssertion();
        AuthnStatementType authnStatement = (AuthnStatementType) assertion.getStatements().iterator().next();
        return authnStatement.getSessionIndex();
    }

    /**
     * Perform login with Artifact Binding: send AuthnRequest, fill login form,
     * capture artifact at ACS, then resolve it via SOAP ArtifactResolve.
     */
    public SAMLDocumentHolder doArtifactLogin(String username, String password) {
        openLoginForm();

        String currentUrl = managedWebDriver.driver().getCurrentUrl();
        if (!currentUrl.contains(testSamlApp.getAssertionConsumerServiceUrl())) {
            fillLoginForm(username, password);
        }

        // Wait for ACS to receive the artifact
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        String artifact = testSamlApp.getLastArtifact();
        if (artifact == null) {
            throw new RuntimeException("No artifact received at ACS. "
                    + "Ensure client has saml.artifact.binding=true. Current URL: "
                    + managedWebDriver.driver().getCurrentUrl());
        }
        testSamlApp.clearLastArtifact();

        return doArtifactResolve(artifact);
    }

    /**
     * Send ArtifactResolve via SOAP to Keycloak's artifact resolution endpoint
     * and return the ArtifactResponse.
     */
    public SAMLDocumentHolder doArtifactResolve(String artifact) {
        String artifactResolutionEndpoint = getSamlEndpoint() + "/resolve";
        try {
            String artifactResolveXml = """
                    <samlp:ArtifactResolve
                            xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                            xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                            ID="%s" Version="2.0" IssueInstant="%s"
                            Destination="%s">
                        <saml:Issuer>%s</saml:Issuer>
                        <samlp:Artifact>%s</samlp:Artifact>
                    </samlp:ArtifactResolve>
                    """.formatted(
                            "_" + UUID.randomUUID(),
                            Instant.now().toString(),
                            artifactResolutionEndpoint,
                            clientId,
                            artifact);

            Document doc = DocumentUtil.getDocument(artifactResolveXml);

            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            soapMessage.getSOAPBody().addDocument(doc);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            soapMessage.writeTo(baos);
            byte[] soapBytes = baos.toByteArray();

            HttpPost post = new HttpPost(artifactResolutionEndpoint);
            post.setHeader("Content-Type", "text/xml; charset=utf-8");
            post.setHeader("SOAPAction", "http://www.oasis-open.org/committees/security");
            post.setEntity(new ByteArrayEntity(soapBytes));

            try (CloseableHttpResponse response = httpClient.execute(post, httpContext)) {
                int status = response.getStatusLine().getStatusCode();
                byte[] responseBytes = EntityUtils.toByteArray(response.getEntity());
                if (status != 200) {
                    throw new RuntimeException("ArtifactResolve failed with HTTP " + status
                            + ": " + new String(responseBytes, StandardCharsets.UTF_8)
                                    .substring(0, Math.min(500, responseBytes.length)));
                }
                Document soapBody = Soap.extractSoapMessage(new ByteArrayInputStream(responseBytes));
                lastResponse = SAML2Request.getSAML2ObjectFromDocument(soapBody);
                return lastResponse;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to perform ArtifactResolve", e);
        }
    }

    public SamlClient client(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public SamlClient realm(String realmName) {
        this.realmName = realmName;
        return this;
    }

    public SamlClient assertionConsumerServiceUrl(String url) {
        this.assertionConsumerServiceUrl = url;
        return this;
    }


    /**
     * Clear WebDriver cookies to simulate logout without an SSO cookie.
     * Navigates to Keycloak domain first because WebDriver can only delete cookies for the current domain.
     */
    public void clearCookies() {
        managedWebDriver.driver().navigate().to(baseUrl);
        managedWebDriver.driver().manage().deleteAllCookies();
    }

    public String getSamlEndpoint() {
        return baseUrl + "/realms/" + realmName + "/protocol/saml";
    }

    public String getClientId() {
        return clientId;
    }

    public String getRealm() {
        return realmName;
    }

    public SAMLDocumentHolder getLastResponse() {
        return lastResponse;
    }

    public ClientResource admin() {
        return clientResource;
    }

    public String getBackchannelLogoutUrl() {
        return testSamlApp.getBackchannelLogoutUrl();
    }

    public Document getLastBackchannelLogoutDocument() {
        return testSamlApp.getLastBackchannelLogoutDocument();
    }

    public void clearLastBackchannelLogoutDocument() {
        testSamlApp.clearLastBackchannelLogoutDocument();
    }

    public void setBackchannelLogoutIssuer(String issuer) {
        testSamlApp.setBackchannelLogoutIssuer(issuer);
    }

    public void close() {
        if (clientResource != null) {
            clientResource.remove();
        }
    }

    private void fillLoginForm(String username, String password) {
        LoginPage loginPage = new LoginPage(managedWebDriver);
        PageFactory.initElements(managedWebDriver.driver(), loginPage);
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    private AuthnRequestType createAuthnRequest(boolean passive) throws ConfigurationException {
        SAML2Request samlReq = new SAML2Request();
        AuthnRequestType authnRequest = samlReq.createAuthnRequestType(
                UUID.randomUUID().toString(),
                assertionConsumerServiceUrl,
                getSamlEndpoint(),
                clientId);
        if (passive) {
            authnRequest.setIsPassive(true);
        }
        return authnRequest;
    }

    private Document createLogoutRequestDocument(String nameId, String sessionIndex) {
        try {
            String logoutXml = """
                    <samlp:LogoutRequest
                            xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                            xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                            ID="%s" Version="2.0" IssueInstant="%s"
                            Destination="%s">
                        <saml:Issuer>%s</saml:Issuer>
                        <saml:NameID>%s</saml:NameID>
                        <samlp:SessionIndex>%s</samlp:SessionIndex>
                    </samlp:LogoutRequest>
                    """.formatted(
                            "_" + UUID.randomUUID().toString(),
                            Instant.now().toString(),
                            getSamlEndpoint(),
                            clientId,
                            nameId,
                            sessionIndex);

            return DocumentUtil.getDocument(logoutXml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create LogoutRequest", e);
        }
    }

    private static final Pattern SAML_INPUT_PATTERN = Pattern.compile(
            "<input[^>]+name=\"(SAMLResponse|SAMLRequest)\"[^>]+value=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
}
