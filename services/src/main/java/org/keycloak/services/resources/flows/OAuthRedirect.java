package org.keycloak.services.resources.flows;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.keycloak.AbstractOAuthClient;
import org.keycloak.OAuth2Constants;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Helper code to obtain oauth access tokens via browser redirects
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthRedirect extends AbstractOAuthClient {
    protected static final Logger logger = Logger.getLogger(OAuthRedirect.class);

    /**
     * closes client
     */
    public void stop() {
    }

    public Response redirect(UriInfo uriInfo, String redirectUri) {
        String state = getStateCode();

        UriBuilder uriBuilder = UriBuilder.fromUri(authUrl)
                .queryParam(OAuth2Constants.CLIENT_ID, clientId)
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.STATE, state)
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE);
        if (scope != null) {
            uriBuilder.queryParam(OAuth2Constants.SCOPE, scope);
        }

        URI url = uriBuilder.build();

        // todo httpOnly!
        NewCookie cookie = new NewCookie(getStateCookieName(), state, getStateCookiePath(uriInfo), null, null, -1, isSecure);
        logger.debug("NewCookie: " + cookie.toString());
        logger.debug("Oauth Redirect to: " + url);
        return Response.status(302)
                .location(url)
                .cookie(cookie).build();
    }

    public String getStateCookiePath(UriInfo uriInfo) {
        if (stateCookiePath != null) return stateCookiePath;
        return uriInfo.getBaseUri().getRawPath();
    }

    public String getError(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst(OAuth2Constants.ERROR);
    }

    public String getAccessCode(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst(OAuth2Constants.CODE);
    }

    public void checkStateCookie(UriInfo uriInfo, HttpHeaders headers) {
        Cookie stateCookie = headers.getCookies().get(stateCookieName);
        if (stateCookie == null) throw new BadRequestException("state cookie not set");
        String state = uriInfo.getQueryParameters().getFirst(OAuth2Constants.STATE);
        if (state == null) throw new BadRequestException("state parameter was null");
        if (!state.equals(stateCookie.getValue())) {
            throw new BadRequestException("state parameter invalid");
        }
    }
}
