/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.authorization.cip;

import java.util.Map;

import org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.PolicyEnforcer;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpClaimInformationPointProviderFactory implements ClaimInformationPointProviderFactory<HttpClaimInformationPointProvider> {

    private PolicyEnforcer policyEnforcer;

    @Override
    public String getName() {
        return "http";
    }

    @Override
    public void init(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    @Override
    public HttpClaimInformationPointProvider create(Map<String, Object> config) {
        return new HttpClaimInformationPointProvider(config, policyEnforcer);
    }
}
