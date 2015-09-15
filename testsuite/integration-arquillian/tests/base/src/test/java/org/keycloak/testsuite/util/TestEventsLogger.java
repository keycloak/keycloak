package org.keycloak.testsuite.util;

import org.jboss.logging.Logger;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 *
 * @author Petr Mensik
 * @author tkyjovsk
 */
public class TestEventsLogger extends RunListener {

    private Logger log(Description d) {
        return Logger.getLogger(d.getClassName());
    }

    private String getMessage(Description d, String status) {
        return String.format("[%s] %s() %s", d.getTestClass().getSimpleName(), d.getMethodName(), status);
    }

    @Override
    public void testStarted(Description d) throws Exception {
        log(d).info(getMessage(d, "STARTED"));
    }

    @Override
    public void testFailure(Failure f) throws Exception {
        Description d = f.getDescription();
        log(d).error(getMessage(d, "FAILED"));
    }

    @Override
    public void testIgnored(Description d) throws Exception {
        log(d).warn(getMessage(d, "IGNORED\n\n"));
    }

    @Override
    public void testFinished(Description d) throws Exception {
        log(d).info(getMessage(d, "FINISHED\n\n"));
    }

}
