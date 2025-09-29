/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.transaction;

import org.keycloak.provider.Spi;

public class InfinispanTransactionSpi implements Spi {

    private static final String ID = "infinispanTransactions";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Class<InfinispanTransactionProvider> getProviderClass() {
        return InfinispanTransactionProvider.class;
    }

    @Override
    public Class<InfinispanTransactionProviderFactory> getProviderFactoryClass() {
        return InfinispanTransactionProviderFactory.class;
    }
}
