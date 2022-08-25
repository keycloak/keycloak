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
package org.keycloak.testsuite.user.profile.config;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.config.UPConfigUtils;

/**
 * Unit test for {@link UPConfigUtils}
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPConfigUtilsTest {

    @Test
    public void canBeAuthFlowContext() {
        Assert.assertFalse(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.ACCOUNT));
        Assert.assertFalse(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.ACCOUNT_OLD));
        Assert.assertFalse(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.USER_API));

        Assert.assertTrue(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.IDP_REVIEW));
        Assert.assertTrue(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.REGISTRATION_PROFILE));
        Assert.assertTrue(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.REGISTRATION_USER_CREATION));
        Assert.assertTrue(UPConfigUtils.canBeAuthFlowContext(UserProfileContext.UPDATE_PROFILE));
    }

    @Test
    public void isRoleForContext() {

        Assert.assertFalse(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT, null));

        Set<String> roles = new HashSet<>();
        roles.add(ROLE_ADMIN);
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.USER_API, roles));
        Assert.assertFalse(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT, roles));
        Assert.assertFalse(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT_OLD, roles));
        Assert.assertFalse(UPConfigUtils.isRoleForContext(UserProfileContext.UPDATE_PROFILE, roles));

        roles = new HashSet<>();
        roles.add(ROLE_USER);
        Assert.assertFalse(UPConfigUtils.isRoleForContext(UserProfileContext.USER_API, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT_OLD, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.IDP_REVIEW, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.REGISTRATION_PROFILE, roles));

        // both in roles
        roles.add(ROLE_ADMIN);
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.USER_API, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.ACCOUNT_OLD, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.IDP_REVIEW, roles));
        Assert.assertTrue(UPConfigUtils.isRoleForContext(UserProfileContext.REGISTRATION_PROFILE, roles));
    }

    @Test
    public void breakString() {
        List<String> ret = UPConfigUtils.getChunks(null, 2);
        Assert.assertEquals(0, ret.size());

        ret = UPConfigUtils.getChunks("", 2);
        assertListContent(ret, "");

        ret = UPConfigUtils.getChunks("1234567", 3);
        assertListContent(ret, "123", "456", "7");

        ret = UPConfigUtils.getChunks("12345678", 3);
        assertListContent(ret, "123", "456", "78");

        ret = UPConfigUtils.getChunks("123456789", 3);
        assertListContent(ret, "123", "456", "789");
    }

    /**
     * Assert list exactly contains all expected parts in given order
     */
    private void assertListContent(List<String> actual, String... expectedParts) {
        int i = 0;
        Assert.assertEquals(expectedParts.length, actual.size());
        for (String ep : expectedParts) {
            Assert.assertEquals(ep, actual.get(i++));
        }
    }

    @Test
    public void capitalizeFirstLetter() {
        Assert.assertNull(UPConfigUtils.capitalizeFirstLetter(null));
        Assert.assertEquals("",UPConfigUtils.capitalizeFirstLetter(""));
        Assert.assertEquals("A",UPConfigUtils.capitalizeFirstLetter("a"));
        Assert.assertEquals("AbcDefGh",UPConfigUtils.capitalizeFirstLetter("abcDefGh"));
    }

}
