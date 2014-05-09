package org.keycloak.services.scheduled;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ScheduledTaskRunner implements Runnable {

    private static final Logger logger = Logger.getLogger(ScheduledTaskRunner.class);

    private final KeycloakSessionFactory keycloakSessionFactory;
    private final ProviderSessionFactory providerSessionFactory;
    private final ScheduledTask task;

    public ScheduledTaskRunner(KeycloakSessionFactory keycloakSessionFactory, ProviderSessionFactory providerSessionFactory, ScheduledTask task) {
        this.keycloakSessionFactory = keycloakSessionFactory;
        this.providerSessionFactory = providerSessionFactory;
        this.task = task;
    }

    @Override
    public void run() {
        KeycloakSession keycloakSession = keycloakSessionFactory.createSession();
        ProviderSession providerSession = providerSessionFactory.createSession();
        try {
            keycloakSession.getTransaction().begin();
            task.run(keycloakSession, providerSession);
            keycloakSession.getTransaction().commit();

            logger.debug("Executed scheduled task " + task.getClass().getSimpleName());
        } catch (Throwable t) {
            logger.error("Failed to run scheduled task " + task.getClass().getSimpleName(), t);

            keycloakSession.getTransaction().rollback();
        } finally {
            try {
                keycloakSession.close();
            } catch (Throwable t) {
                logger.error("Failed to close KeycloakSession", t);
            }
            try {
                providerSession.close();
            } catch (Throwable t) {
                logger.error("Failed to close ProviderSession", t);
            }
        }
    }

}
