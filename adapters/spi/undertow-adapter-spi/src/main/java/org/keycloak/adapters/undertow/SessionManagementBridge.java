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
package org.keycloak.adapters.undertow;

import io.undertow.server.session.SessionManager;
import org.keycloak.adapters.spi.UserSessionManagement;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SessionManagementBridge implements UserSessionManagement {

    protected UndertowUserSessionManagement userSessionManagement;
    protected SessionManager sessionManager;

    public SessionManagementBridge(UndertowUserSessionManagement userSessionManagement, SessionManager sessionManager) {
        this.userSessionManagement = userSessionManagement;
        this.sessionManager = sessionManager;
    }

    @Override
    public void logoutAll() {
        userSessionManagement.logoutAll(sessionManager);
    }

    @Override
    public void logoutHttpSessions(List<String> ids) {
        userSessionManagement.logoutHttpSessions(sessionManager, ids);
    }

}
