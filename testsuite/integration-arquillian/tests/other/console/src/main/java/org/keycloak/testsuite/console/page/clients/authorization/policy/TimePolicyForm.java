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

import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.console.page.fragment.ModalDialog;
import org.keycloak.testsuite.page.Form;
import org.keycloak.testsuite.util.UIUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyForm extends Form {

    @FindBy(id = "name")
    private WebElement name;

    @FindBy(id = "description")
    private WebElement description;

    @FindBy(id = "logic")
    private Select logic;

    @FindBy(id = "notBefore")
    private WebElement notBefore;

    @FindBy(id = "notOnOrAfter")
    private WebElement notOnOrAfter;

    @FindBy(id = "dayMonth")
    private WebElement dayMonth;

    @FindBy(id = "month")
    private WebElement month;

    @FindBy(id = "year")
    private WebElement year;

    @FindBy(id = "hour")
    private WebElement hour;

    @FindBy(id = "minute")
    private WebElement minute;

    @FindBy(id = "dayMonthEnd")
    private WebElement dayMonthEnd;

    @FindBy(id = "monthEnd")
    private WebElement monthEnd;

    @FindBy(id = "yearEnd")
    private WebElement yearEnd;

    @FindBy(id = "hourEnd")
    private WebElement hourEnd;

    @FindBy(id = "minuteEnd")
    private WebElement minuteEnd;


    @FindBy(xpath = "//i[contains(@class,'pficon-delete')]")
    private WebElement deleteButton;

    @FindBy(xpath = "//div[@class='modal-dialog']")
    protected ModalDialog modalDialog;

    public void populate(TimePolicyRepresentation expected, boolean save) {
        UIUtils.setTextInputValue(name, expected.getName());
        UIUtils.setTextInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());
        UIUtils.setTextInputValue(notBefore, expected.getNotBefore());
        UIUtils.setTextInputValue(notOnOrAfter, expected.getNotOnOrAfter());
        UIUtils.setTextInputValue(dayMonth, expected.getDayMonth());
        UIUtils.setTextInputValue(dayMonthEnd, expected.getDayMonthEnd());
        UIUtils.setTextInputValue(month, expected.getMonth());
        UIUtils.setTextInputValue(monthEnd, expected.getMonthEnd());
        UIUtils.setTextInputValue(year, expected.getYear());
        UIUtils.setTextInputValue(yearEnd, expected.getYearEnd());
        UIUtils.setTextInputValue(hour, expected.getHour());
        UIUtils.setTextInputValue(hourEnd, expected.getHourEnd());
        UIUtils.setTextInputValue(minute, expected.getMinute());
        UIUtils.setTextInputValue(minuteEnd, expected.getMinuteEnd());

        if (save) {
            save();
        }
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public TimePolicyRepresentation toRepresentation() {
        TimePolicyRepresentation representation = new TimePolicyRepresentation();

        representation.setName(UIUtils.getTextInputValue(name));
        representation.setDescription(UIUtils.getTextInputValue(description));
        representation.setLogic(Logic.valueOf(UIUtils.getTextFromElement(logic.getFirstSelectedOption()).toUpperCase()));
        representation.setDayMonth(UIUtils.getTextInputValue(dayMonth));
        representation.setDayMonthEnd(UIUtils.getTextInputValue(dayMonthEnd));
        representation.setMonth(UIUtils.getTextInputValue(month));
        representation.setMonthEnd(UIUtils.getTextInputValue(monthEnd));
        representation.setYear(UIUtils.getTextInputValue(year));
        representation.setYearEnd(UIUtils.getTextInputValue(yearEnd));
        representation.setHour(UIUtils.getTextInputValue(hour));
        representation.setHourEnd(UIUtils.getTextInputValue(hourEnd));
        representation.setMinute(UIUtils.getTextInputValue(minute));
        representation.setMinuteEnd(UIUtils.getTextInputValue(minuteEnd));
        representation.setNotBefore(UIUtils.getTextInputValue(notBefore));
        representation.setNotOnOrAfter(UIUtils.getTextInputValue(notOnOrAfter));

        return representation;
    }
}