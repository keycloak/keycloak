/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.arquillian.jira;

import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.execution.TestExecutionDecider;

/**
 *
 * @author <a href="mailto:pmensik@redhat.com">Petr Mensik</a>
 */
public class JiraExtension implements LoadableExtension {

	@Override
	public void register(ExtensionBuilder builder) {
		builder.service(TestExecutionDecider.class, JiraTestExecutionDecider.class);
	}

}
