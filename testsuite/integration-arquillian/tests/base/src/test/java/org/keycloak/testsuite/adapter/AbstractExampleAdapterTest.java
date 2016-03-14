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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

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

        if (!System.getProperty("unpacked.container.folder.name","").isEmpty()) {
            EXAMPLES_HOME_DIR = EXAMPLES_HOME + "/" + System.getProperty("unpacked.container.folder.name","") + "-examples";
            TEST_APPS_HOME_DIR = EXAMPLES_HOME + "/" + System.getProperty("unpacked.container.folder.name","") + "-test-apps";
        } else {
            EXAMPLES_HOME_DIR = EXAMPLES_HOME + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX;
            TEST_APPS_HOME_DIR = EXAMPLES_HOME + "/Keycloak-" + EXAMPLES_VERSION_SUFFIX + "-test-apps";
        }

        EXAMPLES_WEB_XML = EXAMPLES_HOME + "/web.xml";
    }

    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
    }

    protected static WebArchive exampleDeployment(String name, String contextPath) throws IOException {
        URL webXML = Paths.get(EXAMPLES_WEB_XML).toUri().toURL();
        String webXmlContent = IOUtils.toString(webXML.openStream())
                .replace("%CONTEXT_PATH%", contextPath);
        WebArchive webArchive = ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML)
                .add(new StringAsset(webXmlContent), "/WEB-INF/web.xml");
        return webArchive;
    }

    protected static JavaArchive exampleJarDeployment(String name) {
        return ShrinkWrap.createFromZipFile(JavaArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".jar"));
    }

}