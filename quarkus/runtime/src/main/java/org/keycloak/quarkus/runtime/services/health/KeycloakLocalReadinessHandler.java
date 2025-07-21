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

package org.keycloak.quarkus.runtime.services.health;

import org.keycloak.utils.SecureContextResolver;

import io.quarkus.smallrye.health.runtime.SmallRyeReadinessHandler;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import jakarta.json.JsonObject;
import jakarta.json.spi.JsonProvider;

public class KeycloakLocalReadinessHandler extends SmallRyeReadinessHandler {

    private static final JsonObject DUMMY_HEALTH = JsonProvider.provider().createObjectBuilder().add("status", "UNKNOWN").build();

    @Override
    protected Uni<SmallRyeHealth> getHealth(SmallRyeHealthReporter reporter, RoutingContext ctx) {
        // similar to the isLocal check performed in WelcomeResource
        var request = ctx.request();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String forwarded = request.getHeader("Forwarded");
        SocketAddress remoteAddress = request.remoteAddress();
        // if not local, just return a dummy response
        if (xForwardedFor != null || forwarded != null
                || (remoteAddress != null && !SecureContextResolver.isLocalAddress(remoteAddress.hostAddress()))) {
            return Uni.createFrom().item(new SmallRyeHealth(DUMMY_HEALTH));
        }
        // what about lb-check? it's separate from the smallrye checks
        return super.getHealth(reporter, ctx);
    }

}