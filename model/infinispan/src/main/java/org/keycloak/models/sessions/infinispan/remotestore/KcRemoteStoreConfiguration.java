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

package org.keycloak.models.sessions.infinispan.remotestore;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;
import org.infinispan.persistence.remote.configuration.ConnectionPoolConfiguration;
import org.infinispan.persistence.remote.configuration.ExecutorFactoryConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@BuiltBy(KcRemoteStoreConfigurationBuilder.class)
@ConfigurationFor(KcRemoteStore.class)
public class KcRemoteStoreConfiguration extends RemoteStoreConfiguration {

    public KcRemoteStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore,
                                    ExecutorFactoryConfiguration asyncExecutorFactory, ConnectionPoolConfiguration connectionPool) {
        super(attributes, async, singletonStore, asyncExecutorFactory, connectionPool);
    }
}
