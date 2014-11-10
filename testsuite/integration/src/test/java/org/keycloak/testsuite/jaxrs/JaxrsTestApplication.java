package org.keycloak.testsuite.jaxrs;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JaxrsTestApplication extends Application {

    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    public JaxrsTestApplication(@Context ServletContext context) throws Exception {
        singletons.add(new JaxrsTestResource());

        String configFile = context.getInitParameter(JaxrsFilterTest.CONFIG_FILE_INIT_PARAM);
        JaxrsBearerTokenFilterImpl filter = new JaxrsBearerTokenFilterImpl();
        filter.setKeycloakConfigFile(configFile);
        singletons.add(filter);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
