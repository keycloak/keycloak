package org.keycloak.timer.basic;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProviderFactory implements TimerProviderFactory {

    private Timer timer;

    private ConcurrentMap<String, TimerTask> scheduledTasks = new ConcurrentHashMap<String, TimerTask>();

    @Override
    public TimerProvider create(KeycloakSession session) {
        return new BasicTimerProvider(timer, this);
    }

    @Override
    public void init(Config.Scope config) {
        timer = new Timer();
    }

    @Override
    public void close() {
        timer.cancel();
        timer = null;
    }

    @Override
    public String getId() {
        return "basic";
    }

    protected TimerTask putTask(String taskName, TimerTask task) {
        return scheduledTasks.put(taskName, task);
    }

    protected TimerTask removeTask(String taskName) {
        return scheduledTasks.remove(taskName);
    }

}
