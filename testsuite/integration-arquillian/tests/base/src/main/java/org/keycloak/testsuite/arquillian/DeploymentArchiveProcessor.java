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

package org.keycloak.testsuite.arquillian;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;
import org.keycloak.testsuite.util.IOUtil;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.*;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.IOUtil.*;

;

/**
 * @author tkyjovsk
 */
public class DeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    public static final String REALM_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    protected final Logger log = org.jboss.logging.Logger.getLogger(this.getClass());

    private final boolean authServerSslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    public static final String WEBXML_PATH = "/WEB-INF/web.xml";
    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_JS = "/keycloak.json";
    public static final String SAML_ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak-saml.xml";

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        log.info("Processing archive " + archive.getName());
//        if (isAdapterTest(testClass)) {
        modifyAdapterConfigs(archive, testClass);
        modifyWebXml(archive, testClass);
//        } else {
//            log.info(testClass.getJavaClass().getSimpleName() + " is not an AdapterTest");
//        }
    }

    public static boolean isAdapterTest(TestClass testClass) {
        return hasAppServerContainerAnnotation(testClass.getJavaClass());
    }

    protected void modifyAdapterConfigs(Archive<?> archive, TestClass testClass) {
        boolean relative = isRelative(testClass.getJavaClass());
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_TENANT1, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_TENANT2, relative);
        modifyAdapterConfig(archive, ADAPTER_CONFIG_PATH_JS, relative);
        modifyAdapterConfig(archive, SAML_ADAPTER_CONFIG_PATH, relative);
    }

    protected void modifyAdapterConfig(Archive<?> archive, String adapterConfigPath, boolean relative) {
        if (archive.contains(adapterConfigPath)) {
            log.info("Modifying adapter config " + adapterConfigPath + " in " + archive.getName());
            if (adapterConfigPath.equals(SAML_ADAPTER_CONFIG_PATH)) { // SAML adapter config
                log.info("Modyfying saml adapter config in " + archive.getName());

                Document doc = loadXML(archive.get("WEB-INF/keycloak-saml.xml").getAsset().openStream());
                if (authServerSslRequired) {
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "http", "https");
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "8080", System.getProperty("auth.server.https.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "http", "https");
                } else {
                    modifyDocElementAttribute(doc, "SingleSignOnService", "bindingUrl", "8080", System.getProperty("auth.server.http.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "postBindingUrl", "8080", System.getProperty("auth.server.http.port"));
                    modifyDocElementAttribute(doc, "SingleLogoutService", "redirectBindingUrl", "8080", System.getProperty("auth.server.http.port"));
                }

                try {
                    archive.add(new StringAsset(IOUtil.documentToString(doc)), adapterConfigPath);
                } catch (TransformerException e) {
                    log.error("Can't transform document to String");
                    throw new RuntimeException(e);
                }
            } else { // OIDC adapter config
                try {
                    AdapterConfig adapterConfig = loadJson(archive.get(adapterConfigPath)
                            .getAsset().openStream(), AdapterConfig.class);

                    log.info(" setting " + (relative ? "" : "non-") + "relative auth-server-url");
                    if (relative) {
                        adapterConfig.setAuthServerUrl("/auth");
//                ac.setRealmKey(null); // TODO verify if realm key is required for relative scneario
                    } else {
                        adapterConfig.setAuthServerUrl(getAuthServerContextRoot() + "/auth");
                        adapterConfig.setRealmKey(REALM_KEY);
                    }

                    if ("true".equals(System.getProperty("app.server.ssl.required"))) {
                        adapterConfig.setSslRequired("all");
                    }

                    archive.add(new StringAsset(JsonSerialization.writeValueAsPrettyString(adapterConfig)),
                            adapterConfigPath);

                } catch (IOException ex) {
                    log.log(Level.FATAL, "Cannot serialize adapter config to JSON.", ex);
                }
            }
        }
    }

    DirectoryScanner scanner = new DirectoryScanner();

    protected List<File> getAdapterLibs(File adapterLibsLocation) {
        assert adapterLibsLocation.exists();
        List<File> libs = new ArrayList<>();
        scanner.setBasedir(adapterLibsLocation);
        scanner.setIncludes(new String[]{"**/*jar"});
        scanner.scan();
        for (String lib : scanner.getIncludedFiles()) {
            libs.add(new File(adapterLibsLocation, lib));
        }
        return libs;
    }

    protected void modifyWebXml(Archive<?> archive, TestClass testClass) {
        try {
            String webXmlContent = IOUtils.toString(
                    archive.get(WEBXML_PATH).getAsset().openStream());
            if (isTomcatAppServer(testClass.getJavaClass())) {
                webXmlContent = webXmlContent.replace("<auth-method>KEYCLOAK</auth-method>", "<auth-method>BASIC</auth-method>");
            }

            if (testClass.getJavaClass().isAnnotationPresent(UseServletFilter.class)) {
                //We need to add filter declaration to web.xml
                log.info("Adding filter to " + testClass.getAnnotation(UseServletFilter.class).filterClass() + " with mapping " + testClass.getAnnotation(UseServletFilter.class).filterPattern() + " for " + archive.getName());
                String filter = "\n<filter>\n" +
                        "<filter-name>" + testClass.getAnnotation(UseServletFilter.class).filterName() + "</filter-name>\n" +
                        "<filter-class>" + testClass.getAnnotation(UseServletFilter.class).filterClass() + "</filter-class>\n" +
                        "</filter>\n" +
                        "\n<filter-mapping>\n" +
                        "<filter-name>" + testClass.getAnnotation(UseServletFilter.class).filterName() + "</filter-name>\n" +
                        "<url-pattern>" + testClass.getAnnotation(UseServletFilter.class).filterPattern() + "</url-pattern>\n";
                if (!testClass.getAnnotation(UseServletFilter.class).dispatcherType().isEmpty()) {
                    filter += "<dispatcher>" + testClass.getAnnotation(UseServletFilter.class).dispatcherType() + "</dispatcher>\n";
                }
                filter += "</filter-mapping>\n";

                webXmlContent = webXmlContent.replace("</module-name>", "</module-name> " + filter);

                //Also we need to add all dependencies within war lib directory, because filter needs to work without installed adapter
                log.info("Adding SAMLFilter dependencies to " + archive.getName());
                ((WebArchive) archive).addAsLibraries(new SAMLFilterDependency().getDependencies());


                //finally we need to remove all keycloak related configuration from web.xml
                int start = webXmlContent.indexOf("<security-constraint>");
                int end = webXmlContent.indexOf("</security-role>") + "</security-role>".length();


                webXmlContent = webXmlContent.substring(0, start) + webXmlContent.substring(end);
            }

            archive.add(new StringAsset((webXmlContent)), WEBXML_PATH);
        } catch (IOException ex) {
            throw new RuntimeException("Cannot load web.xml from archive.");
        }
    }

}
