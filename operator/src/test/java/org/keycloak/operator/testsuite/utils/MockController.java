/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.testsuite.utils;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

public abstract class MockController<T extends HasMetadata, R extends KubernetesDependentResource<T, Keycloak>> {

    protected final R dependentResource;
    protected final Keycloak keycloak;
    private Status status;

    protected MockController(R dependentResource, Keycloak keycloak) {
        this.dependentResource = dependentResource;
        this.keycloak = keycloak;
        this.status = Status.NEW;
    }

    public boolean reconciled() {
        return getReconciledResource().isPresent();
    }

    public boolean deleted() {
        return status == Status.DELETED;
    }

    public void setExists() {
        status = Status.EXISTS;
    }

    public Optional<T> getReconciledResource() {
        if (isEnabled()) {
            status = Status.EXISTS;
            return Optional.of(desired());
        }
        if (status == Status.EXISTS) {
            status = Status.DELETED;
        }
        return Optional.empty();
    }

    protected abstract boolean isEnabled();

    protected abstract T desired();

    private enum Status {
        NEW,
        EXISTS,
        DELETED,
    }
}
