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

import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractPage {

    @ArquillianResource
    protected SuiteContext suiteContext;

    @ArquillianResource
    protected WebDriver driver;

    @ArquillianResource
    protected OAuthClient oauth;

    public void assertCurrent() {
        WaitUtils.waitForPageToLoad();
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent());
    }

    protected URI getAuthServerRoot() {
        try {
            return KeycloakUriBuilder.fromUri(suiteContext.getAuthServerInfo().getBrowserContextRoot().toURI()).path("/auth/").build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    abstract public boolean isCurrent();

    abstract public void open() throws Exception;

    public void setDriver(WebDriver driver) {
        this.driver = driver ;
        oauth.setDriver(driver);
    }

}
