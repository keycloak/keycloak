package org.keycloak.example.oauth;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DataApplication extends Application
{
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
