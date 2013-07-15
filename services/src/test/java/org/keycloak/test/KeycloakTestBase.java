package org.keycloak.test;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.test.BaseResourceTest;
import org.keycloak.representations.idm.RealmRepresentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakTestBase extends BaseResourceTest
{
   public static RealmRepresentation loadJson(String path) throws IOException
   {
      InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      int c;
      while ( (c = is.read()) != -1)
      {
         os.write(c);
      }
      byte[] bytes = os.toByteArray();
      System.out.println(new String(bytes));

      return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
   }
}
