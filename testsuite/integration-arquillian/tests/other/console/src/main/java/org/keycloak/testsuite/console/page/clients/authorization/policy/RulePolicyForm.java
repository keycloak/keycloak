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
package org.keycloak.testsuite.console.page.clients.authorization.policy;

import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RulePolicyRepresentation;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RulePolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "artifactGroupId")
    private WebElement artifactGroupId;

    @FindBy(id = "artifactId")
    private WebElement artifactId;

    @FindBy(id = "artifactVersion")
    private WebElement artifactVersion;

    @FindBy(id = "moduleName")
    private Select moduleName;

    @FindBy(id = "sessionName")
    private Select sessionName;

    @FindBy(id = "scannerPeriod")
    private WebElement scannerPeriod;

    @FindBy(id = "scannerPeriodUnit")
    private Select scannerPeriodUnit;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    @FindBy(id = "resolveModule")
    private WebElement resolveModuleButton;

    public void populate(RulePolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        setInputValue(artifactGroupId, expected.getArtifactGroupId());
        setInputValue(artifactId, expected.getArtifactId());
        setInputValue(artifactVersion, expected.getArtifactVersion());

        resolveModuleButton.click();
        WaitUtils.waitForPageToLoad();

        moduleName.selectByVisibleText(expected.getModuleName());
        WaitUtils.pause(1000);

        sessionName.selectByVisibleText(expected.getSessionName());

        setInputValue(scannerPeriod, expected.getScannerPeriod());
        scannerPeriodUnit.selectByVisibleText(expected.getScannerPeriodUnit());
        logic.selectByValue(expected.getLogic().name());

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public RulePolicyRepresentation toRepresentation() {
        RulePolicyRepresentation representation = new RulePolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setArtifactGroupId(getInputValue(artifactGroupId));
        representation.setArtifactId(getInputValue(artifactId));
        representation.setArtifactVersion(getInputValue(artifactVersion));
        representation.setModuleName(moduleName.getFirstSelectedOption().getText());
        representation.setSessionName(sessionName.getFirstSelectedOption().getText());
        representation.setScannerPeriod(getInputValue(scannerPeriod));
        representation.setScannerPeriodUnit(scannerPeriodUnit.getFirstSelectedOption().getText());

        return representation;
    }
}