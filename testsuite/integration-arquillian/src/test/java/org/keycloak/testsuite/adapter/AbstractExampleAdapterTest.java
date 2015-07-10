package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import static org.keycloak.testsuite.adapter.AbstractAdapterTest.JBOSS_DEPLOYMENT_STRUCTURE_XML;
import static org.keycloak.testsuite.adapter.AbstractAdapterTest.jbossDeploymentStructure;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractExampleAdapterTest extends AbstractAdapterTest {
    
    public static final String EXAMPLES_HOME;
    public static final String EXAMPLES_VERSION_SUFFIX;
    public static final String EXAMPLES_HOME_DIR;

    static {
        EXAMPLES_HOME = System.getProperty("examples.home", null);
        Assert.assertNotNull(EXAMPLES_HOME, "Property ${examples.home} must bet set.");
        System.out.println(EXAMPLES_HOME);

        EXAMPLES_VERSION_SUFFIX = System.getProperty("examples.version.suffix", null);
        Assert.assertNotNull(EXAMPLES_VERSION_SUFFIX, "Property ${examples.version.suffix} must bet set.");
        System.out.println(EXAMPLES_VERSION_SUFFIX);
        
        EXAMPLES_HOME_DIR = EXAMPLES_HOME + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX;
    }
    
    protected static WebArchive exampleDeployment(String name) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + name + "-" + EXAMPLES_VERSION_SUFFIX + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
    }

}
