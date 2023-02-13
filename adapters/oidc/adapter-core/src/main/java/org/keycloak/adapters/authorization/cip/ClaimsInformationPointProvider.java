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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.adapters.authorization.ClaimInformationPointProvider;
import org.keycloak.adapters.authorization.util.PlaceHolders;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ClaimsInformationPointProvider implements ClaimInformationPointProvider {

    private final Map<String, Object> config;

    public ClaimsInformationPointProvider(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public Map<String, List<String>> resolve(HttpFacade httpFacade) {
        Map<String, List<String>> claims = new HashMap<>();

        for (Entry<String, Object> configEntry : config.entrySet()) {
            String claimName = configEntry.getKey();
            Object claimValue = configEntry.getValue();
            List<String> values = new ArrayList<>();

            if (claimValue instanceof String) {
                values = getValues(claimValue.toString(), httpFacade);
            } else if (claimValue instanceof Collection) {

                for (Object value : Collection.class.cast(claimValue)) {
                    List<String> resolvedValues = getValues(value.toString(), httpFacade);

                    if (!resolvedValues.isEmpty()) {
                        values.addAll(resolvedValues);
                    }
                }
            }

            if (!values.isEmpty()) {
                claims.put(claimName, values);
            }
        }

        return claims;
    }

    private List<String> getValues(String value, HttpFacade httpFacade) {
        return PlaceHolders.resolve(value, httpFacade);
    }
}
