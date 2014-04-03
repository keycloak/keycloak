package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UsernameLoginFailureModel
{
   String getUsername();
   int getFailedLoginNotBefore();
   void setFailedLoginNotBefore(int notBefore);
   int getNumFailures();
   void incrementFailures();
   void clearFailures();
   long getLastFailure();
   void setLastFailure(long lastFailure);
   String getLastIPFailure();
   void setLastIPFailure(String ip);


}
