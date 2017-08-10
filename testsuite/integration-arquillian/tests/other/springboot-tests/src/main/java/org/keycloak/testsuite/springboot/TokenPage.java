package org.keycloak.testsuite.springboot;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.adapter.page.AbstractShowTokensPage;

public class TokenPage extends AbstractShowTokensPage {

	@Override
	public boolean isCurrent() {
		return driver.getTitle().equalsIgnoreCase("tokens from spring boot");
	}

	@Override
	public URL getInjectedUrl() {
		return null;
	}
}
