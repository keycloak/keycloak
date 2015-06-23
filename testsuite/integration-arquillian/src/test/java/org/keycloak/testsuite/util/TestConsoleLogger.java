/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 *
 * TODO Changed System.out to org.slf4j.Logger
 */
public class TestConsoleLogger extends RunListener {

	private static SimpleDateFormat formatter = new SimpleDateFormat("hh:MM:ss");
	private boolean isFailure = false;

	@Override
	public void testStarted(Description d) throws Exception {
		System.out.println(String.format("\n[%s] STARTED: %s#%s", getFormattedTime(), d.getClassName(), d.getMethodName()));
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		isFailure = true;
		System.out.println(getMessage("FAILURE", failure.getDescription()));
	}

	@Override
	public void testIgnored(Description description) throws Exception {
		System.out.println(getMessage("IGNORED", description));
	}

	@Override
	public void testFinished(Description description) throws Exception {
		if (!isFailure) {
			System.out.println(getMessage("SUCCESS", description));
		}
	}

	private String getMessage(String status, Description d) {
		return String.format("[%s] %s: %s#%s\n", getFormattedTime(), status, d.getClassName(), d.getMethodName());
	}

	private String getFormattedTime() {
		return formatter.format(new Date());
	}

}
