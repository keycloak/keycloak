package org.keycloak.services.service;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.model.RealmManager;
import org.keycloak.services.model.RealmModel;
import org.keycloak.services.model.UserCredentialModel;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/registrations")
public class RegistrationService
{
   protected RealmManager adapter;
   protected RealmModel defaultRealm;

   @Context
   protected UriInfo uriInfo;

   public RegistrationService(RealmManager adapter)
   {
      this.adapter = adapter;
      defaultRealm = adapter.getRealm(Realm.DEFAULT_REALM);
   }



   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   public Response register(UserRepresentation newUser)
   {
      User user = defaultRealm.getIdm().getUser(newUser.getUsername());
      if (user != null)
      {
         return Response.status(400).type("text/plain").entity("user exists").build();
      }

      user = new SimpleUser(newUser.getUsername());
      defaultRealm.getIdm().add(user);
      for (UserRepresentation.Credential cred : newUser.getCredentials())
      {
         UserCredentialModel credModel = new UserCredentialModel();
         credModel.setType(cred.getType());
         credModel.setValue(cred.getValue());
         defaultRealm.updateCredential(user, credModel);
      }
      URI uri = uriInfo.getBaseUriBuilder().path(RealmFactory.class).path(user.getLoginName()).build();
      return Response.created(uri).build();
   }


}
