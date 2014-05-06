package org.keycloak.example.services;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@ApplicationPath("/rest")
public class DataApplication extends Application
{
    public DataApplication(@Context ServletContext context) {
        AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext)context.getAttribute(AdapterDeploymentContext.class.getName());
        AdapterConfig config = new AdapterConfig();
        config.setDisableTrustManager(true);
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> set = new HashSet<Class<?>>();
        set.add(CustomerService.class);
        set.add(ProductService.class);
        return set;
    }

    @Override
    public Set<Object> getSingletons() {
        return super.getSingletons();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
