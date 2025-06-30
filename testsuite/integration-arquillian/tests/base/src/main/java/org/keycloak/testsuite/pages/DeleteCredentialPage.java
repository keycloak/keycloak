/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.pages;

import org.junit.Assert;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DeleteCredentialPage extends AbstractPage {

    @FindBy(id = "kc-accept")
    private WebElement submitButton;

    @FindBy(id = "kc-decline")
    private WebElement cancelButton;

    @FindBy(id = "kc-delete-text")
    private WebElement message;

    public boolean isCurrent() {
        return PageUtils.getPageTitle(driver).startsWith("Delete ");
    }

    public void confirm() {
        submitButton.click();
    }
    public void cancel() {
        cancelButton.click();
    }

    public void assertCredentialInMessage(String expectedLabel) {
        Assert.assertEquals("Do you want to delete " + expectedLabel + "?", message.getText());
    }

    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }
}
