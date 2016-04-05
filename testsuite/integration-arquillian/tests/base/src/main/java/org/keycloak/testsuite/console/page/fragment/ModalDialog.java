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

package org.keycloak.testsuite.console.page.fragment;

import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

import org.jboss.arquillian.graphene.fragment.Root;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class ModalDialog {

    @Root
    private WebElement root;

    @FindBy(xpath = ".//button[text()='Cancel']")
    private WebElement cancelButton;
    @FindBy(xpath = ".//button[text()='Delete']")
    private WebElement deleteButton;

    @FindBy(xpath = ".//button[@ng-click='ok()']")
    private WebElement okButton;
    @FindBy(id = "name")
    private WebElement nameInput;

    public void ok() {
        waitUntilElement(okButton).is().present();
        okButton.click();
        waitUntilElement(root).is().not().present();
    }
    
    public void confirmDeletion() {
        waitUntilElement(deleteButton).is().present();
        deleteButton.click();
        waitUntilElement(root).is().not().present();
        pause(200);
    }

    public void cancel() {
        waitUntilElement(cancelButton).is().present();
        cancelButton.click();
        waitUntilElement(root).is().not().present();
    }

    public void setName(String name) {
        waitUntilElement(nameInput).is().present();
        nameInput.clear();
        nameInput.sendKeys(name);
    }
}