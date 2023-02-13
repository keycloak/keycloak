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

package org.keycloak.testsuite.page;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.URLUtils;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPage {

    protected final Logger log = Logger.getLogger(this.getClass());
    
    private final Map<String, Object> uriParameters = new HashMap<>();

    @Drone
    protected WebDriver driver;

    private UriBuilder builder;

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public abstract UriBuilder createUriBuilder();

    public String getUriFragment() {
        return "";
    }

    /**
     *
     * @return Instance of UriBuilder that can build URIs for a concrete page.
     */
    public UriBuilder getUriBuilder() {
        if (builder == null) {
            builder = createUriBuilder();
            String fragment = getUriFragment();
            if (fragment != null && !fragment.isEmpty()) {
                builder.fragment(fragment);
            }
        }
        return builder;
    }

    public AbstractPage setUriParameter(String name, Object value) {
        uriParameters.put(name, value);
        return this;
    }

    public AbstractPage removeUriParameter(String name) {
        uriParameters.remove(name);
        return this;
    }

    public Object getUriParameter(String name) {
        return uriParameters.get(name);
    }

    public URI buildUri() {
        return getUriBuilder().buildFromMap(uriParameters);
    }

    @Override
    public String toString() {
        return buildUri().toASCIIString();
    }

    public void navigateTo() {
        URLUtils.navigateToUri(buildUri().toASCIIString());
    }

    public boolean isCurrent() {
        return URLUtils.currentUrlEquals(toString());
    }

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + DroneUtils.getCurrentDriver().getTitle() + " (" + DroneUtils.getCurrentDriver().getCurrentUrl() + ")",
                isCurrent());
    }
}
