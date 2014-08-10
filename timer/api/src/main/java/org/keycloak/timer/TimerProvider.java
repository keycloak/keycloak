package org.keycloak.timer;

import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface TimerProvider extends Provider {

    public void schedule(Runnable runnable, long interval, String taskName);

    public void cancelTask(String taskName);

}
