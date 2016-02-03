/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
