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
package org.keycloak.services;

import org.keycloak.models.KeycloakSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JtaRegistration {
    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private TransactionManager tm;

    public JtaRegistration() {
        try {
            InitialContext ctx = new InitialContext();
            tm = (TransactionManager)ctx.lookup("java:jboss/TransactionManager");
            if (tm == null) {
                logger.debug("Could not locate TransactionManager");
            }
        } catch (NamingException e) {
            logger.debug("Could not load TransactionManager", e);
        }

    }

    public void begin(KeycloakSession session) {
        if (tm == null) return;

        session.getTransactionManager().enlist(new JtaTransactionWrapper(tm));
    }
}
