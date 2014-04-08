package org.keycloak.timer.basic;

import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

import java.util.Timer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class BasicTimerProviderFactory implements TimerProviderFactory {

    private Timer timer;

    @Override
    public TimerProvider create() {
        return new BasicTimerProvider(timer);
    }

    @Override
    public void init() {
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

    @Override
    public boolean lazyLoad() {
        return true;
    }
}
