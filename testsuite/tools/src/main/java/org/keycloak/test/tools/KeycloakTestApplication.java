package org.keycloak.test.tools;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakTestApplication extends Application {

    protected ProviderSessionFactory providerSessionFactory;
    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    public KeycloakTestApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        KeycloakApplication.loadConfig();

        this.providerSessionFactory = KeycloakApplication.createProviderSessionFactory();

        context.setAttribute(ProviderSessionFactory.class.getName(), this.providerSessionFactory);

        singletons.add(new PerfTools(providerSessionFactory));
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