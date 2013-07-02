package org.keycloak.services.model;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.internal.IdentityManagerFactory;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.SimpleAgent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmManager
{
   private static AtomicLong counter = new AtomicLong(1);

   public static String generateId()
   {
      return counter.getAndIncrement() + "-" + System.currentTimeMillis();
   }

   protected IdentityManagerFactory factory;

   public RealmManager(IdentityManagerFactory factory)
   {
      this.factory = factory;
   }

   public RealmModel getRealm(String id)
   {
      Realm existing = factory.findRealm(id);
      if (existing == null)
      {
         return null;
      }
      return new RealmModel(existing, factory);
   }

   public RealmModel create(String name)
   {
      Realm newRealm = factory.createRealm(generateId());
      IdentityManager idm = factory.createIdentityManager(newRealm);
      SimpleAgent agent = new SimpleAgent(RealmModel.REALM_AGENT_ID);
      idm.add(agent);
      return new RealmModel(newRealm, factory);
   }

}
