/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.connections.file;

import org.keycloak.models.KeycloakSession;

/**
 * Provides the InMemoryModel and notifies the factory to save it when
 * the session is done.
 * 
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class DefaultFileConnectionProvider implements FileConnectionProvider {

    private final DefaultFileConnectionProviderFactory factory;
    private final KeycloakSession session;
    private final InMemoryModel inMemoryModel;

    private boolean isRollbackOnly = false;

    public DefaultFileConnectionProvider(DefaultFileConnectionProviderFactory factory,
                                         KeycloakSession session,
                                         InMemoryModel inMemoryModel) {
        this.factory = factory;
        this.session = session;
        this.inMemoryModel = inMemoryModel;
    }

    @Override
    public InMemoryModel getModel() {
        return inMemoryModel;
    }

    @Override
    public void sessionClosed(KeycloakSession session) {
        factory.sessionClosed(session);
    }

    @Override
    public void close() {
    }

    @Override
    public void begin() {
    }

    @Override
    public void commit() {
        factory.commit(session);
    }

    @Override
    public void rollback() {
        factory.rollback(session);
    }

    @Override
    public void setRollbackOnly() {
        isRollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return isRollbackOnly;
    }

    @Override
    public boolean isActive() {
        return factory.isActive(session);
    }

}
