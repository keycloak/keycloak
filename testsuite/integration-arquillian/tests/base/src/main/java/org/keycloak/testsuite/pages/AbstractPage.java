/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.pages;

import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractPage {

    @ArquillianResource
    protected WebDriver driver;

    @ArquillianResource
    protected OAuthClient oauth;

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isCurrent());
    }

    abstract public boolean isCurrent();

    public boolean isCurrent(String expectedTitle) {
        return PageUtils.getPageTitle(driver).equals(expectedTitle);
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver ;
        oauth.setDriver(driver);
    }

}
