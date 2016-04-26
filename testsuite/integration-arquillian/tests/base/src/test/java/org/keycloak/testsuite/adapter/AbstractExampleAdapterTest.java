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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractExampleAdapterTest extends AbstractAdapterTest {

    public static final String TEST_APPS_HOME;
    public static final String TEST_APPS_VERSION_SUFFIX;
    public static final String TEST_APPS_HOME_DIR;
    public static final String TEST_APPS_WEB_XML;

    static {
        TEST_APPS_HOME = System.getProperty("test-apps.home", null);
        Assert.assertNotNull("Property ${test-apps.home} must bet set.", TEST_APPS_HOME);
        System.out.println(TEST_APPS_HOME);

        TEST_APPS_VERSION_SUFFIX = System.getProperty("test-apps.version.suffix", null);
        Assert.assertNotNull("Property ${test-apps.version.suffix} must bet set.", TEST_APPS_VERSION_SUFFIX);
        System.out.println(TEST_APPS_VERSION_SUFFIX);

//        if (!System.getProperty("unpacked.container.folder.name","").isEmpty()) {
//            TEST_APPS_HOME_DIR = TEST_APPS_HOME + "/" + System.getProperty("unpacked.container.folder.name","") + "-test-apps";
//        } else {
//            TEST_APPS_HOME_DIR = TEST_APPS_HOME + "/Keycloak-" + TEST_APPS_VERSION_SUFFIX + "-test-apps";
//        }

        TEST_APPS_HOME_DIR = TEST_APPS_HOME + "/test-apps-dist";

        TEST_APPS_WEB_XML = TEST_APPS_HOME + "/web.xml";
    }

    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(TEST_APPS_HOME + "/" + name + "-" + TEST_APPS_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
    }

    protected static WebArchive exampleDeployment(String name, String contextPath) throws IOException {
        URL webXML = Paths.get(TEST_APPS_WEB_XML).toUri().toURL();
        String webXmlContent = IOUtils.toString(webXML.openStream())
                .replace("%CONTEXT_PATH%", contextPath);
        WebArchive webArchive = ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(TEST_APPS_HOME + "/" + name + "-" + TEST_APPS_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML)
                .add(new StringAsset(webXmlContent), "/WEB-INF/web.xml");
        return webArchive;
    }

    protected static JavaArchive exampleJarDeployment(String name) {
        return ShrinkWrap.createFromZipFile(JavaArchive.class,
                new File(TEST_APPS_HOME + "/" + name + "-" + TEST_APPS_VERSION_SUFFIX + ".jar"));
    }

}