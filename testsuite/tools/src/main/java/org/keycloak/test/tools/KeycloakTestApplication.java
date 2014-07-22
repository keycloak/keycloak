package org.keycloak.test.tools;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSessionFactory;
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

    protected KeycloakSessionFactory sessionFactory;
    protected Set<Class<?>> classes = new HashSet<Class<?>>();
    protected Set<Object> singletons = new HashSet<Object>();

    static Mail mail = new Mail();

    public KeycloakTestApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        KeycloakApplication.loadConfig();

        this.sessionFactory = KeycloakApplication.createSessionFactory();

        context.setAttribute(KeycloakSessionFactory.class.getName(), this.sessionFactory);

        singletons.add(new PerfTools(sessionFactory));
        singletons.add(mail);
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