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

package org.keycloak.models.sessions.infinispan;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.sessions.StickySessionEncoderProvider;
import org.keycloak.sessions.StickySessionEncoderProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanStickySessionEncoderProviderFactory implements StickySessionEncoderProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanStickySessionEncoderProviderFactory.class);


    private boolean shouldAttachRoute;

    @Override
    public StickySessionEncoderProvider create(KeycloakSession session) {
        return new InfinispanStickySessionEncoderProvider(session, shouldAttachRoute);
    }

    @Override
    public void init(Config.Scope config) {
        this.shouldAttachRoute = config.getBoolean("shouldAttachRoute", true);
        log.debugf("Should attach route to the sticky session cookie: %b", shouldAttachRoute);

    }

    // Used for testing
    public void setShouldAttachRoute(boolean shouldAttachRoute) {
        this.shouldAttachRoute = shouldAttachRoute;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "infinispan";
    }
}
