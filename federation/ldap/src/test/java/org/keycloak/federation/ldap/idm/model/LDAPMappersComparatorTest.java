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

package org.keycloak.federation.ldap.idm.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.LDAPMappersComparator;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPMappersComparatorTest {



    @Test
    public void testCompareWithCNUsername() {
        Map<String, String> cfg = new HashMap<>();
        cfg.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, LDAPConstants.CN);
        LDAPConfig config = new LDAPConfig(cfg);

        List<UserFederationMapperModel> sorted = LDAPMappersComparator.sortAsc(config, getMappers());
        assertOrder(sorted, "username-cn", "sAMAccountName", "first name", "full name");

        sorted = LDAPMappersComparator.sortDesc(config, getMappers());
        assertOrder(sorted, "full name", "first name", "sAMAccountName", "username-cn");
    }

    @Test
    public void testCompareWithSAMAccountNameUsername() {
        Map<String, String> cfg = new HashMap<>();
        cfg.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, LDAPConstants.SAM_ACCOUNT_NAME);
        LDAPConfig config = new LDAPConfig(cfg);

        List<UserFederationMapperModel> sorted = LDAPMappersComparator.sortAsc(config, getMappers());
        assertOrder(sorted, "sAMAccountName", "username-cn", "first name", "full name");

        sorted = LDAPMappersComparator.sortDesc(config, getMappers());
        assertOrder(sorted, "full name", "first name", "username-cn", "sAMAccountName");
    }

    private void assertOrder(List<UserFederationMapperModel> result, String... names) {
        Assert.assertEquals(result.size(), names.length);
        for (int i=0 ; i<names.length ; i++) {
            Assert.assertEquals(names[i], result.get(i).getName());
        }
    }

    private Set<UserFederationMapperModel> getMappers() {
        Set<UserFederationMapperModel> result = new HashSet<>();

        UserFederationMapperModel mapperModel = KeycloakModelUtils.createUserFederationMapperModel("first name",  "fed-provider", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.FIRST_NAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.GIVENNAME,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd1");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("username-cn", "fed-provider", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.CN,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd2");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("full name", "fed-provider", FullNameLDAPFederationMapperFactory.PROVIDER_ID,
                FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                UserAttributeLDAPFederationMapper.READ_ONLY, "true");
        mapperModel.setId("idd3");
        result.add(mapperModel);

        mapperModel = KeycloakModelUtils.createUserFederationMapperModel("sAMAccountName", "fed-provider", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID,
                UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, UserModel.USERNAME,
                UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, LDAPConstants.SAM_ACCOUNT_NAME,
                UserAttributeLDAPFederationMapper.READ_ONLY, "false",
                UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false",
                UserAttributeLDAPFederationMapper.IS_MANDATORY_IN_LDAP, "true");
        mapperModel.setId("idd4");
        result.add(mapperModel);

        return result;
    }
}
