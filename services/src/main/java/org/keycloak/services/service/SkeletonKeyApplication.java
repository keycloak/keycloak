package org.keycloak.services.service;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.keycloak.SkeletonKeyContextResolver;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ApplicationPath("/")
public class SkeletonKeyApplication extends Application
{
   protected Set<Object> singletons = new HashSet<Object>();
   protected Set<Class<?>> classes = new HashSet<Class<?>>();

   public SkeletonKeyApplication()
   {
      Cache cache = getCache();
      singletons.add(new TokenService(null));
      singletons.add(new RealmFactory(null));
      singletons.add(new RealmResource(null));
      classes.add(SkeletonKeyContextResolver.class);
   }

   @Override
   public Set<Class<?>> getClasses()
   {
      return classes;
   }

   @Override
   public Set<Object> getSingletons()
   {
      return singletons;
   }

   protected Cache getCache()
   {
      try
      {
         InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("skeleton-key.xml");
         return new DefaultCacheManager(is).getCache("skeleton-key");
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }
}
