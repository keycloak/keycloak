package org.keycloak.timer.basic;

import org.keycloak.timer.TimerProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProvider implements TimerProvider {

    private Timer timer;

    public BasicTimerProvider(Timer timer) {

        this.timer = timer;
    }

    @Override
    public void schedule(final Runnable runnable, String config) {
        long interval = Long.parseLong(config);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };

        timer.schedule(task, interval, interval);
    }

    @Override
    public void close() {
        // do nothing
    }

}
