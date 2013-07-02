package org.keycloak.services.service;

import org.keycloak.services.IdentityManagerAdapter;
import org.keycloak.services.model.data.RealmModel;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.PublishedRealmRepresentation;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/")
public class RealmResource
{
   protected Logger logger = Logger.getLogger(RealmResource.class);
   protected IdentityManagerAdapter identityManager;
   @Context
   protected UriInfo uriInfo;

   public RealmResource(IdentityManagerAdapter identityManager)
   {
      this.identityManager = identityManager;
   }

   @GET
   @Path("realms/{realm}")
   @Produces("application/json")
   public PublishedRealmRepresentation getRealm(@PathParam("realm") String id)
   {
      RealmModel realm = identityManager.getRealm(id);
      if (realm == null)
      {
         logger.debug("realm not found");
         throw new NotFoundException();
      }
      return realmRep(realm, uriInfo);
   }

   @GET
   @Path("realms/{realm}.html")
   @Produces("text/html")
   public String getRealmHtml(@PathParam("realm") String id)
   {
      RealmModel realm = identityManager.getRealm(id);
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

      html.append("<html><body><h1>Realm: ").append(realm.getName()).append("</h1>");
      html.append("<p>auth: ").append(authUri).append("</p>");
      html.append("<p>code: ").append(codeUri).append("</p>");
      html.append("<p>grant: ").append(grantUrl).append("</p>");
      html.append("<p>public key: ").append(realm.getPublicKeyPem()).append("</p>");
      html.append("</body></html>");

      return html.toString();
   }


   @GET
   @Path("realms")
   @Produces("application/json")
   public Response getRealmsByName(@QueryParam("name") String name)
   {
      if (name == null) return Response.noContent().build();
      List<RealmModel> realms = identityManager.getRealmsByName(name);
      if (realms.size() == 0) return Response.noContent().build();

      List<PublishedRealmRepresentation> list = new ArrayList<PublishedRealmRepresentation>();
      for (RealmModel realm : realms)
      {
         list.add(realmRep(realm, uriInfo));
      }
      GenericEntity<List<PublishedRealmRepresentation>> entity = new GenericEntity<List<PublishedRealmRepresentation>>(list){};
      return Response.ok(entity).type(MediaType.APPLICATION_JSON_TYPE).build();
   }

   @GET
   @Path("realms.html")
   @Produces("text/html")
   public String getRealmsByNameHtml(@QueryParam("name") String name)
   {
      if (name == null) return "<html><body><h1>No realms with that name</h1></body></html>";
      List<RealmModel> realms = identityManager.getRealmsByName(name);
      if (realms.size() == 0) return "<html><body><h1>No realms with that name</h1></body></html>";
      if (realms.size() == 1) return realmHtml(realms.get(0));

      StringBuffer html = new StringBuffer();
      html.append("<html><body><h1>Realms</h1>");
      for (RealmModel realm : realms)
      {
         html.append("<p><a href=\"").append(uriInfo.getBaseUriBuilder().path("realms").path(realm.getId() + ".html"))
                 .append("\">").append(realm.getId()).append("</a></p>");
      }
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
      return rep;
   }
}
