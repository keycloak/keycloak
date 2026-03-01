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

import java.util.HashSet;
import java.util.Set;

import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.config.UPConfigUtils;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

/**
 * Unit test for {@link UPConfigUtils}
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UPConfigUtilsTest {

    @Test
    public void canBeAuthFlowContext() {
        Assert.assertFalse(UserProfileContext.USER_API.canBeAuthFlowContext());

        Assert.assertTrue(UserProfileContext.ACCOUNT.canBeAuthFlowContext());
        Assert.assertTrue(UserProfileContext.IDP_REVIEW.canBeAuthFlowContext());
        Assert.assertTrue(UserProfileContext.REGISTRATION.canBeAuthFlowContext());
        Assert.assertTrue(UserProfileContext.UPDATE_PROFILE.canBeAuthFlowContext());
    }

    @Test
    public void isRoleForContext() {

        Assert.assertFalse(UserProfileContext.ACCOUNT.isRoleForContext( null));

        Set<String> roles = new HashSet<>();
        roles.add(ROLE_ADMIN);
        Assert.assertTrue(UserProfileContext.USER_API.isRoleForContext(roles));
        Assert.assertFalse(UserProfileContext.ACCOUNT.isRoleForContext(roles));
        Assert.assertFalse(UserProfileContext.UPDATE_PROFILE.isRoleForContext(roles));

        roles = new HashSet<>();
        roles.add(ROLE_USER);
        Assert.assertFalse(UserProfileContext.USER_API.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.ACCOUNT.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.IDP_REVIEW.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.REGISTRATION.isRoleForContext(roles));

        // both in roles
        roles.add(ROLE_ADMIN);
        Assert.assertTrue(UserProfileContext.USER_API.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.ACCOUNT.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.IDP_REVIEW.isRoleForContext(roles));
        Assert.assertTrue(UserProfileContext.REGISTRATION.isRoleForContext(roles));
    }

    @Test
    public void capitalizeFirstLetter() {
        Assert.assertNull(UPConfigUtils.capitalizeFirstLetter(null));
        Assert.assertEquals("",UPConfigUtils.capitalizeFirstLetter(""));
        Assert.assertEquals("A",UPConfigUtils.capitalizeFirstLetter("a"));
        Assert.assertEquals("AbcDefGh",UPConfigUtils.capitalizeFirstLetter("abcDefGh"));
    }

}
