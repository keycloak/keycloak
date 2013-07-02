package org.keycloak;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmConfiguration
{
   protected ResourceMetadata metadata;
   protected ResteasyClient client;
   protected UriBuilder authUrl;
   protected ResteasyWebTarget codeUrl;
   protected String clientId;
   protected Form credentials = new Form();
   protected boolean sslRequired = true;
   protected String stateCookieName = "OAuth_Token_Request_State";

   public ResourceMetadata getMetadata()
   {
      return metadata;
   }

   public void setMetadata(ResourceMetadata metadata)
   {
      this.metadata = metadata;
   }

   public ResteasyClient getClient()
   {
      return client;
   }

   public void setClient(ResteasyClient client)
   {
      this.client = client;
   }

   public UriBuilder getAuthUrl()
   {
      return authUrl;
   }

   public void setAuthUrl(UriBuilder authUrl)
   {
      this.authUrl = authUrl;
   }

   public String getClientId()
   {
      return clientId;
   }

   public void setClientId(String clientId)
   {
      this.clientId = clientId;
   }

   public Form getCredentials()
   {
      return credentials;
   }

   public ResteasyWebTarget getCodeUrl()
   {
      return codeUrl;
   }

   public void setCodeUrl(ResteasyWebTarget codeUrl)
   {
      this.codeUrl = codeUrl;
   }

   public boolean isSslRequired()
   {
      return sslRequired;
   }

   public void setSslRequired(boolean sslRequired)
   {
      this.sslRequired = sslRequired;
   }

   public String getStateCookieName()
   {
      return stateCookieName;
   }

   public void setStateCookieName(String stateCookieName)
   {
      this.stateCookieName = stateCookieName;
   }
}
