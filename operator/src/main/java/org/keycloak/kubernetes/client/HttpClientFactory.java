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

package org.keycloak.kubernetes.client;

import io.fabric8.kubernetes.client.okhttp.OkHttpClientFactory;

/**
 * FIXME: This Factory is meant to force Fabric8 to use OKHttp Client. We should not normally do that as it is not aligned with Quarkus.
 * Remove once the following are resolved:
 * https://github.com/fabric8io/kubernetes-client/issues/5036
 * https://github.com/fabric8io/kubernetes-client/issues/5033 (only needed by tests)
 *
 * Tracked by: https://github.com/keycloak/keycloak/issues/19573
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class HttpClientFactory extends OkHttpClientFactory {
    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public int priority() {
        return 10000;
    }
}
