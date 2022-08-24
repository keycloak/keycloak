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

package org.keycloak.testsuite.adapter;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.ServerURLs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.APP_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.CURRENT_APP_SERVER;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.enableHTTPSForAppServer;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * <code>@AppServerContainer</code> is needed for stopping recursion in 
 * AppServerTestEnricher.getNearestSuperclassWithAnnotation
 * 
 * @author tkyjovsk
 */
@AppServerContainer("")
@AuthServerContainerExclude(AuthServer.REMOTE)
public abstract class AbstractAdapterTest extends AbstractAuthTest {

    @Page
    protected AppServerContextRoot appServerContextRootPage;

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";
    public static final URL jbossDeploymentStructure = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);
    public static final String UNDERTOW_HANDLERS_CONF = "undertow-handlers.conf";
    public static final URL undertowHandlersConf = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/samesite/undertow-handlers.conf");
    public static final String TOMCAT_CONTEXT_XML = "context.xml";
    public static final URL tomcatContext = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + TOMCAT_CONTEXT_XML);

    protected static boolean sslConfigured = false;

    @Before
    public void setUpAppServer() throws Exception {
        if (!sslConfigured && shouldConfigureSSL()) { // Other containers need some external configuraiton to run SSL tests
            enableHTTPSForAppServer();

            sslConfigured = true;
        }
    }

    @AfterClass
    public static void resetSSLConfig() {
        sslConfigured = false;
    }

    protected boolean shouldConfigureSSL() {
        return APP_SERVER_SSL_REQUIRED && (CURRENT_APP_SERVER.contains("eap") || CURRENT_APP_SERVER.contains("wildfly"));
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        addAdapterTestRealms(testRealms);
        for (RealmRepresentation tr : testRealms) {
            log.info("Setting redirect-uris in test realm '" + tr.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");

            modifyClientRedirectUris(tr, "http://localhost:8080", "");
            modifyClientRedirectUris(tr, "^((?:/.*|)/\\*)",
                  ServerURLs.getAppServerContextRoot() + "$1",
                    ServerURLs.getAuthServerContextRoot() + "$1");

            modifyClientWebOrigins(tr, "http://localhost:8080", ServerURLs.getAppServerContextRoot(),
                    ServerURLs.getAuthServerContextRoot());

            modifyClientUrls(tr, "http://localhost:8080", "");
            modifySamlMasterURLs(tr, "http://localhost:8080", "");
            modifySAMLClientsAttributes(tr, "http://localhost:8080", "");

            if (isRelative()) {
                modifyClientUrls(tr, ServerURLs.getAppServerContextRoot().toString(), "");
                modifySamlMasterURLs(tr, "/", ServerURLs.getAppServerContextRoot() + "/");
                modifySAMLClientsAttributes(tr, "8080", AUTH_SERVER_PORT);
            } else {
                modifyClientUrls(tr, "^(/.*)", ServerURLs.getAppServerContextRoot() + "$1");
                modifySamlMasterURLs(tr, "^(/.*)", ServerURLs.getAppServerContextRoot() + "$1");
                modifySAMLClientsAttributes(tr, "^(/.*)",  ServerURLs.getAppServerContextRoot() + "$1");
                modifyClientJWKSUrl(tr, "^(/.*)", ServerURLs.getAppServerContextRoot() + "$1");
            }
            if (AUTH_SERVER_SSL_REQUIRED) {
                tr.setSslRequired("all");
            }
        }
    }

    // TODO: Fix to not require re-import
    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    private void modifyClientJWKSUrl(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            realm.getClients().stream().
                    filter(client -> "client-jwt".equals(client.getClientAuthenticatorType()) && client.getAttributes().containsKey("jwks.url")).
                    forEach(client -> {
                Map<String, String> attr = client.getAttributes();
                attr.put("jwks.url", attr.get("jwks.url").replaceFirst(regex, replacement));
                client.setAttributes(attr);
            });
        }
    }

    public abstract void addAdapterTestRealms(List<RealmRepresentation> testRealms);

    public boolean isRelative() {
        return testContext.isRelativeAdapterTest();
    }

    protected void modifyClientRedirectUris(RealmRepresentation realm, String regex, String... replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                List<String> redirectUris = client.getRedirectUris();
                if (redirectUris != null) {
                    List<String> newRedirectUris = new ArrayList<>();
                    for (String uri : redirectUris) {
                        for (String uriReplacement : replacement) {
                            newRedirectUris.add(uri.replaceAll(regex, uriReplacement));
                        }

                    }
                    client.setRedirectUris(newRedirectUris);
                }
            }
        }
    }

    protected void modifyClientUrls(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                String baseUrl = client.getBaseUrl();
                if (baseUrl != null) {
                    client.setBaseUrl(baseUrl.replaceAll(regex, replacement));
                }
                String adminUrl = client.getAdminUrl();
                if (adminUrl != null) {
                    client.setAdminUrl(adminUrl.replaceAll(regex, replacement));
                }
            }
        }
    }

    protected void modifyClientWebOrigins(RealmRepresentation realm, String regex, String... replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                List<String> webOrigins = client.getWebOrigins();
                if (webOrigins != null) {
                    List<String> newWebOrigins = new ArrayList<>();
                    for (String uri : webOrigins) {
                        for (String originReplacement : replacement) {
                            newWebOrigins.add(uri.replaceAll(regex, originReplacement));
                        }
                    }
                    client.setWebOrigins(newWebOrigins);
                }
            }
        }
    }

    protected void modifySAMLClientsAttributes(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                if (client.getProtocol() != null && client.getProtocol().equals("saml")) {
                    log.debug("Modifying attributes of SAML client: " + client.getClientId());
                    for (Map.Entry<String, String> entry : client.getAttributes().entrySet()) {
                        client.getAttributes().put(entry.getKey(), entry.getValue().replaceAll(regex, replacement));
                    }
                }
            }
        }
    }

    protected void modifySamlMasterURLs(RealmRepresentation realm, String regex, String replacement) {
        if (realm.getClients() != null) {
            for (ClientRepresentation client : realm.getClients()) {
                if (client.getProtocol() != null && client.getProtocol().equals("saml")) {
                    log.debug("Modifying master URL of SAML client: " + client.getClientId());
                    String masterUrl = client.getAdminUrl();
                    if (masterUrl == null) {
                        masterUrl = client.getBaseUrl();
                    }
                    masterUrl = masterUrl.replaceFirst(regex, replacement);
                    client.setAdminUrl(masterUrl + ((!masterUrl.endsWith("/saml")) ? "/saml" : ""));
                }
            }
        }
    }

    /**
     * Modifies baseUrl, adminUrl and redirectUris for client based on real
     * deployment url of the app.
     *
     * @param realm
     * @param clientId
     * @param deploymentUrl
     */
    protected void fixClientUrisUsingDeploymentUrl(RealmRepresentation realm, String clientId, String deploymentUrl) {
        for (ClientRepresentation client : realm.getClients()) {
            if (clientId.equals(client.getClientId())) {
                if (client.getBaseUrl() != null) {
                    client.setBaseUrl(deploymentUrl);
                }
                if (client.getAdminUrl() != null) {
                    client.setAdminUrl(deploymentUrl);
                }
                List<String> redirectUris = client.getRedirectUris();
                if (redirectUris != null) {
                    List<String> newRedirectUris = new ArrayList<>();
                    for (String uri : redirectUris) {
                        newRedirectUris.add(deploymentUrl + "/*");
                    }
                    client.setRedirectUris(newRedirectUris);
                }
            }
        }
    }

    public static void addContextXml(Archive archive, String contextPath) {
        try {
            String contextXmlContent = IOUtils.toString(tomcatContext.openStream(), "UTF-8")
                    .replace("%CONTEXT_PATH%", contextPath);
            archive.add(new StringAsset(contextXmlContent), "/META-INF/context.xml");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void addSameSiteUndertowHandlers(WebArchive archive) {
        if (SuiteContext.BROWSER_STRICT_COOKIES) {
            archive.addAsWebInfResource(undertowHandlersConf, UNDERTOW_HANDLERS_CONF);
        }
    }
}
