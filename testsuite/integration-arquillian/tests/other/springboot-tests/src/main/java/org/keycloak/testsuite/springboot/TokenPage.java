package org.keycloak.testsuite.springboot;

import java.net.URL;

import org.keycloak.testsuite.adapter.page.AbstractShowTokensPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TokenPage extends AbstractShowTokensPage {

	public static final String PAGE_TITLE = "tokens from spring boot";

	@Override
	public boolean isCurrent() {
		return driver.getTitle().equalsIgnoreCase(PAGE_TITLE.toLowerCase());
	}

	public void assertIsCurrent() {
		assertThat(driver.getTitle().toLowerCase(), is(equalTo(PAGE_TITLE)));
	}

	@Override
	public URL getInjectedUrl() {
		return null;
	}
}
