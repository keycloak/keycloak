package org.keycloak.testsuite.springboot;

import org.keycloak.testsuite.pages.AbstractPage;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSpringbootPage extends AbstractPage {

    protected String title;

    public AbstractSpringbootPage(String title) {
        this.title = title;
    }

    public void assertIsCurrent() {
        assertThat(driver.getTitle()).isEqualToIgnoringCase(title);
    }

    @Override
    public boolean isCurrent() {
        return driver.getTitle().equalsIgnoreCase(title);
    }

    @Override
    public void open() throws Exception {
    }

}
