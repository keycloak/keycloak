/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.idp.mappers;

import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import static org.keycloak.testsuite.util.UIUtils.setTextInputValue;

/**
 * @author Martin Idel <external.Martin.Idel@bosch.io>
 */
public class IdentityProviderMapperForm extends Form {
    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "syncMode")
    private Select syncMode;

    @FindBy(id = "mapperTypeCreate")
    private Select mapperType;

    public void setName(final String value) {
        setTextInputValue(name, value);
    }

    public void setSyncMode(final String value) {
        syncMode.selectByVisibleText(value);
    }

    public String syncMode() {
        return syncMode.getFirstSelectedOption().getText();
    }

    public void setMapperType(final String value) {
        mapperType.selectByVisibleText(value);
    }

    public String getMapperType() {
        return mapperType.getFirstSelectedOption().getText();
    }
}
