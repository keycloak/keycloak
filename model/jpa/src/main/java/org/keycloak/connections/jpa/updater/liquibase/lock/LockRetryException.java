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

package org.keycloak.connections.jpa.updater.liquibase.lock;

/**
 * Indicates that retrieve lock wasn't successful, but it worth to retry it in different transaction (For example if we were trying to create LOCK table, but other transaction
 * created the table in the meantime etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LockRetryException extends RuntimeException {

    public LockRetryException(String message) {
        super(message);
    }

    public LockRetryException(Throwable cause) {
        super(cause);
    }

    public LockRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
