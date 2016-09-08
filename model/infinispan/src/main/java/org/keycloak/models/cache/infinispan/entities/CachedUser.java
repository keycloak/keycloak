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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.common.util.MultivaluedHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedUser extends AbstractRevisioned implements InRealm  {
    private String realm;
    private String username;
    private Long createdTimestamp;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private List<UserCredentialValueModel> credentials = new LinkedList<>();
    private boolean enabled;
    private boolean totp;
    private String federationLink;
    private String serviceAccountClientLink;
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private Set<String> requiredActions = new HashSet<>();
    private Set<String> roleMappings = new HashSet<>();
    private Set<String> groups = new HashSet<>();



    public CachedUser(Long revision, RealmModel realm, UserModel user) {
        super(revision, user.getId());
        this.realm = realm.getId();
        this.username = user.getUsername();
        this.createdTimestamp = user.getCreatedTimestamp();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.attributes.putAll(user.getAttributes());
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.credentials.addAll(user.getCredentialsDirectly());
        this.enabled = user.isEnabled();
        this.totp = user.isOtpEnabled();
        this.federationLink = user.getFederationLink();
        this.serviceAccountClientLink = user.getServiceAccountClientLink();
        this.requiredActions.addAll(user.getRequiredActions());
        for (RoleModel role : user.getRoleMappings()) {
            roleMappings.add(role.getId());
        }
        Set<GroupModel> groupMappings = user.getGroups();
        if (groupMappings != null) {
            for (GroupModel group : groupMappings) {
                groups.add(group.getId());
            }
        }
    }

    public String getRealm() {
        return realm;
    }

    public String getUsername() {
        return username;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public List<UserCredentialValueModel> getCredentials() {
        return credentials;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isTotp() {
        return totp;
    }

    public MultivaluedHashMap<String, String> getAttributes() {
        return attributes;
    }

    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    public Set<String> getRoleMappings() {
        return roleMappings;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public String getServiceAccountClientLink() {
        return serviceAccountClientLink;
    }

    public Set<String> getGroups() {
        return groups;
    }

}
