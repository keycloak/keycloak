/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model.parameters;

import org.keycloak.cluster.infinispan.InfinispanClusterProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionSpi;
import org.keycloak.models.session.UserSessionPersisterSpi;
import org.keycloak.models.sessions.infinispan.InfinispanAuthenticationSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserLoginFailureProviderFactory;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.sessions.AuthenticationSessionSpi;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;
import org.keycloak.sessions.StickySessionEncoderSpi;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.models.cache.CacheRealmProviderSpi;
import org.keycloak.models.cache.CacheUserProviderSpi;
import org.keycloak.models.cache.infinispan.InfinispanCacheRealmProviderFactory;
import org.keycloak.models.cache.infinispan.InfinispanUserCacheProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.testsuite.model.Config;
import com.google.common.collect.ImmutableSet;
import org.keycloak.timer.TimerProviderFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author hmlnarik
 */
public class Infinispan extends KeycloakModelParameters {

    private static final AtomicInteger NODE_COUNTER = new AtomicInteger();

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(AuthenticationSessionSpi.class)
      .add(CacheRealmProviderSpi.class)
      .add(CacheUserProviderSpi.class)
      .add(InfinispanConnectionSpi.class)
      .add(StickySessionEncoderSpi.class)
      .add(UserSessionPersisterSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(InfinispanAuthenticationSessionProviderFactory.class)
      .add(InfinispanCacheRealmProviderFactory.class)
      .add(InfinispanClusterProviderFactory.class)
      .add(InfinispanConnectionProviderFactory.class)
      .add(InfinispanUserCacheProviderFactory.class)
      .add(InfinispanUserSessionProviderFactory.class)
      .add(InfinispanUserLoginFailureProviderFactory.class)
      .add(StickySessionEncoderProviderFactory.class)
      .add(TimerProviderFactory.class)
      .build();

    @Override
    public void updateConfig(Config cf) {
        cf.spi("connectionsInfinispan")
            .provider("default")
              .config("embedded", "true")
              .config("clustered", "true")
              .config("useKeycloakTimeService", "true")
              .config("nodeName", "node-" + NODE_COUNTER.incrementAndGet());
    }

    public Infinispan() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }
}
