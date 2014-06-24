package org.keycloak.testsuite.performance.web;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.test.tools.PerfTools;

/**
 * Modified version of {@link org.keycloak.test.tools.KeycloakTestApplication}, which shares ProviderSessionFactory with KeycloakApplication
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakToolsApplication extends Application {

    protected ProviderSessionFactory providerSessionFactory;
    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    public KeycloakToolsApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        this.providerSessionFactory = ProviderSessionFactoryHolder.getProviderSessionFactory();
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
