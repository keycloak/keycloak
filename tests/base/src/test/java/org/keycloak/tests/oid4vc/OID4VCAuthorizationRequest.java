package org.keycloak.tests.oid4vc;

import java.util.List;

import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

/**
 * The Authorization request/response follows the known pattern that we have for other message exchanges on
 * 'OID4VCClient'. The response does not only capture a successful authorization, but (like the other APIs)
 * also error cases, like ...
 * <p>
 * - login page does not get displayed, because of an invalid request
 * - invalid credentials on login page (i.e. redirect url without code)
 * <p>
 * In all three cases (i.e. success + 2x error cases), we like to return a response as soon as we have it,
 * instead of waiting for a timeout like we currently have with authorization through the 'OAuthClient'.
 * <p>
 * This API also abstracts to the low level details of web driver and login forms that are specific for the interactive
 * Authorization Code Flow. In future, we can use the same API for non-interactive flows (e.g. as required by EBSI)
 * <p>
 * Historically, this Authorization request abstractions was with the 'OID4VCClient', rather than with the 'OAuthClient'
 * because we did not want to expose all Keycloak tests to go through this still experimental code. It never got
 * approved there and is now only available through the Wallet. We can assume that code similar to this would be found
 * in a real OID4VCI Wallet.
 * <p>
 * 'OID4VCPublicClientTest' covers authorization success and the error cases.
 */
public class OID4VCAuthorizationRequest {

    private final LoginUrlBuilder loginForm;
    private final OAuthClient oauth;

    public OID4VCAuthorizationRequest(OAuthClient oauth) {
        this.loginForm = oauth.loginForm();
        this.oauth = oauth;
    }

    public OID4VCAuthorizationRequest authorizationDetails(OID4VCAuthorizationDetail authDetail) {
        loginForm.authorizationDetails(List.of(authDetail));
        return this;
    }

    public OID4VCAuthorizationRequest codeChallenge(PkceGenerator pkce) {
        loginForm.codeChallenge(pkce);
        return this;
    }

    public OID4VCAuthorizationRequest issuerState(String issuerState) {
        loginForm.issuerState(issuerState);
        return this;
    }

    public OID4VCAuthorizationRequest dcql(String dcqlQuery) {
        loginForm.param("query", dcqlQuery);
        return this;
    }

    public OID4VCAuthorizationRequest request(String request) {
        loginForm.request(request);
        return this;
    }

    public OID4VCAuthorizationRequest scope(String... scopes) {
        if (scopes != null && scopes.length > 0) {
            loginForm.scope(scopes);
        }
        return this;
    }

    public boolean openLoginForm() {
        loginForm.open();
        String currUrl = oauth.getDriver().getCurrentUrl();
        return currUrl != null && !currUrl.contains("error=") && !currUrl.contains("error_description=");
    }

    public OID4VCAuthorizationRequest fillLoginForm(String username, String password) {
        oauth.fillLoginForm(username, password);
        return this;
    }

    public AuthorizationEndpointResponse parseLoginResponse() {
        return oauth.parseLoginResponse();
    }

    public OID4VCAuthorizationResponse send(String username, String password) {
        openLoginForm();
        fillLoginForm(username, password);
        return new OID4VCAuthorizationResponse(parseLoginResponse());
    }

    public OID4VCAuthorizationResponse send() {
        String responseUrl = "https://example.com/response#vp_token=oid4vc_natural_person:VPToken";
        String responseType = OIDCResponseType.NONE;
        String responseMode = OIDCResponseMode.FRAGMENT.value();
        String redirectUri = "https://example.com/response";
        return new OID4VCAuthorizationResponse(responseUrl, responseType, responseMode, redirectUri);
    }
}
