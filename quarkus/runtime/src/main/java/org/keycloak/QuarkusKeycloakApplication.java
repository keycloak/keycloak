package org.keycloak;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.ApplicationPath;

import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.quarkus.QuarkusPlatform;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.QuarkusWelcomeResource;
import org.keycloak.services.resources.WelcomeResource;

@ApplicationPath("/")
public class QuarkusKeycloakApplication extends KeycloakApplication {

    private static boolean filterSingletons(Object o) {
        return !WelcomeResource.class.isInstance(o);
    }

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
        Set<Object> singletons = super.getSingletons().stream()
                .filter(QuarkusKeycloakApplication::filterSingletons)
                .collect(Collectors.toSet());

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
