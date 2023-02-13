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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

public class HiddenProviderTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;
    
    @Override
    protected RealmResource testRealm() {
        return adminClient.realm("realm-with-broker");
    }
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {        
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                                              .providerId("oidc")
                                              .alias("visible-oidc")
                                              .displayName("VisibleOIDC")
                                              .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                                              .providerId("oidc")
                                              .alias("hidden-oidc")
                                              .displayName("HiddenOIDC")
                                              .hideOnLoginPage()
                                              .build());
    }

    @Test
    public void testVisibleProviderButton() {
        loginPage.open();
        Assert.assertNotNull(loginPage.findSocialButton("visible-oidc"));
    }
    
    @Test(expected=org.openqa.selenium.NoSuchElementException.class)
    public void testHiddenProviderButton() {
        loginPage.open();
        Assert.assertNull(loginPage.findSocialButton("hidden-oidc"));
    } 
}
