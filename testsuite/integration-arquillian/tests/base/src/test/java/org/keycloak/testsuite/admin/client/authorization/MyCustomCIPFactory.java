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
package org.keycloak.testsuite.admin.client.authorization;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.adapters.authorization.ClaimInformationPointProvider;
import org.keycloak.adapters.authorization.ClaimInformationPointProviderFactory;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.spi.HttpFacade;

public class MyCustomCIPFactory implements ClaimInformationPointProviderFactory<MyCustomCIP> {

    @Override
    public String getName() {
        return "my-custom-cip";
    }

    @Override
    public void init(PolicyEnforcer policyEnforcer) {

    }

    @Override
    public MyCustomCIP create(Map<String, Object> config) {
        return new MyCustomCIP(config);
    }
}

class MyCustomCIP implements ClaimInformationPointProvider {

    private final Map<String, Object> config;

    MyCustomCIP(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public Map<String, List<String>> resolve(HttpFacade httpFacade) {
        Map<String, List<String>> claims = new HashMap<>();

        claims.put("resolved-claim", Arrays.asList(config.get("claim-value").toString()));

        return claims;
    }
}
