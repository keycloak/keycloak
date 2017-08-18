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

package org.keycloak.storage.ldap.idm.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPMappersComparator;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPMappersComparatorTest {



    @Test
    public void testCompareWithCNUsername() {
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>();
        cfg.add(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, LDAPConstants.CN);
        LDAPConfig config = new LDAPConfig(cfg);

        List<ComponentModel> sorted = LDAPMappersComparator.sortAsc(config, getMappers());
        assertOrder(sorted, "username-cn", "sAMAccountName", "first name", "full name");

        sorted = LDAPMappersComparator.sortDesc(config, getMappers());
        assertOrder(sorted, "full name", "first name", "sAMAccountName", "username-cn");
    }

    @Test
    public void testCompareWithSAMAccountNameUsername() {
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>();
        cfg.add(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, LDAPConstants.SAM_ACCOUNT_NAME);
        LDAPConfig config = new LDAPConfig(cfg);

        List<ComponentModel> sorted = LDAPMappersComparator.sortAsc(config, getMappers());
        assertOrder(sorted, "sAMAccountName", "username-cn", "first name", "full name");

        sorted = LDAPMappersComparator.sortDesc(config, getMappers());
        assertOrder(sorted, "full name", "first name", "username-cn", "sAMAccountName");
    }

    private void assertOrder(List<ComponentModel> result, String... names) {
        Assert.assertEquals(result.size(), names.length);
        for (int i=0 ; i<names.length ; i++) {
            Assert.assertEquals(names[i], result.get(i).getName());
        }
    }

    private List<ComponentModel> getMappers() {
        List<ComponentModel> result = new LinkedList<>();

        ComponentModel mapperModel = KeycloakModelUtils.createComponentModel("first name",  "fed-provider", UserAttributeLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                UserAttributeLDAPStorageMapper.READ_ONLY, "true",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd1");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createComponentModel("username-cn", "fed-provider", UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                UserAttributeLDAPStorageMapper.READ_ONLY, "true",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd2");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createComponentModel("full name", "fed-provider", FullNameLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                UserAttributeLDAPStorageMapper.READ_ONLY, "true");
        mapperModel.setId("idd3");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createComponentModel("sAMAccountName", "fed-provider", UserAttributeLDAPStorageMapperFactory.PROVIDER_ID,LDAPStorageMapper.class.getName(),
                UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, LDAPConstants.SAM_ACCOUNT_NAME,
                UserAttributeLDAPStorageMapper.READ_ONLY, "false",
                UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd4");
        result.add(mapperModel);

        return result;
    }
}
