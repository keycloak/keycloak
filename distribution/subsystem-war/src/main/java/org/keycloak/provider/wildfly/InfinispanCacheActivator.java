package org.keycloak.provider.wildfly;

import org.jboss.msc.service.*;
import org.keycloak.Config;

import java.util.List;

/**
 * Used to add a dependency on Infinispan caches to make sure they are started.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheActivator implements ServiceActivator {

    private static final ServiceName cacheContainerService = ServiceName.of("jboss", "infinispan", "keycloak");

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        if (context.getServiceRegistry().getService(cacheContainerService) != null) {
            ServiceTarget st = context.getServiceTarget();
            st.addDependency(cacheContainerService);
            st.addDependency(cacheContainerService.append("realms"));
            st.addDependency(cacheContainerService.append("users"));
            st.addDependency(cacheContainerService.append("sessions"));
            st.addDependency(cacheContainerService.append("loginFailures"));
        }
    }

}
