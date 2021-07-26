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
package org.keycloak.testsuite.broker;

import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

/**
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
@EnableFeature(value = Profile.Feature.DECLARATIVE_USER_PROFILE)
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class KcSamlFirstBrokerLoginWithUserProfileTest extends KcSamlFirstBrokerLoginTest {
    
    @Override
    @Before
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        enableDynamicUserProfile();
    }

}
