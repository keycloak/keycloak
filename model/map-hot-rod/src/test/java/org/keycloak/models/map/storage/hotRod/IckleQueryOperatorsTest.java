package org.keycloak.models.map.storage.hotRod;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

public class IckleQueryOperatorsTest {

    @Test
    public void testFindAvailableNamedParamSimple() {
        Set<String> existingNames = new HashSet<>();

        String param = IckleQueryOperators.findAvailableNamedParam(existingNames, "clientId");

        assertThat("should create the first ID", param, Matchers.equalTo("clientId0"));
    }

    @Test
    public void testFindAvailableNamedParamAlreadyExists() {
        Set<String> existingNames = new HashSet<>();
        existingNames.add("clientId0");

        String param = IckleQueryOperators.findAvailableNamedParam(existingNames, "clientId");

        assertThat("should create the next ID as clientId0 is already taken", param, Matchers.equalTo("clientId1"));
    }

    @Test
    public void testFindAvailableNamedParamIllegalCharacterInPrefix() {
        Set<String> existingNames = new HashSet<>();
        existingNames.add("clientid0");

        String param = IckleQueryOperators.findAvailableNamedParam(existingNames, "client.id");

        assertThat("should remove non-characters and non-numbers from the ID", param, Matchers.equalTo("clientid1"));
    }

}