package org.keycloak.testsuite.pages;

import org.junit.Assert;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;

public abstract class Page {

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Exptected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isCurrent());
    }

    abstract boolean isCurrent();

    abstract void open();

}
