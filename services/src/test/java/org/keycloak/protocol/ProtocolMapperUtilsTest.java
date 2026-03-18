/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexander Schwartz
 */
public class ProtocolMapperUtilsTest {

    @Test
    public void getUserModelValue() {
        UserModel useModel = new UserModelDelegate(null) {
            @Override
            public String getUsername() {
                return "theo";
            }

            @Override
            public boolean isEmailVerified() {
                return true;
            }
        };

        Assert.assertEquals("theo", ProtocolMapperUtils.getUserModelValue(useModel, "username"));
        Assert.assertEquals("theo", ProtocolMapperUtils.getUserModelValue(useModel, "Username"));
        Assert.assertEquals("true", ProtocolMapperUtils.getUserModelValue(useModel, "emailVerified"));
        Assert.assertNull(ProtocolMapperUtils.getUserModelValue(useModel, "emailverified"));
        Assert.assertNull(ProtocolMapperUtils.getUserModelValue(useModel, "unknown"));
    }
}
