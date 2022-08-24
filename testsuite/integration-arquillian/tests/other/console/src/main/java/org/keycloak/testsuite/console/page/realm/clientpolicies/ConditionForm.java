/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.realm.clientpolicies;

import org.keycloak.testsuite.console.page.fragment.MultipleStringSelect2;
import org.keycloak.testsuite.page.Form;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ConditionForm extends Form {
    @FindBy(xpath = ".//select[starts-with(@id, 'conditionType')]")
    private Select conditionTypeSelect;

    @FindBy(xpath = ".//*[starts-with(@id, 's2id_autogen')]")
    private MultipleStringSelect2 select2;

    public String getConditionType() {
        return conditionTypeSelect.getFirstSelectedOption().getText();
    }

    public void setConditionType(String conditionType) {
        conditionTypeSelect.selectByVisibleText(conditionType);
    }

    public Set<String> getSelect2SelectedItems() {
        return select2.getSelected();
    }

    public void selectSelect2Item(String item) {
        select2.select(item);
    }
}
