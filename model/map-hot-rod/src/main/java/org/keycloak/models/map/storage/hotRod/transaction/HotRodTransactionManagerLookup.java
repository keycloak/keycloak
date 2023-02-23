/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.transaction;

import org.infinispan.client.hotrod.transaction.manager.RemoteTransactionManager;
import org.infinispan.commons.tx.lookup.TransactionManagerLookup;
import org.keycloak.models.KeycloakSession;
import org.keycloak.transaction.JtaTransactionManagerLookup;

import javax.transaction.TransactionManager;

/**
 * HotRod client provides its own {@link org.infinispan.client.hotrod.transaction.lookup.GenericTransactionManagerLookup}
 * that is able to locate variety of JTA transaction implementation present
 * in the runtime. We need to make sure we use JTA only when it is detected
 * by other parts of Keycloak (such as {@link org.keycloak.models.KeycloakTransactionManager}),
 * therefore we implemented this custom TransactionManagerLookup that locates
 * JTA transaction using {@link JtaTransactionManagerLookup} provider
 *
 */
public class HotRodTransactionManagerLookup implements TransactionManagerLookup {

    private final TransactionManager transactionManager;

    public HotRodTransactionManagerLookup(KeycloakSession session) {
        JtaTransactionManagerLookup jtaLookup = session.getProvider(JtaTransactionManagerLookup.class);
        TransactionManager txManager = jtaLookup != null ? jtaLookup.getTransactionManager() : null;
        transactionManager = txManager != null ? txManager : RemoteTransactionManager.getInstance();
    }

    @Override
    public TransactionManager getTransactionManager() throws Exception {
        return transactionManager;
    }

}
