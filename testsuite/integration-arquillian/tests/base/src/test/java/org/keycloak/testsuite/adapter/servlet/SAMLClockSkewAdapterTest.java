/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.adapter.servlet;

import java.util.List;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.filter.AdapterActionsFilter;
import org.keycloak.testsuite.adapter.page.SalesPostClockSkewServlet;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.utils.arquillian.ContainerConstants;
import org.keycloak.testsuite.utils.io.IOUtil;

import org.apache.http.util.EntityUtils;
import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

import static org.keycloak.testsuite.util.SamlClient.Binding.POST;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;


@AppServerContainer(ContainerConstants.APP_SERVER_WILDFLY)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP)
@AppServerContainer(ContainerConstants.APP_SERVER_EAP8)
public class SAMLClockSkewAdapterTest extends AbstractSAMLServletAdapterTest {

    @Page protected SalesPostClockSkewServlet salesPostClockSkewServletPage;
    private static final String DEPLOYMENT_NAME_3_SEC = SalesPostClockSkewServlet.DEPLOYMENT_NAME + "_3Sec";
    private static final String DEPLOYMENT_NAME_30_SEC = SalesPostClockSkewServlet.DEPLOYMENT_NAME + "_30Sec";

    @ArquillianResource private Deployer deployer;

    @Deployment(name = DEPLOYMENT_NAME_3_SEC, managed = false)
    protected static WebArchive salesPostClockSkewServlet3Sec() {
        return samlServletDeployment(SalesPostClockSkewServlet.DEPLOYMENT_NAME, DEPLOYMENT_NAME_3_SEC, SalesPostClockSkewServlet.DEPLOYMENT_NAME + "/WEB-INF/web.xml", 3, AdapterActionsFilter.class, SendUsernameServlet.class);
    }
    @Deployment(name = DEPLOYMENT_NAME_30_SEC, managed = false)
    protected static WebArchive salesPostClockSkewServlet30Sec() {
        return samlServletDeployment(SalesPostClockSkewServlet.DEPLOYMENT_NAME, DEPLOYMENT_NAME_30_SEC, SalesPostClockSkewServlet.DEPLOYMENT_NAME + "/WEB-INF/web.xml", 30, AdapterActionsFilter.class, SendUsernameServlet.class);
    }

    @Deployment(name = SalesPostClockSkewServlet.DEPLOYMENT_NAME, managed = false)
    protected static WebArchive salesPostClockSkewServlet5Sec() {
        return samlServletDeployment(SalesPostClockSkewServlet.DEPLOYMENT_NAME, SalesPostClockSkewServlet.DEPLOYMENT_NAME + "/WEB-INF/web.xml", 5, AdapterActionsFilter.class, SendUsernameServlet.class);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(IOUtil.loadRealm("/adapter-test/keycloak-saml/testsaml.json"));
    }

    private void assertOutcome(int timeOffset, Matcher matcher) throws Exception {
        try {
            String resultPage = new SamlClientBuilder()
                    .navigateTo(salesPostClockSkewServletPage.toString())
                    .processSamlResponse(POST).build()
                    .login().user(bburkeUser).build()
                    .processSamlResponse(POST)
                    .transformDocument(doc -> {
                        setAdapterAndServerTimeOffset(timeOffset, salesPostClockSkewServletPage.toString());
                        return doc;
                    }).build().executeAndTransform(resp -> EntityUtils.toString(resp.getEntity()));

            assertThat(resultPage, matcher);
        } finally {
            setAdapterAndServerTimeOffset(0, salesPostClockSkewServletPage.toString());
        }
    }

    private void assertTokenIsNotValid(int timeOffset) throws Exception {
        deployer.deploy(DEPLOYMENT_NAME_3_SEC);

        try {
            assertOutcome(timeOffset, allOf(
                not(containsString("request-path: principal=bburke")),
                containsString("SAMLRequest"),
                containsString("FORM METHOD=\"POST\"")
            ));
        } finally {
            deployer.undeploy(DEPLOYMENT_NAME_3_SEC);
        }
    }

    @Test
    public void testTokenHasExpired() throws Exception {
        assertTokenIsNotValid(65);
    }

    @Test
    public void testTokenIsNotYetValid() throws Exception {
        assertTokenIsNotValid(-65);
    }


    @Test
    public void testTokenTimeIsValid() throws Exception {
        deployer.deploy(DEPLOYMENT_NAME_30_SEC);

        try {
             assertOutcome(-10, allOf(containsString("request-path:"), containsString("principal=bburke")));
        } finally {
            deployer.undeploy(DEPLOYMENT_NAME_30_SEC);
        }
    }
}
