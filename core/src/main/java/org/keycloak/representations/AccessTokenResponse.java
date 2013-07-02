package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * OAuth 2.0 Access Token Response json
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccessTokenResponse
{
   @JsonProperty("access_token")
   protected String token;

   @JsonProperty("expires_in")
   protected long expiresIn;

   @JsonProperty("refresh_token")
   protected String refreshToken;

   @JsonProperty("token_type")
   protected String tokenType;

   public String getToken()
   {
      return token;
   }

   public void setToken(String token)
   {
      this.token = token;
   }

   public long getExpiresIn()
   {
      return expiresIn;
   }

   public void setExpiresIn(long expiresIn)
   {
      this.expiresIn = expiresIn;
   }

   public String getRefreshToken()
   {
      return refreshToken;
   }

   public void setRefreshToken(String refreshToken)
   {
      this.refreshToken = refreshToken;
   }

   public String getTokenType()
   {
      return tokenType;
   }

   public void setTokenType(String tokenType)
   {
      this.tokenType = tokenType;
   }
}
