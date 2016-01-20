package org.keycloak.adapters.jetty.spi;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WrappingSessionHandler extends SessionHandler {

    public WrappingSessionHandler() {
        super();
    }

    public WrappingSessionHandler(SessionManager mgr) {
        super(mgr);
    }

    @Override
    public void setHandler(Handler handler) {
        if (getHandler() != null && getHandler() instanceof HandlerWrapper) {
            HandlerWrapper wrappedHandler = (HandlerWrapper) getHandler();
            wrappedHandler.setHandler(handler);
        } else {
            super.setHandler(handler);
        }
    }
}
