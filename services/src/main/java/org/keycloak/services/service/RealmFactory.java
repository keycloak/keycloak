package org.keycloak.services.service;

import org.keycloak.services.model.RealmManager;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.model.RealmModel;
import org.picketlink.idm.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("/realmfactory")
public class RealmFactory
{
   protected RealmManager adapter;

   @Context
   protected UriInfo uriInfo;

   public RealmFactory(RealmManager adapter)
   {
      this.adapter = adapter;
   }


   @POST
   @Consumes("application/json")
   public Response importDomain(RealmRepresentation rep)
   {
      RealmModel realm = createRealm(rep);
      UriBuilder builder = uriInfo.getRequestUriBuilder().path(realm.getId());
      return Response.created(builder.build())
              //.entity(RealmResource.realmRep(realm, uriInfo))
              .type(MediaType.APPLICATION_JSON_TYPE).build();
   }

   protected RealmModel createRealm(RealmRepresentation rep)
   {
      //verifyRealmRepresentation(rep);

      RealmModel realm = adapter.create(rep.getRealm());
      KeyPair keyPair = null;
      try
      {
         keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
      }
      catch (NoSuchAlgorithmException e)
      {
         throw new RuntimeException(e);
      }
      realm.setPrivateKey(keyPair.getPrivate());
      realm.setPublicKey(keyPair.getPublic());
      realm.setName(rep.getRealm());
      realm.setEnabled(rep.isEnabled());
      realm.setTokenLifespan(rep.getTokenLifespan());
      realm.setAccessCodeLifespan(rep.getAccessCodeLifespan());
      realm.setSslNotRequired(rep.isSslNotRequired());
      realm.updateRealm();


      Map<String, User> userMap = new HashMap<String, User>();
      return null;
   }/*
      RoleModel adminRole = identityManager.create(realm, "admin");

      for (RequiredCredentialRepresentation requiredCred : rep.getRequiredCredentials())
      {
         RequiredCredentialModel credential = new RequiredCredentialModel();
         credential.setType(requiredCred.getType());
         credential.setInput(requiredCred.isInput());
         credential.setSecret(requiredCred.isSecret());
         identityManager.create(realm, credential);
      }

      for (UserRepresentation userRep : rep.getUsers())
      {
         UserModel user = new UserModel();
         user.setUsername(userRep.getUsername());
         user.setEnabled(userRep.isEnabled());
         user = identityManager.create(realm, user);
         userMap.put(user.getUsername(), user);
         if (userRep.getCredentials() != null)
         {
            for (UserRepresentation.Credential cred : userRep.getCredentials())
            {
               UserCredentialModel credential = new UserCredentialModel();
               credential.setType(cred.getType());
               credential.setValue(cred.getValue());
               credential.setHashed(cred.isHashed());
               identityManager.create(user, credential);
            }
         }

         if (userRep.getAttributes() != null)
         {
            for (Map.Entry<String, String> entry : userRep.getAttributes().entrySet())
            {
               UserAttributeModel attribute = new UserAttributeModel();
               attribute.setName(entry.getKey());
               attribute.setValue(entry.getValue());
               identityManager.create(user, attribute);
            }
         }
      }

      for (RoleMappingRepresentation mapping : rep.getRoleMappings())
      {
         RoleMappingModel roleMapping = createRoleMapping(userMap, mapping);
         UserModel user = userMap.get(mapping.getUsername());
         identityManager.create(realm, user, roleMapping);
      }

      if (rep.getScopeMappings() != null)
      {
         for (ScopeMappingRepresentation scope : rep.getScopeMappings())
         {
            ScopeMappingModel scopeMapping = createScopeMapping(userMap, scope);
            UserModel user = userMap.get(scope.getUsername());
            identityManager.create(realm, user, scopeMapping);

         }
      }

      if (rep.getResources() != null)
      {
         for (ResourceRepresentation resourceRep : rep.getResources())
         {
            ResourceModel resource = new ResourceModel();
            resource.setName(resourceRep.getName());
            resource.setSurrogateAuthRequired(resourceRep.isSurrogateAuthRequired());
            resource = identityManager.create(realm, resource);
            if (resourceRep.getRoles() != null)
            {
               for (String role : resourceRep.getRoles())
               {
                  RoleModel r = identityManager.create(realm, resource, role);
               }
            }
            if (resourceRep.getRoleMappings() != null)
            {
               for (RoleMappingRepresentation mapping : resourceRep.getRoleMappings())
               {
                  RoleMappingModel roleMapping = createRoleMapping(userMap, mapping);
                  UserModel user = userMap.get(mapping.getUsername());
                  identityManager.create(realm, resource, user, roleMapping);
               }
            }
            if (resourceRep.getScopeMappings() != null)
            {
               for (ScopeMappingRepresentation mapping : resourceRep.getScopeMappings())
               {
                  ScopeMappingModel scopeMapping = createScopeMapping(userMap, mapping);
                  UserModel user = userMap.get(mapping.getUsername());
                  identityManager.create(realm, resource, user, scopeMapping);
               }
            }

         }
      }
      return realm;
   }

   protected RoleMappingModel createRoleMapping(Map<String, UserModel> userMap, RoleMappingRepresentation mapping)
   {
      RoleMappingModel roleMapping = new RoleMappingModel();
      UserModel user = userMap.get(mapping.getUsername());
      roleMapping.setUserid(user.getId());
      if (mapping.getSurrogates() != null)
      {
         for (String s : mapping.getSurrogates())
         {
            UserModel surrogate = userMap.get(s);
            roleMapping.getSurrogateIds().add(surrogate.getId());

         }
      }
      for (String role : mapping.getRoles())
      {
         roleMapping.getRoles().add(role);
      }
      return roleMapping;
   }

   protected ScopeMappingModel createScopeMapping(Map<String, UserModel> userMap, ScopeMappingRepresentation mapping)
   {
      ScopeMappingModel scopeMapping = new ScopeMappingModel();
      UserModel user = userMap.get(mapping.getUsername());
      scopeMapping.setUserid(user.getId());
      for (String role : mapping.getRoles())
      {
         scopeMapping.getRoles().add(role);
      }
      return scopeMapping;
   }


   protected void verifyRealmRepresentation(RealmRepresentation rep)
   {
      if (rep.getUsers() == null)
      {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                 .entity("No realm admin users defined for realm").type("text/plain").build());
      }

      if (rep.getRequiredCredentials() == null)
      {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                 .entity("Realm credential requirements not defined").type("text/plain").build());

      }

      if (rep.getRoleMappings() == null)
      {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                 .entity("No realm admin users defined for realm").type("text/plain").build());
      }

      HashMap<String, UserRepresentation> userReps = new HashMap<String, UserRepresentation>();
      for (UserRepresentation userRep : rep.getUsers()) userReps.put(userRep.getUsername(), userRep);

      // make sure there is a user that has admin privileges for the realm
      Set<UserRepresentation> admins = new HashSet<UserRepresentation>();
      for (RoleMappingRepresentation mapping : rep.getRoleMappings())
      {
         if (!userReps.containsKey(mapping.getUsername()))
         {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("No users declared for role mapping").type("text/plain").build());

         }
         for (String role : mapping.getRoles())
         {
            if (!role.equals("admin"))
            {
               throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                       .entity("There is only an 'admin' role for realms").type("text/plain").build());

            }
            else
            {
               admins.add(userReps.get(mapping.getUsername()));
            }
         }
      }
      if (admins.size() == 0)
      {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                 .entity("No realm admin users defined for realm").type("text/plain").build());
      }

      // override enabled to false if user does not have at least all of browser or client credentials
      for (UserRepresentation userRep : rep.getUsers())
      {
         if (!userRep.isEnabled())
         {
            admins.remove(userRep);
            continue;
         }
         if (userRep.getCredentials() == null)
         {
            admins.remove(userRep);
            userRep.setEnabled(false);
         }
         else
         {
            boolean hasBrowserCredentials = true;
            for (RequiredCredentialRepresentation credential : rep.getRequiredCredentials())
            {
               boolean hasCredential = false;
               for (UserRepresentation.Credential cred : userRep.getCredentials())
               {
                  if (cred.getType().equals(credential.getType()))
                  {
                     hasCredential = true;
                     break;
                  }
               }
               if (!hasCredential)
               {
                  hasBrowserCredentials = false;
                  break;
               }
            }
            if (!hasBrowserCredentials)
            {
               userRep.setEnabled(false);
               admins.remove(userRep);
            }

         }
      }

      if (admins.size() == 0)
      {
         throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                 .entity("No realm admin users are enabled or have appropriate credentials").type("text/plain").build());
      }

      if (rep.getResources() != null)
      {
         // check mappings
         for (ResourceRepresentation resourceRep : rep.getResources())
         {
            if (resourceRep.getRoleMappings() != null)
            {
               for (RoleMappingRepresentation mapping : resourceRep.getRoleMappings())
               {
                  if (!userReps.containsKey(mapping.getUsername()))
                  {
                     throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                             .entity("No users declared for role mapping").type("text/plain").build());

                  }
                  if (mapping.getSurrogates() != null)
                  {
                     for (String surrogate : mapping.getSurrogates())
                     {
                        if (!userReps.containsKey(surrogate))
                        {
                           throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                   .entity("No users declared for role mapping surrogate").type("text/plain").build());
                        }
                     }
                  }
                  for (String role : mapping.getRoles())
                  {
                     if (!resourceRep.getRoles().contains(role))
                     {
                        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                                .entity("No resource role for role mapping").type("text/plain").build());
                     }
                  }
               }
            }
         }
      }
   }
   */

}
