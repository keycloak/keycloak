package org.keycloak.testsuite.arquillian;

import java.util.logging.Logger;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.keycloak.testsuite.adapter.Relative;

/**
 *
 * @author tkyjovsk
 */
public class ArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger log = Logger.getLogger(ArchiveProcessor.class.getName());

    @Override
    public void process(Archive<?> archive, TestClass testClass) {

        // FIXME figure out why this doesn't get called
        log.info("    PROCESSING ARCHIVE");
        if (!testClass.isAnnotationPresent(Relative.class)) {
            log.info("   TEST NOT RELATIVE");
            archive.addAsDirectory("/not_relative");
        }
    }

}
