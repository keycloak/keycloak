package org.jboss.resteasy.example.oauth;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.SkeletonKeySession;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProductDatabaseClient
{
   public static List<String> getProducts(HttpServletRequest request)
   {
      SkeletonKeySession session = (SkeletonKeySession)request.getAttribute(SkeletonKeySession.class.getName());
      ResteasyClient client = new ResteasyClientBuilder()
                 .trustStore(session.getMetadata().getTruststore())
                 .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY).build();
      try
      {
         Response response = client.target("http://localhost:8080/database/products").request()
                 .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.getTokenString()).get();
         return response.readEntity(new GenericType<List<String>>(){});
      }
      finally
      {
         client.close();
      }
   }
}
