package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.protocol.oid4vc.model.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.IDTokenRequest;
import org.keycloak.protocol.oid4vc.model.IDTokenRequestBuilder;
import org.keycloak.protocol.oid4vc.model.IDTokenResponse;
import org.keycloak.protocol.oid4vc.model.IDTokenResponseBuilder;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jboss.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.util.DIDUtils.encodeDidKey;

public class AuthorizationRequestRequest extends AbstractHttpGetRequest<AuthorizationRequestRequest, AuthorizationRequestResponse> {

    private static final Logger log = Logger.getLogger(AuthorizationRequestRequest.class);

    private final AuthorizationRequest authRequest;

    private CredentialIssuer issuerMetadata;
    private KeyPair keyPair;
    private String username;
    private String password;

    public AuthorizationRequestRequest(AbstractOAuthClient<?> client, AuthorizationRequest authRequest) {
        super(client);
        this.authRequest = authRequest;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    @Override
    protected void initRequest() {
    }

    public AuthorizationRequestRequest credentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public AuthorizationRequestRequest issuerMetadata(CredentialIssuer issuerMetadata) {
        this.issuerMetadata = issuerMetadata;
        return this;
    }

    public AuthorizationRequestRequest subjectKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
        return this;
    }

    public AuthorizationRequestResponse send() {
        initRequest();

        String clientId = authRequest.getClientId();
        String endpointUrl = authRequest.toRequestUrl(getEndpoint());
        log.infof("Send AuthorizationRequest: %s", JsonSerialization.valueAsString(authRequest));

        // Handle IDToken Authorization
        if (clientId.startsWith("did:")) {
            return handleIDTokenAuthorization(endpointUrl);
        }

        client.driver.navigate().to(endpointUrl);
        AuthPageState pageState = waitForLoginOrError();

        AuthorizationRequestResponse response;
        switch(pageState) {
            case LOGIN -> {
                client.fillLoginForm(username, password);
                response =  new AuthorizationRequestResponse(client);
            }

            case ERROR_REDIRECT -> {
                response = new AuthorizationRequestResponse(client);
            }

            case ERROR_PAGE -> {
                Map<String, String> params = new HashMap<>();
                params.put("error", "invalid_request");
                client.driver.findElements(By.id("kc-error-message")).stream()
                        .map(d -> d.getText().trim().split("\\n")[0])
                        .forEach(s -> params.put("error_description", s));
                response = new AuthorizationRequestResponse(params);
            }

            default -> {
                log.warn("Unknown AuthorizationResponse");
                response = new AuthorizationRequestResponse(client);
            }

        }
        return response;
    }

    @Override
    protected AuthorizationRequestResponse toResponse(CloseableHttpResponse response) {
        return null;
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private AuthorizationRequestResponse handleIDTokenAuthorization(String endpointUrl) {

        // Disable redirects for the HttpClients
        client.httpClient().set(HttpClients.custom().disableRedirectHandling().build());

        AuthorizationRequestResponse response;
        try {
            HttpGet get = new HttpGet(endpointUrl);

            String location = new AuthorizationRedirectResponse(client.httpClient().get().execute(get))
                    .getRedirectLocation();

            // Wallet receives the IDTokenRequest
            //
            IDTokenRequest idTokenRequest = IDTokenRequestBuilder.fromUri(location).build();
            log.infof("Received IDTokenRequest: %s", JsonSerialization.valueAsString(idTokenRequest));

            // Wallet gets the Issuer's PublicKey to verify the IDTokenRequest signature
            /*
            String kid = idTokenRequest.getJWSInput().getHeader().getKeyId();
            KeyMetadataRepresentation key = testRealm().keys().getKeyMetadata().getKeys().stream()
                    .filter(kmd -> kmd.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No key for: " + kid));
            PublicKey publicKey = DerUtils.decodePublicKey(key.getPublicKey(), key.getType());

            // Wallet verifies the IDTokenRequest
            //
            idTokenRequest.verify(publicKey);
            */

            // Wallet creates/sends the IDTokenResponse
            //
            IDTokenResponse idTokenResponse = createIDTokenResponse(idTokenRequest);

            log.infof("Send IDTokenResponse: %s", JsonSerialization.valueAsString(idTokenResponse));
            response = new IDTokenResponseRequest(client, idTokenRequest.getRedirectUri(), idTokenResponse).send();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            client.httpClient().reset();
        }
        return response;
    }

    private IDTokenResponse createIDTokenResponse(IDTokenRequest idTokenRequest) {
        String aud = issuerMetadata.getCredentialIssuer();
        String clientId = idTokenRequest.getClientId();

        ECPublicKey expPubKey = (ECPublicKey) keyPair.getPublic();
        String didKey = encodeDidKey(expPubKey);
        if (clientId.equals(didKey))
            throw new IllegalStateException("Unexpected IDToken client_id: " + clientId);

        IDTokenResponse idTokenResponse = new IDTokenResponseBuilder()
                .withJwtIssuer(didKey)
                .withJwtSubject(didKey)
                .withJwtAudience(aud)
                .sign(keyPair)
                .build();

        return idTokenResponse;
    }

    enum AuthPageState { LOGIN, ERROR_REDIRECT, ERROR_PAGE }

    private AuthPageState waitForLoginOrError() {
        WebDriverWait wait = new WebDriverWait(client.driver, Duration.ofSeconds(10));

        return wait.until(d -> {
            String url = d.getCurrentUrl();

            // Error via redirect (some flows)
            if (url != null && (url.contains("error=") || url.contains("error_description="))) {
                return AuthPageState.ERROR_REDIRECT;
            }

            // Error page rendered by Keycloak
            if (!d.findElements(By.id("kc-error-message")).isEmpty()) {
                return AuthPageState.ERROR_PAGE;
            }

            // Login form present
            if (!d.findElements(By.id("username")).isEmpty()
                    || !d.findElements(By.name("username")).isEmpty()) {
                return AuthPageState.LOGIN;
            }

            return null; // keep waiting
        });
    }
}
