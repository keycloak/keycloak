/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.adapter;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Retry;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.page.EmployeeServletDistributable;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.ServerURLs;

import org.apache.http.client.methods.HttpGet;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

import static org.keycloak.testsuite.admin.Users.setPasswordFor;
import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractSAMLAdapterClusteredTest extends AbstractAdapterClusteredTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/keycloak-saml/testsaml-behind-lb.json"));

        if (!"localhost".equals(ServerURLs.APP_SERVER_HOST)) {
            for (RealmRepresentation realm : testRealms) {
                Optional<ClientRepresentation> clientRepresentation = realm.getClients().stream()
                        .filter(c -> c.getClientId().equals("http://localhost:8580/employee-distributable/"))
                        .findFirst();

                clientRepresentation.ifPresent(cr -> {
                    cr.setBaseUrl(cr.getBaseUrl().replace("localhost", ServerURLs.APP_SERVER_HOST));
                    cr.setRedirectUris(cr.getRedirectUris()
                            .stream()
                            .map(url -> url.replace("localhost", ServerURLs.APP_SERVER_HOST))
                            .collect(Collectors.toList())
                    );
                    cr.setAttributes(cr.getAttributes().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> entry.getValue().replace("localhost", ServerURLs.APP_SERVER_HOST))
                            )
                    );

                });
            }
        }
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmSAMLPostLoginPage.setAuthRealm(DEMO);
        loginPage.setAuthRealm(DEMO);
        loginActionsPage.setAuthRealm(DEMO);
    }

    @Override
    protected void deploy() {
        deployer.deploy(EmployeeServletDistributable.DEPLOYMENT_NAME);
        deployer.deploy(EmployeeServletDistributable.DEPLOYMENT_NAME + "_2");
    }

    @Override
    protected void undeploy() {
        deployer.undeploy(EmployeeServletDistributable.DEPLOYMENT_NAME);
        deployer.undeploy(EmployeeServletDistributable.DEPLOYMENT_NAME + "_2");
    }

    private void testLogoutViaSessionIndex(URL employeeUrl, boolean forceRefreshAtOtherNode, BiConsumer<SamlClientBuilder, String> logoutFunction) {
        setPasswordFor(bburkeUser, CredentialRepresentation.PASSWORD);

        String employeeUrlString = getProxiedUrl(employeeUrl);
        SamlClientBuilder builder = new SamlClientBuilder()
          // Go to employee URL at reverse proxy which is set to forward to first node
          .navigateTo(employeeUrlString)

          // process redirection to login page
          .processSamlResponse(Binding.POST).build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST).build()

          // Returned to the page
          .assertResponse(Matchers.bodyHC(containsString("principal=bburke")))

          // Update the proxy to forward to the second node.
          .addStep(() -> updateProxy(NODE_2_NAME, NODE_2_URI, NODE_1_URI));

        if (forceRefreshAtOtherNode) {
            // Go to employee URL at reverse proxy which is set to forward to _second_ node now
            builder
              .navigateTo(employeeUrlString)
              .doNotFollowRedirects()
              .assertResponse(Matchers.bodyHC(containsString("principal=bburke")));
        }

        // Logout at the _second_ node
        logoutFunction.accept(builder, employeeUrlString);

        SamlClient samlClient = builder.execute();
        delayedCheckLoggedOut(samlClient, employeeUrlString);

        // Update the proxy to forward to the first node.
        updateProxy(NODE_1_NAME, NODE_1_URI, NODE_2_URI);
        delayedCheckLoggedOut(samlClient, employeeUrlString);
    }

    private void delayedCheckLoggedOut(SamlClient samlClient, String url) {
        Retry.execute(() -> {
          samlClient.execute(
            (client, currentURI, currentResponse, context) -> new HttpGet(url),
            (client, currentURI, currentResponse, context) -> {
              assertThat(currentResponse, Matchers.bodyHC(not(containsString("principal=bburke"))));
              return null;
            }
          );
        }, 10, 300);
    }

    private void logoutViaAdminConsole() {
        RealmResource demoRealm = adminClient.realm(DEMO);
        String bburkeId = ApiUtil.findUserByUsername(demoRealm, "bburke").getId();
        demoRealm.users().get(bburkeId).logout();
        log.infov("Logged out via admin console");
    }
    
    @Test
    public void testAdminInitiatedBackchannelLogout(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, false, (builder, url) -> builder.addStep(this::logoutViaAdminConsole));
    }

    @Test
    public void testAdminInitiatedBackchannelLogoutWithAssertionOfLoggedIn(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, true, (builder, url) -> builder.addStep(this::logoutViaAdminConsole));
    }

    @Test
    public void testUserInitiatedFrontchannelLogout(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, false, (builder, url) -> {
            builder
              .navigateTo(url + "?GLO=true")
              .processSamlResponse(Binding.POST).build()    // logout request
              .processSamlResponse(Binding.POST).build()    // logout response
            ;
        });
    }

    @Test
    public void testUserInitiatedFrontchannelLogoutWithAssertionOfLoggedIn(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        testLogoutViaSessionIndex(employeeUrl, true, (builder, url) -> {
            builder
              .navigateTo(url + "?GLO=true")
              .processSamlResponse(Binding.POST).build()    // logout request
              .processSamlResponse(Binding.POST).build()    // logout response
            ;
        });
    }

    @Test
    public void testNodeRestartResiliency(@ArquillianResource
      @OperateOnDeployment(value = EmployeeServletDistributable.DEPLOYMENT_NAME) URL employeeUrl) throws Exception {
        ContainerInfo containerInfo = testContext.getAppServerBackendsInfo().get(0);

        setPasswordFor(bburkeUser, CredentialRepresentation.PASSWORD);

        String employeeUrlString = getProxiedUrl(employeeUrl);
        SamlClient samlClient = new SamlClientBuilder()
          // Go to employee URL at reverse proxy which is set to forward to first node
          .navigateTo(employeeUrlString)

          // process redirection to login page
          .processSamlResponse(Binding.POST).build()
          .login().user(bburkeUser).build()
          .processSamlResponse(Binding.POST).build()

          // Returned to the page
          .assertResponse(Matchers.bodyHC(containsString("principal=bburke")))

          .execute();

        controller.stop(containerInfo.getQualifier());
        updateProxy(NODE_2_NAME, NODE_2_URI, NODE_1_URI);   // Update the proxy to forward to the second node.
        samlClient.execute(new SamlClientBuilder()
          .navigateTo(employeeUrlString)
          .doNotFollowRedirects()
          .assertResponse(Matchers.bodyHC(containsString("principal=bburke")))
          .getSteps());

        controller.start(containerInfo.getQualifier());
        updateProxy(NODE_1_NAME, NODE_1_URI, NODE_2_URI);   // Update the proxy to forward to the first node.
        samlClient.execute(new SamlClientBuilder()
          .navigateTo(employeeUrlString)
          .doNotFollowRedirects()
          .assertResponse(Matchers.bodyHC(containsString("principal=bburke")))
          .getSteps());
    }
}
