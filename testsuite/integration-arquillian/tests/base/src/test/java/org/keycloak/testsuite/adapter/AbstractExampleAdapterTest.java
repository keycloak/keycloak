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
    public static final String EXAMPLES_WEB_XML;

    static {
        EXAMPLES_HOME = System.getProperty("examples.home", null);
        Assert.assertNotNull("Property ${examples.home} must bet set.", EXAMPLES_HOME);
        System.out.println(EXAMPLES_HOME);

        EXAMPLES_VERSION_SUFFIX = System.getProperty("examples.version.suffix", null);
        Assert.assertNotNull("Property ${examples.version.suffix} must bet set.", EXAMPLES_VERSION_SUFFIX);
        System.out.println(EXAMPLES_VERSION_SUFFIX);

        EXAMPLES_HOME_DIR = EXAMPLES_HOME + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX;

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