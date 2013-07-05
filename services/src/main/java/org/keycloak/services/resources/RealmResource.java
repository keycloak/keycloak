package org.keycloak.services.resources;

import org.keycloak.services.models.RealmManager;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.PublishedRealmRepresentation;
import org.keycloak.services.models.RealmModel;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/realms")
public class RealmResource
{
   protected Logger logger = Logger.getLogger(RealmResource.class);
   protected RealmManager adapter;
   @Context
   protected UriInfo uriInfo;

   public RealmResource(RealmManager adapter)
   {
      this.adapter = adapter;
   }

   @GET
   @Path("{realm}")
   @Produces("application/json")
   public PublishedRealmRepresentation getRealm(@PathParam("realm") String id)
   {
      RealmModel realm = adapter.getRealm(id);
      if (realm == null)
      {
         logger.debug("realm not found");
         throw new NotFoundException();
      }
      return realmRep(realm, uriInfo);
   }

   @GET
   @Path("{realm}.html")
   @Produces("text/html")
   public String getRealmHtml(@PathParam("realm") String id)
   {
      RealmModel realm = adapter.getRealm(id);
      if (realm == null)
      {
         logger.debug("realm not found");
         throw new NotFoundException();
      }
      return realmHtml(realm);
   }

   private String realmHtml(RealmModel realm)
   {
      StringBuffer html = new StringBuffer();

      UriBuilder auth = uriInfo.getBaseUriBuilder();
      auth.path(TokenService.class)
              .path(TokenService.class, "requestAccessCode");
      String authUri = auth.build(realm.getId()).toString();

      UriBuilder code = uriInfo.getBaseUriBuilder();
      code.path(TokenService.class).path(TokenService.class, "accessRequest");
      String codeUri = code.build(realm.getId()).toString();

      UriBuilder grant = uriInfo.getBaseUriBuilder();
      grant.path(TokenService.class).path(TokenService.class, "accessTokenGrant");
      String grantUrl = grant.build(realm.getId()).toString();

      UriBuilder idGrant = uriInfo.getBaseUriBuilder();
      grant.path(TokenService.class).path(TokenService.class, "identityTokenGrant");
      String idGrantUrl = idGrant.build(realm.getId()).toString();

      html.append("<html><body><h1>Realm: ").append(realm.getName()).append("</h1>");
      html.append("<p>auth: ").append(authUri).append("</p>");
      html.append("<p>code: ").append(codeUri).append("</p>");
      html.append("<p>grant: ").append(grantUrl).append("</p>");
      html.append("<p>identity grant: ").append(idGrantUrl).append("</p>");
      html.append("<p>public key: ").append(realm.getPublicKeyPem()).append("</p>");
      html.append("</body></html>");

      return html.toString();
   }


   public static PublishedRealmRepresentation realmRep(RealmModel realm, UriInfo uriInfo)
   {
      PublishedRealmRepresentation rep = new PublishedRealmRepresentation();
      rep.setRealm(realm.getName());
      rep.setSelf(uriInfo.getRequestUri().toString());
      rep.setPublicKeyPem(realm.getPublicKeyPem());

      UriBuilder auth = uriInfo.getBaseUriBuilder();
      auth.path(TokenService.class)
              .path(TokenService.class, "requestAccessCode");
      rep.setAuthorizationUrl(auth.build(realm.getId()).toString());

      UriBuilder code = uriInfo.getBaseUriBuilder();
      code.path(TokenService.class).path(TokenService.class, "accessRequest");
      rep.setCodeUrl(code.build(realm.getId()).toString());

      UriBuilder grant = uriInfo.getBaseUriBuilder();
      grant.path(TokenService.class).path(TokenService.class, "accessTokenGrant");
      String grantUrl = grant.build(realm.getId()).toString();
      rep.setGrantUrl(grantUrl);

      UriBuilder idGrant = uriInfo.getBaseUriBuilder();
      grant.path(TokenService.class).path(TokenService.class, "identityTokenGrant");
      String idGrantUrl = idGrant.build(realm.getId()).toString();
      rep.setIdentityGrantUrl(idGrantUrl);
      return rep;
   }
}
