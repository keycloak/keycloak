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

import org.apache.tools.ant.DirectoryScanner;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.testsuite.arquillian.annotation.UseServletFilter;
import org.keycloak.testsuite.util.IOUtil;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.hasAppServerContainerAnnotation;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.isRelative;
import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.isTomcatAppServer;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.IOUtil.*;


/**
 * @author tkyjovsk
 */
public class DeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    protected final Logger log = org.jboss.logging.Logger.getLogger(this.getClass());

    private final boolean authServerSslRequired = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    public static final String WEBXML_PATH = "/WEB-INF/web.xml";
    public static final String ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT1 = "/WEB-INF/classes/tenant1-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_TENANT2 = "/WEB-INF/classes/tenant2-keycloak.json";
    public static final String ADAPTER_CONFIG_PATH_JS = "/keycloak.json";
    public static final String SAML_ADAPTER_CONFIG_PATH = "/WEB-INF/keycloak-saml.xml";
    public static final String JBOSS_DEPLOYMENT_XML_PATH = "/WEB-INF/jboss-deployment-structure.xml";

    @Inject
    @ClassScoped
    private InstanceProducer<TestContext> testContextProducer;

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        // Ignore run on server classes
        if (archive.getName().equals("run-on-server-classes.war")) {
            return;
        }

        log.info("Processing archive " + archive.getName());
//        if (isAdapterTest(testClass)) {
        modifyAdapterConfigs(archive, testClass);
        if (archive.contains(WEBXML_PATH)) {
            modifyWebXml(archive, testClass);
        }
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
                log.info("Modifying saml adapter config in " + archive.getName());

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

                archive.add(new StringAsset(IOUtil.documentToString(doc)), adapterConfigPath);


                // For running SAML tests it is necessary to have few dependencies on app-server side.
                // Few of them are not in adapter zip so we need to add them manually here
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

    public void addFilterDependencies(Archive<?> archive, TestClass testClass) {
        log.info("Adding filter dependencies to " + archive.getName());

        TestContext testContext = testContextProducer.get();
        if (testContext.getAppServerInfo().isUndertow()) {
            return;
        }

        String dependency = testClass.getAnnotation(UseServletFilter.class).filterDependency();
        ((WebArchive) archive).addAsLibraries(KeycloakDependenciesResolver.resolveDependencies((dependency + ":" + System.getProperty("project.version"))));

        Document jbossXmlDoc = loadXML(archive.get(JBOSS_DEPLOYMENT_XML_PATH).getAsset().openStream());
        removeNodeByAttributeValue(jbossXmlDoc, "dependencies", "module", "name", "org.keycloak.keycloak-saml-core");
        removeNodeByAttributeValue(jbossXmlDoc, "dependencies", "module", "name", "org.keycloak.keycloak-adapter-spi");
        archive.add(new StringAsset((documentToString(jbossXmlDoc))), JBOSS_DEPLOYMENT_XML_PATH);

    }

    protected void modifyWebXml(Archive<?> archive, TestClass testClass) {
        Document webXmlDoc = loadXML(
                archive.get(WEBXML_PATH).getAsset().openStream());
        if (isTomcatAppServer(testClass.getJavaClass())) {
            modifyDocElementValue(webXmlDoc, "auth-method", "KEYCLOAK", "BASIC");
        }

        if (testClass.getJavaClass().isAnnotationPresent(UseServletFilter.class)) {

            addFilterDependencies(archive, testClass);

            //We need to add filter declaration to web.xml
            log.info("Adding filter to " + testClass.getAnnotation(UseServletFilter.class).filterClass() + " with mapping " + testClass.getAnnotation(UseServletFilter.class).filterPattern() + " for " + archive.getName());

            Element filter = webXmlDoc.createElement("filter");
            Element filterName = webXmlDoc.createElement("filter-name");
            Element filterClass = webXmlDoc.createElement("filter-class");

            filterName.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterName());
            filterClass.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterClass());

            filter.appendChild(filterName);
            filter.appendChild(filterClass);
            appendChildInDocument(webXmlDoc, "web-app", filter);

            filter.appendChild(filterName);
            filter.appendChild(filterClass);

            // Limitation that all deployments of annotated class use same skipPattern. Refactor if something more flexible is needed (would require more tricky web.xml parsing though...)
            String skipPattern = testClass.getAnnotation(UseServletFilter.class).skipPattern();
            if (skipPattern != null && !skipPattern.isEmpty()) {
                Element initParam = webXmlDoc.createElement("init-param");

                Element paramName = webXmlDoc.createElement("param-name");
                paramName.setTextContent(KeycloakOIDCFilter.SKIP_PATTERN_PARAM);

                Element paramValue = webXmlDoc.createElement("param-value");
                paramValue.setTextContent(skipPattern);

                initParam.appendChild(paramName);
                initParam.appendChild(paramValue);

                filter.appendChild(initParam);
            }

            appendChildInDocument(webXmlDoc, "web-app", filter);

            Element filterMapping = webXmlDoc.createElement("filter-mapping");


            Element urlPattern = webXmlDoc.createElement("url-pattern");

            filterName = webXmlDoc.createElement("filter-name");

            filterName.setTextContent(testClass.getAnnotation(UseServletFilter.class).filterName());
            urlPattern.setTextContent(getElementTextContent(webXmlDoc, "web-app/security-constraint/web-resource-collection/url-pattern"));

            filterMapping.appendChild(filterName);
            filterMapping.appendChild(urlPattern);

            if (!testClass.getAnnotation(UseServletFilter.class).dispatcherType().isEmpty()) {
                Element dispatcher = webXmlDoc.createElement("dispatcher");
                dispatcher.setTextContent(testClass.getAnnotation(UseServletFilter.class).dispatcherType());
                filterMapping.appendChild(dispatcher);
            }
            appendChildInDocument(webXmlDoc, "web-app", filterMapping);

            //finally we need to remove all keycloak related configuration from web.xml
            removeElementsFromDoc(webXmlDoc, "web-app", "security-constraint");
            removeElementsFromDoc(webXmlDoc, "web-app", "login-config");
            removeElementsFromDoc(webXmlDoc, "web-app", "security-role");


        }


        archive.add(new StringAsset((documentToString(webXmlDoc))), WEBXML_PATH);
    }

}
