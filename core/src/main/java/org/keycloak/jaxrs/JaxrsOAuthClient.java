package org.keycloak.jaxrs;

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
public class JaxrsOAuthClient extends AbstractOAuthClient
{
   public Response redirect(UriInfo uriInfo, String redirectUri)
   {
      String state = getStateCode();

      URI url = UriBuilder.fromUri(authUrl)
              .queryParam("client_id", clientId)
              .queryParam("redirect_uri", redirectUri)
              .queryParam("state", state)
              .build();
      NewCookie cookie = new NewCookie(stateCookieName, state, uriInfo.getBaseUri().getPath(), null, null, -1, true);
      return Response.status(302)
              .location(url)
              .cookie(cookie).build();
   }

   public String getBearerToken(UriInfo uriInfo, HttpHeaders headers) throws BadRequestException, InternalServerErrorException
   {
      String error = uriInfo.getQueryParameters().getFirst("error");
      if (error != null) throw new BadRequestException(new Exception("OAuth error: " + error));
      Cookie stateCookie = headers.getCookies().get(stateCookieName);
      if (stateCookie == null) throw new BadRequestException(new Exception("state cookie not set"));;

      String state = uriInfo.getQueryParameters().getFirst("state");
      if (state == null) throw new BadRequestException(new Exception("state parameter was null"));
      if (!state.equals(stateCookie.getValue()))
      {
         throw new BadRequestException(new Exception("state parameter invalid"));
      }
      String code = uriInfo.getQueryParameters().getFirst("code");
      if (code == null) throw new BadRequestException(new Exception("code parameter was null"));
      return resolveBearerToken(uriInfo.getRequestUri().toString(), code);
   }
}
