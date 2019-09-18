package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

public abstract class AbstractSpringbootPage extends AbstractPage {

    protected String title;

    public AbstractSpringbootPage(String title) {
        this.title = title;
    }

    public void assertIsCurrent() {
        assertThat(driver.getTitle().toLowerCase(), is(equalTo(title.toLowerCase())));
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase(title);
    }

    @Override
    public void open() throws Exception {
    }

}
