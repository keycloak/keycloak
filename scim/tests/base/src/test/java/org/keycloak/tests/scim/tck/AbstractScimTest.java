/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.tests.scim.tck;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.userprofile.config.UPConfigUtils;

import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA_ATTRIBUTE;
import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;

public abstract class AbstractScimTest {

    @InjectRealm(config = ScimRealmConfig.class)
    ManagedRealm realm;

    @InjectScimClient
    ScimClient client;

    @InjectAdminEvents
    AdminEvents adminEvents;

    protected void addEnterpriseUserUserProfileAttributes() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        UPConfig originalConfig = configuration.clone();
        realm.cleanup().add(realm -> realm.users().userProfile().update(originalConfig));

        configuration.addOrReplaceAttribute(new UPAttribute("department", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":department")));
        configuration.addOrReplaceAttribute(new UPAttribute("division", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":division")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":costCenter")));
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("organization", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":organization")));
        configuration.addOrReplaceAttribute(new UPAttribute("manager", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":manager.value")));
        configuration.addOrReplaceAttribute(new UPAttribute("managerName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ":manager.displayName")));
        realm.admin().users().userProfile().update(configuration);
    }

    protected UPAttribute addOrReplaceUPAttribute(String name) {
        return addOrReplaceUPAttribute(null, name);
    }

    protected UPAttribute addOrReplaceUPAttribute(String customSchema, String name) {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        UPAttribute upAttribute = new UPAttribute("scim." + name, Map.of(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, Optional.ofNullable(customSchema).map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s + ":";
            }
        }).orElse("") + name));
        upAttribute.setPermissions(new UPAttributePermissions(Set.of(UPConfigUtils.ROLE_ADMIN), Set.of(UPConfigUtils.ROLE_ADMIN)));
        upConfig.addOrReplaceAttribute(upAttribute);
        realm.admin().users().userProfile().update(upConfig);
        adminEvents.clear();
        return upAttribute;
    }
}
