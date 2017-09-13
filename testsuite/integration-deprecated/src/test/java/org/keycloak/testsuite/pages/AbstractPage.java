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

import org.junit.Assert;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractPage {

    @WebResource
    protected WebDriver driver;

    public void assertCurrent() {
        String name = getClass().getSimpleName();
        Assert.assertTrue("Expected " + name + " but was " + driver.getTitle() + " (" + driver.getCurrentUrl() + ")",
                isCurrent());
    }

    abstract public boolean isCurrent();

    abstract public void open() throws Exception;

}
