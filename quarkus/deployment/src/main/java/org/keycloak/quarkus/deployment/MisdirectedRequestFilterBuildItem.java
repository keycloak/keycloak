/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.deployment;

import org.keycloak.quarkus.runtime.services.RejectMisdirectedRequestFilter;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

final class MisdirectedRequestFilterBuildItem extends SimpleBuildItem {

    private final RuntimeValue<RejectMisdirectedRequestFilter> filter;

    MisdirectedRequestFilterBuildItem(RuntimeValue<RejectMisdirectedRequestFilter> filter) {
        this.filter = filter;
    }

    RuntimeValue<RejectMisdirectedRequestFilter> getFilter() {
        return filter;
    }
}
