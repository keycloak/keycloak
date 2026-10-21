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
package org.keycloak.authorization.client;


import org.keycloak.AuthorizationContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClientAuthorizationContext extends AuthorizationContext {

    private final AuthzClient client;

    public ClientAuthorizationContext(AccessToken authzToken, PolicyEnforcerConfig.PathConfig current, AuthzClient client) {
        super(authzToken, current);
        this.client = client;
    }

    public ClientAuthorizationContext(AuthzClient client) {
        this.client = client;
    }

    public AuthzClient getClient() {
        return client;
    }
}
