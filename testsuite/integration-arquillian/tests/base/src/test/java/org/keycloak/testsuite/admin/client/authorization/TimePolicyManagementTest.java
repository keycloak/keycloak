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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.TimePoliciesResource;
import org.keycloak.admin.client.resource.TimePolicyResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TimePolicyManagementTest extends AbstractPolicyManagementTest {

    @Test
    public void testCreate() {
        AuthorizationResource authorization = getClient().authorization();
        assertCreated(authorization, createRepresentation("Time Policy"));
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        TimePolicyRepresentation representation = createRepresentation("Update Time Policy");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.setDayMonth("11");
        representation.setDayMonthEnd("22");
        representation.setMonth("7");
        representation.setMonthEnd("9");
        representation.setYear("2019");
        representation.setYearEnd("2030");
        representation.setHour("15");
        representation.setHourEnd("23");
        representation.setMinute("55");
        representation.setMinuteEnd("58");
        representation.setNotBefore("2019-01-01 00:00:00");
        representation.setNotOnOrAfter("2019-02-03 00:00:00");

        TimePoliciesResource policies = authorization.policies().time();
        TimePolicyResource permission = policies.findById(representation.getId());

        permission.update(representation);
        assertRepresentation(representation, permission);

        representation.setDayMonth(null);
        representation.setDayMonthEnd(null);
        representation.setMonth(null);
        representation.setMonthEnd(null);
        representation.setYear(null);
        representation.setYearEnd(null);
        representation.setHour(null);
        representation.setHourEnd(null);
        representation.setMinute(null);
        representation.setMinuteEnd(null);
        representation.setNotBefore(null);
        representation.setNotOnOrAfter("2019-02-03 00:00:00");

        permission.update(representation);
        assertRepresentation(representation, permission);

        representation.setNotOnOrAfter(null);
        representation.setHour("2");

        permission.update(representation);
        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        TimePolicyRepresentation representation = createRepresentation("Test Delete Policy");
        TimePoliciesResource policies = authorization.policies().time();

        try (Response response = policies.create(representation)) {
            TimePolicyRepresentation created = response.readEntity(TimePolicyRepresentation.class);

            policies.findById(created.getId()).remove();

            TimePolicyResource removed = policies.findById(created.getId());

            try {
                removed.toRepresentation();
                fail("Permission not removed");
            } catch (NotFoundException ignore) {

            }
        }
    }

    private TimePolicyRepresentation createRepresentation(String name) {
        TimePolicyRepresentation representation = new TimePolicyRepresentation();

        representation.setName(name);
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setDayMonth("1");
        representation.setDayMonthEnd("2");
        representation.setMonth("3");
        representation.setMonthEnd("4");
        representation.setYear("5");
        representation.setYearEnd("6");
        representation.setHour("7");
        representation.setHourEnd("8");
        representation.setMinute("9");
        representation.setMinuteEnd("10");
        representation.setNotBefore("2017-01-01 00:00:00");
        representation.setNotOnOrAfter("2017-02-01 00:00:00");
        return representation;
    }

    private void assertCreated(AuthorizationResource authorization, TimePolicyRepresentation representation) {
        TimePoliciesResource permissions = authorization.policies().time();

        try (Response response = permissions.create(representation)) {
            TimePolicyRepresentation created = response.readEntity(TimePolicyRepresentation.class);
            TimePolicyResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertRepresentation(TimePolicyRepresentation representation, TimePolicyResource permission) {
        TimePolicyRepresentation actual = permission.toRepresentation();
        assertRepresentation(representation, actual, () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
        assertEquals(representation.getDayMonth(), actual.getDayMonth());
        assertEquals(representation.getDayMonthEnd(), actual.getDayMonthEnd());
        assertEquals(representation.getMonth(), actual.getMonth());
        assertEquals(representation.getMonthEnd(), actual.getMonthEnd());
        assertEquals(representation.getYear(), actual.getYear());
        assertEquals(representation.getYearEnd(), actual.getYearEnd());
        assertEquals(representation.getHour(), actual.getHour());
        assertEquals(representation.getHourEnd(), actual.getHourEnd());
        assertEquals(representation.getMinute(), actual.getMinute());
        assertEquals(representation.getMinuteEnd(), actual.getMinuteEnd());
        assertEquals(representation.getNotBefore(), actual.getNotBefore());
        assertEquals(representation.getNotOnOrAfter(), actual.getNotOnOrAfter());
    }
}
