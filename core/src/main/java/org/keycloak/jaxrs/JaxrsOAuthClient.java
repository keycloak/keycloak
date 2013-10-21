package org.keycloak.jaxrs;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.AbstractOAuthClient;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
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
public class JaxrsOAuthClient extends AbstractOAuthClient {
    protected static final Logger logger = Logger.getLogger(JaxrsOAuthClient.class);
    public Response redirect(UriInfo uriInfo, String redirectUri) {
        return redirect(uriInfo, redirectUri, null);
    }

    public Response redirect(UriInfo uriInfo, String redirectUri, String path) {
        String state = getStateCode();
        if (path != null) {
            state += "#" + path;
        }

        URI url = UriBuilder.fromUri(authUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build();
        NewCookie cookie = new NewCookie(getStateCookieName(), state, getStateCookiePath(uriInfo), null, null, -1, isSecure, true);
        logger.info("NewCookie: " + cookie.toString());
        return Response.status(302)
                .location(url)
                .cookie(cookie).build();
    }

    public String getStateCookiePath(UriInfo uriInfo) {
        if (stateCookiePath != null) return stateCookiePath;
        return uriInfo.getBaseUri().getPath();
    }

    public String getBearerToken(UriInfo uriInfo, HttpHeaders headers) throws BadRequestException, InternalServerErrorException {
        String error = getError(uriInfo);
        if (error != null) throw new BadRequestException(new Exception("OAuth error: " + error));
        checkStateCookie(uriInfo, headers);
        String code = getAccessCode(uriInfo);
        if (code == null) throw new BadRequestException(new Exception("code parameter was null"));
        return resolveBearerToken(uriInfo.getRequestUri().toString(), code);
    }

    public String getError(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst("error");
    }

    public String getAccessCode(UriInfo uriInfo) {
        return uriInfo.getQueryParameters().getFirst("code");
    }

    public String checkStateCookie(UriInfo uriInfo, HttpHeaders headers) {
        Cookie stateCookie = headers.getCookies().get(stateCookieName);
        if (stateCookie == null) throw new BadRequestException("state cookie not set");
        String state = uriInfo.getQueryParameters().getFirst("state");
        if (state == null) throw new BadRequestException("state parameter was null");
        if (!state.equals(stateCookie.getValue())) {
            throw new BadRequestException("state parameter invalid");
        }
        if (state.indexOf('#') != -1) {
            return state.substring(state.indexOf('#') + 1);
        } else {
            return null;
        }
    }
}
