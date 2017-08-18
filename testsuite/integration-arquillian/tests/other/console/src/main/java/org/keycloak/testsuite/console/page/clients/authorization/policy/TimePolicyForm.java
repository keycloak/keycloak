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

    public void populate(TimePolicyRepresentation expected) {
        setInputValue(name, expected.getName());
        setInputValue(description, expected.getDescription());
        logic.selectByValue(expected.getLogic().name());
        setInputValue(notBefore, expected.getNotBefore());
        setInputValue(notOnOrAfter, expected.getNotOnOrAfter());
        setInputValue(dayMonth, expected.getDayMonth());
        setInputValue(dayMonthEnd, expected.getDayMonthEnd());
        setInputValue(month, expected.getMonth());
        setInputValue(monthEnd, expected.getMonthEnd());
        setInputValue(year, expected.getYear());
        setInputValue(yearEnd, expected.getYearEnd());
        setInputValue(hour, expected.getHour());
        setInputValue(hourEnd, expected.getHourEnd());
        setInputValue(minute, expected.getMinute());
        setInputValue(minuteEnd, expected.getMinuteEnd());

        save();
    }

    public void delete() {
        deleteButton.click();
        modalDialog.confirmDeletion();
    }

    public TimePolicyRepresentation toRepresentation() {
        TimePolicyRepresentation representation = new TimePolicyRepresentation();

        representation.setName(getInputValue(name));
        representation.setDescription(getInputValue(description));
        representation.setLogic(Logic.valueOf(logic.getFirstSelectedOption().getText().toUpperCase()));
        representation.setDayMonth(getInputValue(dayMonth));
        representation.setDayMonthEnd(getInputValue(dayMonthEnd));
        representation.setMonth(getInputValue(month));
        representation.setMonthEnd(getInputValue(monthEnd));
        representation.setYear(getInputValue(year));
        representation.setYearEnd(getInputValue(yearEnd));
        representation.setHour(getInputValue(hour));
        representation.setHourEnd(getInputValue(hourEnd));
        representation.setMinute(getInputValue(minute));
        representation.setMinuteEnd(getInputValue(minuteEnd));
        representation.setNotBefore(getInputValue(notBefore));
        representation.setNotOnOrAfter(getInputValue(notOnOrAfter));

        return representation;
    }
}