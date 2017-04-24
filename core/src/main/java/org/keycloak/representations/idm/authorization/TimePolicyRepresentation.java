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
package org.keycloak.representations.idm.authorization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyRepresentation extends AbstractPolicyRepresentation {

    private String notBefore;
    private String notOnOrAfter;
    private String dayMonth;
    private String dayMonthEnd;
    private String month;
    private String monthEnd;
    private String year;
    private String yearEnd;
    private String hour;
    private String hourEnd;
    private String minute;
    private String minuteEnd;

    @Override
    public String getType() {
        return "time";
    }

    public String getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(String notBefore) {
        this.notBefore = notBefore;
    }

    public String getNotOnOrAfter() {
        return notOnOrAfter;
    }

    public void setNotOnOrAfter(String notOnOrAfter) {
        this.notOnOrAfter = notOnOrAfter;
    }

    public String getDayMonth() {
        return dayMonth;
    }

    public void setDayMonth(String dayMonth) {
        this.dayMonth = dayMonth;
    }

    public String getDayMonthEnd() {
        return dayMonthEnd;
    }

    public void setDayMonthEnd(String dayMonthEnd) {
        this.dayMonthEnd = dayMonthEnd;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getMonthEnd() {
        return monthEnd;
    }

    public void setMonthEnd(String monthEnd) {
        this.monthEnd = monthEnd;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(String yearEnd) {
        this.yearEnd = yearEnd;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getHourEnd() {
        return hourEnd;
    }

    public void setHourEnd(String hourEnd) {
        this.hourEnd = hourEnd;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public String getMinuteEnd() {
        return minuteEnd;
    }

    public void setMinuteEnd(String minuteEnd) {
        this.minuteEnd = minuteEnd;
    }
}
