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

package org.keycloak.models.sessions.infinispan;

import java.util.Objects;

import org.keycloak.models.RevokedTokenProvider;
import org.keycloak.models.SingleUseObjectProvider;

public final class InfinispanRevokedTokenProvider implements RevokedTokenProvider {

    private final SingleUseObjectProvider delegate;

    public InfinispanRevokedTokenProvider(SingleUseObjectProvider delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public boolean put(String id, long lifespanSeconds) {
        return delegate.putIfAbsent(id + SingleUseObjectProvider.REVOKED_KEY, lifespanSeconds);
    }

    @Override
    public boolean contains(String id) {
        return delegate.contains(id + SingleUseObjectProvider.REVOKED_KEY);
    }

    @Override
    public void close() {
        //no-op
    }
}
