package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.enums.TokenStore;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.KeycloakUriBuilder;
import org.keycloak.util.UriUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthRequestAuthenticator {
    private static final Logger log = Logger.getLogger(OAuthRequestAuthenticator.class);
    protected KeycloakDeployment deployment;
    protected RequestAuthenticator reqAuthenticator;
    protected int sslRedirectPort;
    protected AdapterTokenStore tokenStore;
    protected String tokenString;
    protected String idTokenString;
    protected IDToken idToken;
    protected AccessToken token;
    protected HttpFacade facade;
    protected AuthChallenge challenge;
    protected String refreshToken;
    protected String strippedOauthParametersRequestUri;

    public OAuthRequestAuthenticator(RequestAuthenticator requestAuthenticator, HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort, AdapterTokenStore tokenStore) {
        this.reqAuthenticator = requestAuthenticator;
        this.facade = facade;
        this.deployment = deployment;
        this.sslRedirectPort = sslRedirectPort;
        this.tokenStore = tokenStore;
    }

    public AuthChallenge getChallenge() {
        return challenge;
    }

    public String getTokenString() {
        return tokenString;
    }

    public AccessToken getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getIdTokenString() {
        return idTokenString;
    }

    public void setIdTokenString(String idTokenString) {
        this.idTokenString = idTokenString;
    }

    public IDToken getIdToken() {
        return idToken;
    }

    public void setIdToken(IDToken idToken) {
        this.idToken = idToken;
    }

    public String getStrippedOauthParametersRequestUri() {
        return strippedOauthParametersRequestUri;
    }

    public void setStrippedOauthParametersRequestUri(String strippedOauthParametersRequestUri) {
        this.strippedOauthParametersRequestUri = strippedOauthParametersRequestUri;
    }

    protected String getRequestUrl() {
        return facade.getRequest().getURI();
    }

    protected boolean isRequestSecure() {
        return facade.getRequest().isSecure();
    }

    protected HttpFacade.Cookie getCookie(String cookieName) {
        return facade.getRequest().getCookie(cookieName);
    }

    protected String getCookieValue(String cookieName) {
        HttpFacade.Cookie cookie = getCookie(cookieName);
        if (cookie == null) return null;
        return cookie.getValue();
    }

    protected String getQueryParamValue(String paramName) {
        return facade.getRequest().getQueryParamValue(paramName);
    }

    protected String getError() {
        return getQueryParamValue(OAuth2Constants.ERROR);
    }

    protected String getCode() {
        return getQueryParamValue(OAuth2Constants.CODE);
    }

    protected String getRedirectUri(String state) {
        String url = getRequestUrl();
        log.debugf("callback uri: %s", url);
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            int port = sslRedirectPort();
            if (port < 0) {
                // disabled?
                return null;
            }
            KeycloakUriBuilder secureUrl = KeycloakUriBuilder.fromUri(url).scheme("https").port(-1);
            if (port != 443) secureUrl.port(port);
            url = secureUrl.build().toString();
        }

        String loginHint = getQueryParamValue("login_hint");
        url = UriUtils.stripQueryParam(url,"login_hint");

        String idpHint = getQueryParamValue(AdapterConstants.KC_IDP_HINT);
        url = UriUtils.stripQueryParam(url, AdapterConstants.KC_IDP_HINT);

        KeycloakUriBuilder redirectUriBuilder = deployment.getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, deployment.getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, url)
                .queryParam(OAuth2Constants.STATE, state)
                .queryParam("login", "true");
        if(loginHint != null && loginHint.length() > 0){
            redirectUriBuilder.queryParam("login_hint",loginHint);
        }
        if (idpHint != null && idpHint.length() > 0) {
            redirectUriBuilder.queryParam(AdapterConstants.KC_IDP_HINT,idpHint);
        }

        return redirectUriBuilder.build().toString();
    }

    protected int sslRedirectPort() {
        return sslRedirectPort;
    }

    protected static final AtomicLong counter = new AtomicLong();

    protected String getStateCode() {
        return counter.getAndIncrement() + "/" + UUID.randomUUID().toString();
    }

    protected AuthChallenge loginRedirect() {
        final String state = getStateCode();
        final String redirect = getRedirectUri(state);
        if (redirect == null) {
            return new AuthChallenge() {
                @Override
                public boolean challenge(HttpFacade exchange) {
                    exchange.getResponse().setStatus(403);
                    return true;
                }

                @Override
                public boolean errorPage() {
                    return true;
                }
            };
        }
        return new AuthChallenge() {

            @Override
            public boolean errorPage() {
                return false;
            }

            @Override
            public boolean challenge(HttpFacade exchange) {
                tokenStore.saveRequest();
                log.debug("Sending redirect to login page: " + redirect);
                exchange.getResponse().setStatus(302);
                exchange.getResponse().setCookie(deployment.getStateCookieName(), state, /* need to set path? */ null, null, -1, deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr()), false);
                exchange.getResponse().setHeader("Location", redirect);
                return true;
            }
        };
    }

    protected AuthChallenge checkStateCookie() {
        HttpFacade.Cookie stateCookie = getCookie(deployment.getStateCookieName());

        if (stateCookie == null) {
            log.warn("No state cookie");
            return challenge(400);
        }
        // reset the cookie
        log.debug("** reseting application state cookie");
        facade.getResponse().resetCookie(deployment.getStateCookieName(), stateCookie.getPath());
        String stateCookieValue = getCookieValue(deployment.getStateCookieName());

        String state = getQueryParamValue(OAuth2Constants.STATE);
        if (state == null) {
            log.warn("state parameter was null");
            return challenge(400);
        }
        if (!state.equals(stateCookieValue)) {
            log.warn("state parameter invalid");
            log.warn("cookie: " + stateCookieValue);
            log.warn("queryParam: " + state);
            return challenge(400);
        }
        return null;

    }

    public AuthOutcome authenticate() {
        String code = getCode();
        if (code == null) {
            log.debug("there was no code");
            String error = getError();
            if (error != null) {
                // todo how do we send a response?
                log.warn("There was an error: " + error);
                challenge = challenge(400);
                return AuthOutcome.FAILED;
            } else {
                log.debug("redirecting to auth server");
                challenge = loginRedirect();
                return AuthOutcome.NOT_ATTEMPTED;
            }
        } else {
            log.debug("there was a code, resolving");
            challenge = resolveCode(code);
            if (challenge != null) {
                return AuthOutcome.FAILED;
            }
            return AuthOutcome.AUTHENTICATED;
        }

    }

    protected AuthChallenge challenge(final int code) {
        return new AuthChallenge() {
            @Override
            public boolean errorPage() {
                return true;
            }

            @Override
            public boolean challenge(HttpFacade exchange) {
                exchange.getResponse().setStatus(code);
                return true;
            }
        };
    }

    /**
     * Start or continue the oauth login process.
     * <p/>
     * if code query parameter is not present, then browser is redirected to authUrl.  The redirect URL will be
     * the URL of the current request.
     * <p/>
     * If code query parameter is present, then an access token is obtained by invoking a secure request to the codeUrl.
     * If the access token is obtained, the browser is again redirected to the current request URL, but any OAuth
     * protocol specific query parameters are removed.
     *
     * @return null if an access token was obtained, otherwise a challenge is returned
     */
    protected AuthChallenge resolveCode(String code) {
        // abort if not HTTPS
        if (!isRequestSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.error("Adapter requires SSL. Request: " + facade.getRequest().getURI());
            return challenge(403);
        }

        log.debug("checking state cookie for after code");
        AuthChallenge challenge = checkStateCookie();
        if (challenge != null) return challenge;

        AccessTokenResponse tokenResponse = null;
        strippedOauthParametersRequestUri = stripOauthParametersFromRedirect();
        try {
            // For COOKIE store we don't have httpSessionId and single sign-out won't be available
            String httpSessionId = deployment.getTokenStore() == TokenStore.SESSION ? reqAuthenticator.getHttpSessionId(true) : null;
            tokenResponse = ServerRequest.invokeAccessCodeToToken(deployment, code, strippedOauthParametersRequestUri, httpSessionId);
        } catch (ServerRequest.HttpFailure failure) {
            log.error("failed to turn code into token");
            log.error("status from server: " + failure.getStatus());
            if (failure.getStatus() == 400 && failure.getError() != null) {
                log.error("   " + failure.getError());
            }
            return challenge(403);

        } catch (IOException e) {
            log.error("failed to turn code into token", e);
            return challenge(403);
        }

        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();
        try {
            token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());
            if (idTokenString != null) {
                JWSInput input = new JWSInput(idTokenString);
                try {
                    idToken = input.readJsonContent(IDToken.class);
                } catch (IOException e) {
                    throw new VerificationException();
                }
            }
            log.debug("Token Verification succeeded!");
        } catch (VerificationException e) {
            log.error("failed verification of token");
            return challenge(403);
        }
        if (tokenResponse.getNotBeforePolicy() > deployment.getNotBefore()) {
            deployment.setNotBefore(tokenResponse.getNotBeforePolicy());
        }
        if (token.getIssuedAt() < deployment.getNotBefore()) {
            log.error("Stale token");
            return challenge(403);
        }
        log.debug("successful authenticated");
        return null;
    }

    /**
     * strip out unwanted query parameters and redirect so bookmarks don't retain oauth protocol bits
     */
    protected String stripOauthParametersFromRedirect() {
        KeycloakUriBuilder builder = KeycloakUriBuilder.fromUri(facade.getRequest().getURI())
                .replaceQueryParam(OAuth2Constants.CODE, null)
                .replaceQueryParam(OAuth2Constants.STATE, null);
        return builder.build().toString();
    }


}
