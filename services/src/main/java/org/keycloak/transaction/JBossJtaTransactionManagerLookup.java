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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.transaction.TransactionManager;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JBossJtaTransactionManagerLookup implements JtaTransactionManagerLookup {
    private static final Logger logger = Logger.getLogger(JBossJtaTransactionManagerLookup.class);
    private TransactionManager tm;

    @Override
    public TransactionManager getTransactionManager() {
        return tm;
    }

    @Override
    public void init(Config.Scope config) {
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

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return "jboss";
    }
}
