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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.policy.TimePolicy;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyManagementTest extends AbstractAuthorizationSettingsTest {

    @Test
    public void testUpdate() {
        authorizationPage.navigateTo();
        TimePolicyRepresentation expected = new TimePolicyRepresentation();

        expected.setName("Test Time Policy");
        expected.setDescription("description");
        expected.setNotBefore("2017-01-01 00:00:00");
        expected.setNotOnOrAfter("2018-01-01 00:00:00");
        expected.setDayMonth("1");
        expected.setDayMonthEnd("2");
        expected.setMonth("3");
        expected.setMonthEnd("4");
        expected.setYear("5");
        expected.setYearEnd("6");
        expected.setHour("7");
        expected.setHourEnd("8");
        expected.setMinute("9");
        expected.setMinuteEnd("10");

        expected = createPolicy(expected);

        String previousName = expected.getName();

        expected.setName("Changed Test Time Policy");
        expected.setDescription("Changed description");
        expected.setLogic(Logic.NEGATIVE);
        expected.setNotBefore("2018-01-01 00:00:00");
        expected.setNotOnOrAfter("2019-01-01 00:00:00");
        expected.setDayMonth("23");
        expected.setDayMonthEnd("25");
        expected.setMonth("11");
        expected.setMonthEnd("12");
        expected.setYear("2020");
        expected.setYearEnd("2021");
        expected.setHour("17");
        expected.setHourEnd("18");
        expected.setMinute("19");
        expected.setMinuteEnd("20");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(previousName, expected);
        assertAlertSuccess();

        authorizationPage.navigateTo();
        TimePolicy actual = authorizationPage.authorizationTabs().policies().name(expected.getName());

        assertPolicy(expected, actual);

        expected.setNotBefore("");
        expected.setNotOnOrAfter("");

        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().update(expected.getName(), expected);
        assertAlertSuccess();
    }

    @Test
    public void testDelete() {
        authorizationPage.navigateTo();
        TimePolicyRepresentation expected = new TimePolicyRepresentation();

        expected.setName("Test Time Policy");
        expected.setDescription("description");
        expected.setNotBefore("2017-01-01 00:00:00");
        expected.setNotOnOrAfter("2018-01-01 00:00:00");
        expected.setDayMonth("1");
        expected.setDayMonthEnd("2");
        expected.setMonth("3");
        expected.setMonthEnd("4");
        expected.setYear("5");
        expected.setYearEnd("6");
        expected.setHour("7");
        expected.setHourEnd("8");
        expected.setMinute("9");
        expected.setMinuteEnd("10");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().delete(expected.getName());
        assertAlertSuccess();
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    @Test
    public void testDeleteFromList() {
        authorizationPage.navigateTo();
        TimePolicyRepresentation expected = new TimePolicyRepresentation();

        expected.setName("Test Time Policy");
        expected.setDescription("description");
        expected.setNotBefore("2017-01-01 00:00:00");
        expected.setNotOnOrAfter("2018-01-01 00:00:00");
        expected.setDayMonth("1");
        expected.setDayMonthEnd("2");
        expected.setMonth("3");
        expected.setMonthEnd("4");
        expected.setYear("5");
        expected.setYearEnd("6");
        expected.setHour("7");
        expected.setHourEnd("8");
        expected.setMinute("9");
        expected.setMinuteEnd("10");

        expected = createPolicy(expected);
        authorizationPage.navigateTo();
        authorizationPage.authorizationTabs().policies().deleteFromList(expected.getName());
        authorizationPage.navigateTo();
        assertNull(authorizationPage.authorizationTabs().policies().policies().findByName(expected.getName()));
    }

    private TimePolicyRepresentation createPolicy(TimePolicyRepresentation expected) {
        TimePolicy policy = authorizationPage.authorizationTabs().policies().create(expected);
        assertAlertSuccess();
        return assertPolicy(expected, policy);
    }

    private TimePolicyRepresentation assertPolicy(TimePolicyRepresentation expected, TimePolicy policy) {
        TimePolicyRepresentation actual = policy.toRepresentation();

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getLogic(), actual.getLogic());
        assertEquals(expected.getNotBefore(), actual.getNotBefore());
        assertEquals(expected.getNotOnOrAfter(), actual.getNotOnOrAfter());
        assertEquals(expected.getDayMonth(), actual.getDayMonth());
        assertEquals(expected.getDayMonthEnd(), actual.getDayMonthEnd());
        assertEquals(expected.getHour(), actual.getHour());
        assertEquals(expected.getHourEnd(), actual.getHourEnd());
        assertEquals(expected.getMinute(), actual.getMinute());
        assertEquals(expected.getMinuteEnd(), actual.getMinuteEnd());
        assertEquals(expected.getMonth(), actual.getMonth());
        assertEquals(expected.getMonthEnd(), actual.getMonthEnd());
        assertEquals(expected.getYear(), actual.getYear());
        assertEquals(expected.getYearEnd(), actual.getYearEnd());

        return actual;
    }
}
