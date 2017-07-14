package org.keycloak.testsuite.runonserver;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServerException;

/**
 * Created by st on 26.01.17.
 */
public class RunOnServerDeployment {

    @Deployment
    public static WebArchive create(Class<?> ... classes) {
        return ShrinkWrap.create(WebArchive.class, "run-on-server-classes.war")
                .addAsManifestResource("run-on-server-jboss-deployment-structure.xml","jboss-deployment-structure.xml")
                .addClasses(classes)
                .addClass(AbstractKeycloakTest.class)
                .addClass(RunOnServerException.class);
    }

}
