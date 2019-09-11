package org.keycloak.testsuite.springboot;

import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.adapter.page.AbstractShowTokensPage;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenPage extends AbstractShowTokensPage {

	public static final String PAGE_TITLE = "tokens from spring boot";

	@Override
	public boolean isCurrent() {
		return driver.getTitle().equalsIgnoreCase(PAGE_TITLE);
	}

	public void assertIsCurrent() {
		assertThat(driver.getTitle()).isEqualToIgnoringCase(PAGE_TITLE);
	}

	@Override
	public URL getInjectedUrl() {
		return null;
	}
}
