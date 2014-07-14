package org.keycloak.models.sessions.jpa;

import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.jpa.entities.UsernameLoginFailureEntity;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsernameLoginFailureAdapter implements UsernameLoginFailureModel
{
   protected UsernameLoginFailureEntity user;

   public UsernameLoginFailureAdapter(UsernameLoginFailureEntity user) {
      this.user = user;
   }

   @Override
   public String getUsername()
   {
      return user.getUsername();
   }

   @Override
   public int getFailedLoginNotBefore() {
      return user.getFailedLoginNotBefore();
   }

   @Override
   public void setFailedLoginNotBefore(int notBefore) {
      user.setFailedLoginNotBefore(notBefore);
   }

   @Override
   public int getNumFailures() {
      return user.getNumFailures();
   }

   @Override
   public void incrementFailures() {
      user.setNumFailures(getNumFailures() + 1);
   }

   @Override
   public void clearFailures() {
      user.setNumFailures(0);
   }

   @Override
   public long getLastFailure() {
      return user.getLastFailure();
   }

   @Override
   public void setLastFailure(long lastFailure) {
      user.setLastFailure(lastFailure);
   }

   @Override
   public String getLastIPFailure() {
      return user.getLastIPFailure();
   }

   @Override
   public void setLastIPFailure(String ip) {
      user.setLastIPFailure(ip);
   }

}
