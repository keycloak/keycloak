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

package org.keycloak.partialimport;

/**
 * Enum for each resource type that can be partially imported.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public enum ResourceType {
    USER, GROUP, CLIENT, IDP, REALM_ROLE, CLIENT_ROLE;

    /**
     * Used to create the admin path in events.
     *
     * @return The resource portion of the path.
     */
    public String getPath() {
        switch(this) {
            case USER: return "users";
            case GROUP: return "groups";
            case CLIENT: return "clients";
            case IDP: return "identity-provider-settings";
            case REALM_ROLE: return "realms";
            case CLIENT_ROLE: return "clients";
            default: return "";
        }
    }

    @Override
    public String toString() {
        switch(this) {
            case USER: return "User";
            case GROUP: return "Group";
            case CLIENT: return "Client";
            case IDP: return "Identity Provider";
            case REALM_ROLE: return "Realm Role";
            case CLIENT_ROLE: return "Client Role";
            default: return super.toString();
        }
    }
}
