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
import org.junit.BeforeClass;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.adapter.page.AppServerContextRoot;
import org.keycloak.testsuite.arquillian.AppServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.wildfly.extras.creaper.commands.undertow.AddUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.RemoveUndertowListener;
import org.wildfly.extras.creaper.commands.undertow.UndertowListenerType;
import org.wildfly.extras.creaper.commands.web.AddConnector;
import org.wildfly.extras.creaper.commands.web.AddConnectorSslConfig;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.AUTH_SERVER_SSL_REQUIRED;
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

    protected static final boolean APP_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("app.server.ssl.required", "false"));
    protected static final String APP_SERVER_CONTAINER = System.getProperty("app.server", "");

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";
    public static final URL jbossDeploymentStructure = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);
    public static final String TOMCAT_CONTEXT_XML = "context.xml";
    public static final URL tomcatContext = AbstractServletsAdapterTest.class
            .getResource("/adapter-test/" + TOMCAT_CONTEXT_XML);

    @BeforeClass
    public static void setUpAppServer() throws Exception {
        if (APP_SERVER_SSL_REQUIRED && (APP_SERVER_CONTAINER.contains("eap") || APP_SERVER_CONTAINER.contains("wildfly"))) { // Other containers need some external configuraiton to run SSL tests
            enableHTTPSForAppServer();
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        addAdapterTestRealms(testRealms);
        for (RealmRepresentation tr : testRealms) {
            log.info("Setting redirect-uris in test realm '" + tr.getRealm() + "' as " + (isRelative() ? "" : "non-") + "relative");

            modifyClientRedirectUris(tr, "http://localhost:8080", "");
            modifyClientRedirectUris(tr, "^((?:/.*|)/\\*)",
                  "http://localhost:" + System.getProperty("app.server.http.port", "8280") + "$1",
                  "http://localhost:" + System.getProperty("auth.server.http.port", "8180") + "$1",
                  "https://localhost:" + System.getProperty("app.server.https.port", "8643") + "$1",
                  "https://localhost:" + System.getProperty("auth.server.http.port", "8543") + "$1");

            modifyClientWebOrigins(tr, "http://localhost:8080",
                  "http://localhost:" + System.getProperty("app.server.http.port", "8280"),
                  "http://localhost:" + System.getProperty("auth.server.http.port", "8180"),
                  "https://localhost:" + System.getProperty("app.server.https.port", "8643"),
                  "https://localhost:" + System.getProperty("auth.server.http.port", "8543"));

            modifyClientUrls(tr, "http://localhost:8080", "");
            modifySamlMasterURLs(tr, "http://localhost:8080", "");
            modifySAMLClientsAttributes(tr, "http://localhost:8080", "");

            if (isRelative()) {
                modifyClientUrls(tr, appServerContextRootPage.toString(), "");
                modifySamlMasterURLs(tr, "/", "http://localhost:" + System.getProperty("auth.server.http.port", null) + "/");
                modifySAMLClientsAttributes(tr, "8080", System.getProperty("auth.server.http.port", "8180"));
            } else {
                modifyClientUrls(tr, "^(/.*)", appServerContextRootPage.toString() + "$1");
                modifySamlMasterURLs(tr, "^(/.*)", appServerContextRootPage.toString() + "$1");
                modifySAMLClientsAttributes(tr, "^(/.*)",  appServerContextRootPage.toString() + "$1");
                modifyClientJWKSUrl(tr, "^(/.*)", appServerContextRootPage.toString() + "$1");
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
                    log.info("Modifying attributes of SAML client: " + client.getClientId());
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
                    log.info("Modifying master URL of SAML client: " + client.getClientId());
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

    private static void enableHTTPSForAppServer() throws CommandFailedException, InterruptedException, TimeoutException, IOException, CliException, OperationException {
        try (OnlineManagementClient client = AppServerTestEnricher.getManagementClient()) {
            Administration administration = new Administration(client);
            Operations operations = new Operations(client);
            
            if(!operations.exists(Address.coreService("management").and("security-realm", "UndertowRealm"))) {
                client.execute("/core-service=management/security-realm=UndertowRealm:add()");
                client.execute("/core-service=management/security-realm=UndertowRealm/server-identity=ssl:add(keystore-relative-to=jboss.server.config.dir,keystore-password=secret,keystore-path=adapter.jks");
            }
            
            client.execute("/system-property=javax.net.ssl.trustStore:add(value=${jboss.server.config.dir}/keycloak.truststore)");
            client.execute("/system-property=javax.net.ssl.trustStorePassword:add(value=secret)");
            
            if (APP_SERVER_CONTAINER.contains("eap6")) {
                if(!operations.exists(Address.subsystem("web").and("connector", "https"))) {
                    client.apply(new AddConnector.Builder("https")
                            .protocol("HTTP/1.1")
                            .scheme("https")
                            .socketBinding("https")
                            .secure(true)
                            .build());
                    
                    client.apply(new AddConnectorSslConfig.Builder("https")
                            .password("secret")
                            .certificateKeyFile("${jboss.server.config.dir}/adapter.jks")
                            .build());
                }
            } else {
                client.apply(new RemoveUndertowListener.Builder(UndertowListenerType.HTTPS_LISTENER, "https")
                        .forDefaultServer());
                
                administration.reloadIfRequired();
                
                client.apply(new AddUndertowListener.HttpsBuilder("https", "default-server", "https")
                        .securityRealm("UndertowRealm")
                        .build());
            }

            administration.reloadIfRequired();
        }
    }

}
