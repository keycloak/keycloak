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

package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RolesRepresentation {
    protected List<RoleRepresentation> realm;
    protected Map<String, List<RoleRepresentation>> client;
    @Deprecated
    protected Map<String, List<RoleRepresentation>> application;

    public List<RoleRepresentation> getRealm() {
        return realm;
    }

    public void setRealm(List<RoleRepresentation> realm) {
        this.realm = realm;
    }

    public Map<String, List<RoleRepresentation>> getClient() {
        return client;
    }

    public void setClient(Map<String, List<RoleRepresentation>> client) {
        this.client = client;
    }

    @Deprecated
    public Map<String, List<RoleRepresentation>> getApplication() {
        return application;
    }
}
