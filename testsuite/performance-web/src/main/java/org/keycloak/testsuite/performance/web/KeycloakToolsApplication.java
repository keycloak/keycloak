package org.keycloak.testsuite.performance.web;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.test.tools.PerfTools;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.util.HashSet;
import java.util.Set;

/**
 * Modified version of {@link org.keycloak.test.tools.KeycloakTestApplication}, which shares ProviderSessionFactory with KeycloakApplication
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakToolsApplication extends Application {

    protected KeycloakSessionFactory keycloakSessionFactory;
    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    public KeycloakToolsApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        this.keycloakSessionFactory = KeycloakSessionFactoryHolder.getKeycloakSessionFactory();
        context.setAttribute(KeycloakSessionFactory.class.getName(), this.keycloakSessionFactory);
        singletons.add(new PerfTools(keycloakSessionFactory));
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
