package org.keycloak;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.ApplicationPath;

import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    @Inject
    Instance<EntityManagerFactory> entityManagerFactory;
    
    @Override
    protected void startup() {
        forceEntityManagerInitialization();
        initializeKeycloakSessionFactory();
        setupScheduledTasks(sessionFactory);
    }

    @Override
    public Set<Object> getSingletons() {
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
