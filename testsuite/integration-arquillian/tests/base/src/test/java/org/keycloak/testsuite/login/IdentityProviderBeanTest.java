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

package org.keycloak.testsuite.login;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean.IdentityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link org.keycloak.forms.login.freemarker.model.IdentityProviderBean}
 */
public class IdentityProviderBeanTest extends AbstractTestRealmKeycloakTest {
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    @ModelTest
    public void testIdentityProviderAllVisible(KeycloakSession keycloakSession) throws URISyntaxException {
        RealmModel realm = keycloakSession.realms().getRealm("test");

        IdentityProviderModel o1 = buildIdpModel("alias1", "displayName1", "id1", "ur1", null, false);
        IdentityProviderModel o2 = buildIdpModel("alias2", "displayName2", "id2", "ur2", null, false);

        List<IdentityProviderModel> lstIdp = new ArrayList<>();
        lstIdp.add(o1);
        lstIdp.add(o2);

        IdentityProviderBean bean = new IdentityProviderBean(realm, keycloakSession, lstIdp, new URI("http://my.test.uri/"));

        Assert.assertEquals(2, bean.getProviders().size());
        Assert.assertEquals(2, bean.getVisibleProvidersCount());
        Assert.assertEquals(true, bean.isDisplayInfo());
        Assert.assertEquals(true, bean.isDisplaySocial());
    }

    @Test
    @ModelTest
    public void testIdentityProviderNoneVisible(KeycloakSession keycloakSession) throws URISyntaxException {
        RealmModel realm = keycloakSession.realms().getRealmByName("test");

        IdentityProviderModel o1 = buildIdpModel("alias1", "displayName1", "id1", "ur1", null, true);
        IdentityProviderModel o2 = buildIdpModel("alias2", "displayName2", "id2", "ur2", null, true);

        List<IdentityProviderModel> lstIdp = new ArrayList<>();
        lstIdp.add(o1);
        lstIdp.add(o2);

        IdentityProviderBean bean = new IdentityProviderBean(realm, keycloakSession, lstIdp, new URI("http://my.test.uri/"));

        Assert.assertEquals(2, bean.getProviders().size());
        Assert.assertEquals(0, bean.getVisibleProvidersCount());
        Assert.assertEquals(true, bean.isDisplayInfo());
        Assert.assertEquals(false, bean.isDisplaySocial());
    }

    @Test
    @ModelTest
    public void testIdentityProviderOnlyOneVisible(KeycloakSession keycloakSession) throws URISyntaxException {
        RealmModel realm = keycloakSession.realms().getRealmByName("test");

        IdentityProviderModel o1 = buildIdpModel("alias1", "displayName1", "id1", "ur1", null, false);
        IdentityProviderModel o2 = buildIdpModel("alias2", "displayName2", "id2", "ur2", null, true);

        List<IdentityProviderModel> lstIdp = new ArrayList<>();
        lstIdp.add(o1);
        lstIdp.add(o2);

        IdentityProviderBean bean = new IdentityProviderBean(realm, keycloakSession, lstIdp, new URI("http://my.test.uri/"));

        Assert.assertEquals(2, bean.getProviders().size());
        Assert.assertEquals(1, bean.getVisibleProvidersCount());
        Assert.assertEquals(true, bean.isDisplayInfo());
        Assert.assertEquals(true, bean.isDisplaySocial());
    }

    private static IdentityProviderModel buildIdpModel(String alias, String displayName, String providerId, String loginUrl, Integer guiOrder, boolean hideOnLoginPage)
    {
        IdentityProviderModel output = new IdentityProviderModel();
        output.setAlias(alias);
        output.setDisplayName(displayName);
        output.setProviderId(providerId);
        output.setEnabled(true);
        output.setLinkOnly(false);
        output.getConfig().put("singleSignOnServiceUrl", loginUrl);
        output.getConfig().put("guiOrder", String.valueOf(guiOrder));
        output.getConfig().put("hideOnLoginPage", String.valueOf(hideOnLoginPage));

        return output;
    }
}
