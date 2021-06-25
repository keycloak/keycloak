/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.admin.userprofile;

import static org.junit.Assert.assertEquals;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.REALM_USER_PROFILE_ENABLED;
import static org.keycloak.userprofile.config.UPConfigUtils.readDefaultConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.junit.Test;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class UserProfileAdminTest extends AbstractAdminTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getAttributes() == null) {
            testRealm.setAttributes(new HashMap<>());
        }
        testRealm.getAttributes().put(REALM_USER_PROFILE_ENABLED, Boolean.TRUE.toString());
    }

    @Test
    public void testDefaultConfigIfNoneSet() {
        assertEquals(readDefaultConfig(), testRealm().users().userProfile().getConfiguration());
    }

    @Test
    public void testSetDefaultConfig() throws IOException {
        String rawConfig = "{\"attributes\": [{\"name\": \"test\"}]}";
        UserProfileResource userProfile = testRealm().users().userProfile();

        userProfile.update(rawConfig);

        assertEquals(rawConfig, userProfile.getConfiguration());
    }
}
