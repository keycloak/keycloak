package org.keycloak;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.ApplicationPath;

import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.common.util.Resteasy;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.quarkus.QuarkusPlatform;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    @Inject
    Instance<EntityManagerFactory> entityManagerFactory;
    
    @Override
    protected void startup() {
        try {
            forceEntityManagerInitialization();
            initializeKeycloakSessionFactory();
            setupScheduledTasks(sessionFactory);
        } catch (Throwable cause) {
            QuarkusPlatform.exitOnError(cause);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        //TODO: a temporary hack for https://github.com/quarkusio/quarkus/issues/9647, we need to disable the sanitizer to avoid
        // escaping text/html responses from the server
        Resteasy.getContextData(ResteasyDeployment.class).setProperty(ResteasyContextParameters.RESTEASY_DISABLE_HTML_SANITIZER, Boolean.TRUE);

        HashSet<Object> singletons = new HashSet<>(super.getSingletons().stream().filter(new Predicate<Object>() {
            @Override
            public boolean test(Object o) {
                return !WelcomeResource.class.isInstance(o);
            }
        }).collect(Collectors.toSet()));

        singletons.add(new QuarkusWelcomeResource());

        return singletons;
    }

    private void initializeKeycloakSessionFactory() {
        QuarkusKeycloakSessionFactory instance = QuarkusKeycloakSessionFactory.getInstance();
        sessionFactory = instance;
        instance.init();
        sessionFactory.publish(new PostMigrationEvent());
    }

    private void forceEntityManagerInitialization() {
        // also forces an initialization of the entity manager so that providers don't need to wait for any initialization logic
        // when first creating an entity manager
        entityManagerFactory.get().createEntityManager().close();
    }
}
