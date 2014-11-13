package org.keycloak.adapters.tomcat;

import java.util.List;

import org.apache.catalina.Manager;
import org.keycloak.adapters.UserSessionManagement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CatalinaUserSessionManagementWrapper implements UserSessionManagement {

    private final CatalinaUserSessionManagement delegate;
    private final Manager sessionManager;

    public CatalinaUserSessionManagementWrapper(CatalinaUserSessionManagement delegate, Manager sessionManager) {
        this.delegate = delegate;
        this.sessionManager = sessionManager;
    }

    @Override
    public void logoutAll() {
        delegate.logoutAll(sessionManager);
    }

    @Override
    public void logoutHttpSessions(List<String> ids) {
        delegate.logoutHttpSessions(sessionManager, ids);
    }
}
