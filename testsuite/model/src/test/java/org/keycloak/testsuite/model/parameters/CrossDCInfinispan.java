/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.Config.Scope;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.testsuite.util.HotRodServerRule;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class CrossDCInfinispan extends KeycloakModelParameters {

    private final HotRodServerRule hotRodServerRule = new HotRodServerRule();

    private static final AtomicInteger NODE_COUNTER = new AtomicInteger();

    private Object lock = new Object();

    @Override
    public void updateConfig(Config cf) {
        synchronized (lock) {
            NODE_COUNTER.incrementAndGet();
            cf.spi("connectionsInfinispan")
                    .provider("default")
                    .config("embedded", "true")
                    .config("clustered", "true")
                    .config("remoteStoreEnabled", "true")
                    .config("useKeycloakTimeService", "true")
                    .config("remoteStoreSecurityEnabled", "false")
                    .config("nodeName", "node-" + NODE_COUNTER.get())
                    .config("siteName", siteName(NODE_COUNTER.get()))
                    .config("jgroupsUdpMcastAddr", mcastAddr(NODE_COUNTER.get()));
        }
    }

    public CrossDCInfinispan() {
        super(Infinispan.ALLOWED_SPIS, Infinispan.ALLOWED_FACTORIES);
    }

    @Override
    public void suiteRule(Config cf) {
        Scope config = cf.scope("connectionsInfinispan", "default");
        hotRodServerRule.createEmbeddedHotRodServer(config);
    }

    private static String siteName(int node) {
        return "site-" + (node % 2 == 0 ? 2 : 1);
    }

    private static String mcastAddr(int node) {
        return (node % 2 == 0) ? "228.6.7.8" : "228.5.6.7";
    }

    @Override
    public <T> Stream<T> getParameters(Class<T> clazz) {
        if (HotRodServerRule.class.isAssignableFrom(clazz)) {
            return Stream.of((T) hotRodServerRule);
        } else {
            return Stream.empty();
        }

    }

    @Override
    public void afterClass() {
        hotRodServerRule.getRemoteCacheManager().stop();
        //hotRodServer.stop();
        //hotrodCacheManager.stop();
    }
}
