package org.keycloak.timer.basic;

import org.jboss.logging.Logger;
import org.keycloak.timer.TimerProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProvider implements TimerProvider {

    private static final Logger logger = Logger.getLogger(BasicTimerProvider.class);

    private final Timer timer;
    private final BasicTimerProviderFactory factory;

    public BasicTimerProvider(Timer timer, BasicTimerProviderFactory factory) {
        this.timer = timer;
        this.factory = factory;
    }

    @Override
    public void schedule(final Runnable runnable, final long interval, String taskName) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };

        TimerTask existingTask = factory.putTask(taskName, task);
        if (existingTask != null) {
            logger.debugf("Existing timer task '%s' found. Cancelling it", taskName);
            existingTask.cancel();
        }

        logger.debugf("Starting task '%s' with interval '%d'", taskName, interval);
        timer.schedule(task, interval, interval);
    }

    @Override
    public void cancelTask(String taskName) {
        TimerTask existingTask = factory.removeTask(taskName);
        if (existingTask != null) {
            logger.debugf("Cancelling task '%s'", taskName);
            existingTask.cancel();
        }
    }

    @Override
    public void close() {
        // do nothing
    }

}
