package org.keycloak.services.service;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.model.RealmManager;
import org.keycloak.services.model.RealmModel;
import org.keycloak.services.model.RequiredCredentialModel;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.TOTPCredentials;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.User;

import javax.ws.rs.core.MultivaluedMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationManager
{
   protected Logger logger = Logger.getLogger(AuthenticationManager.class);
   public static final String FORM_USERNAME = "username";
   protected RealmManager adapter;

   public AuthenticationManager(RealmManager adapter)
   {
      this.adapter = adapter;
   }

   public boolean authenticate(RealmModel realm, User user, MultivaluedMap<String, String> formData)
   {
      String username = user.getLoginName();
      Set<String> types = new HashSet<String>();

      for (RequiredCredentialModel credential : realm.getRequiredCredentials())
      {
         types.add(credential.getType());
      }

      if (types.contains(RequiredCredentialRepresentation.PASSWORD))
      {
         String password = formData.getFirst(RequiredCredentialRepresentation.PASSWORD);
         if (password == null)
         {
            logger.warn("Password not provided");
            return false;
         }

         if (types.contains(RequiredCredentialRepresentation.TOTP))
         {
            String token = formData.getFirst(RequiredCredentialRepresentation.TOTP);
            if (token == null)
            {
               logger.warn("TOTP token not provided");
               return false;
            }
            TOTPCredentials creds = new TOTPCredentials();
            creds.setToken(token);
            creds.setUsername(username);
            creds.setPassword(new Password(password));
            realm.getIdm().validateCredentials(creds);
            if (creds.getStatus() != Credentials.Status.VALID)
            {
               return false;
            }
         }
         else
         {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, new Password(password));
            realm.getIdm().validateCredentials(creds);
            if (creds.getStatus() != Credentials.Status.VALID)
            {
               return false;
            }
         }
      }
      else
      {
         logger.warn("Do not know how to authenticate user");
         return false;
      }
      return true;
   }
}
