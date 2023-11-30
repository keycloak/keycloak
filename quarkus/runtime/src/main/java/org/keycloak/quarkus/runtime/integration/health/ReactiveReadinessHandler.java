/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.health;

import io.quarkus.arc.Arc;
import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;

/**
 * @author Alexander Schwartz
 */
public class ReactiveReadinessHandler extends ReactiveHealthHandler {

    @Override
    protected Uni<SmallRyeHealth> getHealth() {
        SmallRyeHealthReporter healthReporter = Arc.container().instance(SmallRyeHealthReporter.class).get();
        return healthReporter.getReadinessAsync();
    }
}
