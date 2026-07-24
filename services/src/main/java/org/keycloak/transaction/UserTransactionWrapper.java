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
package org.keycloak.transaction;

import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.keycloak.models.KeycloakTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserTransactionWrapper implements KeycloakTransaction {
    protected UserTransaction ut;

    public UserTransactionWrapper(UserTransaction ut) {
        this.ut = ut;
    }

    @Override
    public void begin() {
        try {
            ut.begin();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commit() {
        try {
            ut.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            ut.rollback();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void setRollbackOnly() {
        try {
            ut.setRollbackOnly();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return ut.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isActive() {
        try {
            return ut.getStatus() == Status.STATUS_ACTIVE;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
