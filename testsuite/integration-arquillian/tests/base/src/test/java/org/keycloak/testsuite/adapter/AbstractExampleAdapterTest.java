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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractExampleAdapterTest extends AbstractAdapterTest {

    public static final String EXAMPLES_HOME;
    public static final String EXAMPLES_VERSION_SUFFIX;
    public static final String EXAMPLES_HOME_DIR;
    public static final String TEST_APPS_HOME_DIR;
    public static final String EXAMPLES_WEB_XML;

    static {
        EXAMPLES_HOME = System.getProperty("examples.home", null);
        Assert.assertNotNull("Property ${examples.home} must bet set.", EXAMPLES_HOME);
        System.out.println(EXAMPLES_HOME);

        EXAMPLES_VERSION_SUFFIX = System.getProperty("examples.version.suffix", null);
        Assert.assertNotNull("Property ${examples.version.suffix} must bet set.", EXAMPLES_VERSION_SUFFIX);
        System.out.println(EXAMPLES_VERSION_SUFFIX);

        EXAMPLES_HOME_DIR = EXAMPLES_HOME + "/example-realms";
        TEST_APPS_HOME_DIR = EXAMPLES_HOME + "/test-apps-dist";
        EXAMPLES_WEB_XML = EXAMPLES_HOME + "/web.xml";
    }

    protected static WebArchive exampleDeployment(String name) {
        return exampleDeployment(name, webArchive -> {});
    }

    protected static WebArchive exampleDeployment(String name, Consumer<WebArchive> additionalResources) {
        WebArchive webArchive = ShrinkWrap.create(ZipImporter.class, name + ".war")
                .importFrom(new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .as(WebArchive.class)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
        addSameSiteUndertowHandlers(webArchive);

        additionalResources.accept(webArchive);

        modifyOIDCAdapterConfig(webArchive);

        return webArchive;
    }

    protected static void modifyOIDCAdapterConfig(WebArchive webArchive) {
        if (webArchive.contains(DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH)) {
            DeploymentArchiveProcessorUtils.modifyOIDCAdapterConfig(webArchive, DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH);
        }

        if (webArchive.contains(DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH_JS)) {
            DeploymentArchiveProcessorUtils.modifyOIDCAdapterConfig(webArchive, DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH_JS);
        }
    }

    protected static WebArchive exampleDeployment(String name, String contextPath) throws IOException {
        return exampleDeployment(name, contextPath, webArchive -> {});
    }

    protected static WebArchive exampleDeployment(String name, String contextPath, Consumer<WebArchive> additionalResources) throws IOException {
        URL webXML = Paths.get(EXAMPLES_WEB_XML).toUri().toURL();
        String webXmlContent = IOUtils.toString(webXML.openStream(), "UTF-8")
                .replace("%CONTEXT_PATH%", contextPath);
        WebArchive webArchive = ShrinkWrap.create(ZipImporter.class, name + ".war")
                .importFrom(new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .as(WebArchive.class)
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML)
                .add(new StringAsset(webXmlContent), "/WEB-INF/web.xml");
        addSameSiteUndertowHandlers(webArchive);

        additionalResources.accept(webArchive);

        modifyOIDCAdapterConfig(webArchive);

        return webArchive;
    }

}