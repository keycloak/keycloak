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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.core.ResteasyContext;
import org.keycloak.common.util.ResteasyProvider;

/**
 * TODO: we should probably rely on the vert.x routing context instead of resteasy context data
 */
public class ResteasyVertxProvider implements ResteasyProvider {

    @Override
    public <R> R getContextData(Class<R> type) {
        R data = ResteasyContext.getContextData(type);

        if (data == null) {
            RoutingContext contextData = ResteasyContext.getContextData(RoutingContext.class);

            if (contextData == null) {
                return null;
            }

            return (R) contextData.data().get(type.getName());
        }

        return data;
    }

    @Override
    public void pushDefaultContextObject(Class type, Object instance) {
        ResteasyContext.getContextData(org.jboss.resteasy.spi.Dispatcher.class).getDefaultContextObjects()
                .put(type, instance);
    }

    @Override
    public void pushContext(Class type, Object instance) {
        ResteasyContext.pushContext(type, instance);
    }

    @Override
    public void clearContextData() {
        ResteasyContext.clearContextData();
    }

}
