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

package org.keycloak.wellknown;

import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface WellKnownProviderFactory extends ProviderFactory<WellKnownProvider> {

    /**
     * Alias, which will be used as URL suffix of this well-known provider. For example if you use alias like "openid-configuration", then your WellKnown provider
     * might be available under URL like "https://myhost/auth/realms/myrealm/.well-known/openid-configuration". If there are multiple provider factories with same alias,
     * the one with lowest priority will be used.
     *
     * @see #getPriority()
     *
     */
    default String getAlias() {
        return getId();
    }

    /**
     * Use low priority, so custom implementation with alias "openid-configuration" will win over the default implementation
     * with alias "openid-configuration", which is provided by Keycloak (OIDCWellKnownProviderFactory).
     *
     */
    default int getPriority() {
        return 1;
    }
}
