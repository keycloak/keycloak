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

package org.keycloak.models.cache.infinispan.locking;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LockingConnectionProviderFactory extends DefaultInfinispanConnectionProviderFactory {
    public static final String VERSION_CACHE_NAME = "realmVersions";

    protected static final Logger logger = Logger.getLogger(LockingConnectionProviderFactory.class);

    @Override
    public String getId() {
        return "locking";
    }


    protected void initEmbedded() {
        super.initEmbedded();
        ConfigurationBuilder counterConfigBuilder = new ConfigurationBuilder();
        counterConfigBuilder.invocationBatching().enable()
                .transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        counterConfigBuilder.transaction().transactionManagerLookup(new DummyTransactionManagerLookup());
        counterConfigBuilder.transaction().lockingMode(LockingMode.PESSIMISTIC);
        Configuration counterCacheConfiguration = counterConfigBuilder.build();

        cacheManager.defineConfiguration(VERSION_CACHE_NAME, counterCacheConfiguration);
    }

}
