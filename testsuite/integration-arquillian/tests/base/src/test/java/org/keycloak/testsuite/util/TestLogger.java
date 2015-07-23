/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class TestLogger extends RunListener {

	private final Logger log = LoggerFactory.getLogger(TestLogger.class);

	@Override
	public void testStarted(Description description) throws Exception {
		log.info("{} STARTED: {}#{}", getCurrentTimeFormatted(), description.getClassName(), description.getMethodName());
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		log.info("{} FAILED: {}#{}", getCurrentTimeFormatted(), failure.getDescription().getClassName(),
				failure.getDescription().getMethodName());
	}

	/*
	 @Override
	 protected void succeeded(Description description) {
	 log.info("{} SUCCESS: {}#{}", getCurrentTimeFormatted(), description.getClassName(), description.getMethodName());
	 }

	 @Override
	 protected void skipped(AssumptionViolatedException e, Description description) {
	 log.info("{} SKIPPED: {}#{}", getCurrentTimeFormatted(), description.getClassName(), description.getMethodName());
	 }*/
	private String getCurrentTimeFormatted() {
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		return "[" + formatter.format(new Date()) + "]";
	}

}
