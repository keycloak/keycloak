/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

/**
 * A SCIM realm where the SCIM client that creates a group owns it.
 */
public class GroupOwnershipRealmConfig implements RealmConfig {

    static final String REALM_ATTRIBUTE = "scimGroupMembershipClaims";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.scimEnabled(true).attribute(REALM_ATTRIBUTE, "true");
    }
}
