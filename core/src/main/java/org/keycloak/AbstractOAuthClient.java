package org.keycloak;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.jboss.resteasy.util.BasicAuthHelper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractOAuthClient
{
   protected String clientId;
   protected String password;
   protected KeyStore truststore;
   protected String authUrl;
   protected String codeUrl;
   protected String stateCookieName = "OAuth_Token_Request_State";
   protected Client client;
   protected final AtomicLong counter = new AtomicLong();

   protected String getStateCode()
   {
      return counter.getAndIncrement() + "/" + UUID.randomUUID().toString();
   }

   public void start()
   {
      if (client == null)
      {
         client = new ResteasyClientBuilder().trustStore(truststore)
                 .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                 .connectionPoolSize(10)
                 .build();
      }
   }

   public void stop()
   {
      client.close();
   }

   public String getClientId()
   {
      return clientId;
   }

   public void setClientId(String clientId)
   {
      this.clientId = clientId;
   }

   public String getPassword()
   {
      return password;
   }

   public void setPassword(String password)
   {
      this.password = password;
   }

   public KeyStore getTruststore()
   {
      return truststore;
   }

   public void setTruststore(KeyStore truststore)
   {
      this.truststore = truststore;
   }

   public String getAuthUrl()
   {
      return authUrl;
   }

   public void setAuthUrl(String authUrl)
   {
      this.authUrl = authUrl;
   }

   public String getCodeUrl()
   {
      return codeUrl;
   }

   public void setCodeUrl(String codeUrl)
   {
      this.codeUrl = codeUrl;
   }

   public String getStateCookieName()
   {
      return stateCookieName;
   }

   public void setStateCookieName(String stateCookieName)
   {
      this.stateCookieName = stateCookieName;
   }

   public Client getClient()
   {
      return client;
   }

   public void setClient(Client client)
   {
      this.client = client;
   }

   public String resolveBearerToken(String redirectUri, String code)
   {
      redirectUri = stripOauthParametersFromRedirect(redirectUri);
      String authHeader = BasicAuthHelper.createHeader(clientId, password);
      Form codeForm = new Form()
              .param("grant_type", "authorization_code")
              .param("code", code)
              .param("redirect_uri", redirectUri);
      Response res = client.target(codeUrl).request().header(HttpHeaders.AUTHORIZATION, authHeader).post(Entity.form(codeForm));
      try
      {
         if (res.getStatus() == 400)
         {
            throw new BadRequestException();
         }
         else if (res.getStatus() != 200)
         {
            throw new InternalServerErrorException(new Exception("Unknown error when getting acess token"));
         }
         AccessTokenResponse tokenResponse = res.readEntity(AccessTokenResponse.class);
         return tokenResponse.getToken();
      }
      finally
      {
         res.close();
      }
   }

   protected String stripOauthParametersFromRedirect(String uri)
   {
      System.out.println("******************** redirect_uri: " + uri);
      UriBuilder builder = UriBuilder.fromUri(uri)
              .replaceQueryParam("code", null)
              .replaceQueryParam("state", null);
      return builder.build().toString();
   }

}
