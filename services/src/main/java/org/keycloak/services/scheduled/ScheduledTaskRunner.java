package org.keycloak.services.scheduled;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ScheduledTaskRunner implements Runnable {

    private static final Logger logger = Logger.getLogger(ScheduledTaskRunner.class);

    private final KeycloakSessionFactory sessionFactory;
    private final ScheduledTask task;

    public ScheduledTaskRunner(KeycloakSessionFactory sessionFactory, ScheduledTask task) {
        this.sessionFactory = sessionFactory;
        this.task = task;
    }

    @Override
    public void run() {
        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            task.run(session);
            session.getTransaction().commit();

            logger.debug("Executed scheduled task " + task.getClass().getSimpleName());
        } catch (Throwable t) {
            logger.error("Failed to run scheduled task " + task.getClass().getSimpleName(), t);

            session.getTransaction().rollback();
        } finally {
            try {
                session.close();
            } catch (Throwable t) {
                logger.error("Failed to close ProviderSession", t);
            }
        }
    }

}
