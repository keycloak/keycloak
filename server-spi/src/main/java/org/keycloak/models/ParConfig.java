/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

public class ParConfig extends AbstractConfig {

    // realm attribute names
    public static final String PAR_REQUEST_URI_LIFESPAN = "parRequestUriLifespan";

    // default value
    public static final int DEFAULT_PAR_REQUEST_URI_LIFESPAN = 60; // sec

    private int requestUriLifespan;

    // client attribute names
    public static final String REQUIRE_PUSHED_AUTHORIZATION_REQUESTS = "require.pushed.authorization.requests";

    public ParConfig(RealmModel realm) {
        this.requestUriLifespan = realm.getAttribute(PAR_REQUEST_URI_LIFESPAN, DEFAULT_PAR_REQUEST_URI_LIFESPAN);

        this.realmForWrite = () -> realm;
    }

    public int getRequestUriLifespan() {
        return requestUriLifespan;
    }

    public void setRequestUriLifespan(String requestUriLifespan) {
        if (requestUriLifespan == null) {
            setRequestUriLifespan((Integer) null);
        } else {
            setRequestUriLifespan(Integer.parseInt(requestUriLifespan));
        }
    }

    public void setRequestUriLifespan(Integer requestUriLifespan) {
        if (requestUriLifespan == null) {
            requestUriLifespan = DEFAULT_PAR_REQUEST_URI_LIFESPAN;
        }
        this.requestUriLifespan = requestUriLifespan;
        persistRealmAttribute(PAR_REQUEST_URI_LIFESPAN, requestUriLifespan);
    }

    public boolean isRequirePushedAuthorizationRequests(ClientModel client) {
        String enabled = client.getAttribute(REQUIRE_PUSHED_AUTHORIZATION_REQUESTS);
        return Boolean.parseBoolean(enabled);
    }
}
