package org.keycloak.adapters.undertow;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.keycloak.adapters.NodesRegistrationLifecycle;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowNodesRegistrationLifecycleWrapper implements ServletContextListener {

    private final NodesRegistrationLifecycle delegate;

    public UndertowNodesRegistrationLifecycleWrapper(NodesRegistrationLifecycle delegate) {
        this.delegate = delegate;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        delegate.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        delegate.stop();
    }
}
