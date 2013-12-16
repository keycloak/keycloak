package org.keycloak;

import junit.framework.Assert;
import org.junit.Test;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SkeletonKeyTokenTest
{
   @Test
   public void testScope() throws Exception
   {
     SkeletonKeyScope scope2 = new SkeletonKeyScope();

      scope2.add("one", "admin");
      scope2.add("one", "buyer");
      scope2.add("two", "seller");
       String json = JsonSerialization.writeValueAsString(scope2);
      System.out.println(json);


   }

   @Test
   public void testToken() throws Exception
   {
      SkeletonKeyToken token = new SkeletonKeyToken();
      token.id("111");
      token.addAccess("foo").addRole("admin");
      token.addAccess("bar").addRole("user");

       String json = JsonSerialization.writeValueAsString(token);
      System.out.println(json);

      token = JsonSerialization.readValue(json, SkeletonKeyToken.class);
      Assert.assertEquals("111", token.getId());
      SkeletonKeyToken.Access foo = token.getResourceAccess("foo");
      Assert.assertNotNull(foo);
      Assert.assertTrue(foo.isUserInRole("admin"));

   }

   @Test
   public void testRSA() throws Exception
   {
      SkeletonKeyToken token = new SkeletonKeyToken();
      token.id("111");
      token.addAccess("foo").addRole("admin");
      token.addAccess("bar").addRole("user");

      KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

      String encoded = new JWSBuilder()
              .jsonContent(token)
              .rsa256(keyPair.getPrivate());

      System.out.println(encoded);

      JWSInput input = new JWSInput(encoded);

      token = input.readJsonContent(SkeletonKeyToken.class);
      Assert.assertEquals("111", token.getId());
      Assert.assertTrue(RSAProvider.verify(input, keyPair.getPublic()));
   }
}
