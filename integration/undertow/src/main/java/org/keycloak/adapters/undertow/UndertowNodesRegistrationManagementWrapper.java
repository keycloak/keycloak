package org.keycloak.adapters.undertow;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.keycloak.adapters.NodesRegistrationManagement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowNodesRegistrationManagementWrapper implements ServletContextListener {

    private final NodesRegistrationManagement delegate;

    public UndertowNodesRegistrationManagementWrapper(NodesRegistrationManagement delegate) {
        this.delegate = delegate;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        delegate.stop();
    }
}
