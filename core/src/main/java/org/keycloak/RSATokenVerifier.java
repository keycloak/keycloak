package org.keycloak;

import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.SkeletonKeyToken;

import java.io.IOException;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSATokenVerifier
{
   public static SkeletonKeyToken verifyToken(String tokenString, ResourceMetadata metadata) throws VerificationException
   {
      PublicKey realmKey = metadata.getRealmKey();
      String realm = metadata.getRealm();
      String resource = metadata.getResourceName();
      JWSInput input = new JWSInput(tokenString);
      boolean verified = false;
      try
      {
         verified = RSAProvider.verify(input, realmKey);
      }
      catch (Exception ignore)
      {

      }
      if (!verified) throw new VerificationException("Token signature not validated");

      SkeletonKeyToken token = null;
      try
      {
         token = JsonSerialization.fromBytes(SkeletonKeyToken.class, input.getContent());
      }
      catch (IOException e)
      {
         throw new VerificationException(e);
      }
      if (!token.isActive())
      {
         throw new VerificationException("Token is not active.");
      }
      String user = token.getPrincipal();
      if (user == null)
      {
         throw new VerificationException("Token user was null");
      }
      if (!realm.equals(token.getAudience()))
      {
         throw new VerificationException("Token audience doesn't match domain");

      }
      return token;
   }
}
