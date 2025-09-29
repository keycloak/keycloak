package org.keycloak.representations.workflows;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.keycloak.util.JsonSerialization;

public class WorkflowDefinitionTest {

    @Test
    public void testFullDefinition() throws IOException {
        WorkflowRepresentation expected = new WorkflowRepresentation();

        expected.setId("workflow-id");
        expected.setUses("my-provider");
        expected.setName("my-name");
        expected.setOn("event");
        expected.setOnEventReset("event-reset-1", "event-reset-2");
        expected.setSteps(null);
        expected.setConditions(null);
        expected.setRecurring(true);
        expected.setEnabled(true);

        expected.setConditions(Arrays.asList(
                WorkflowConditionRepresentation.create()
                        .of("condition-1")
                        .withConfig("key1", "v1")
                        .withConfig("key2", "v1", "v2")
                        .build(),
                WorkflowConditionRepresentation.create()
                        .of("condition-2")
                        .withConfig("key1", "v1")
                        .withConfig("key2", "v1", "v2")
                        .build(),
                WorkflowConditionRepresentation.create()
                        .of("condition-1")
                        .withConfig("key1", "v1")
                        .withConfig("key2", "v1", "v2", "v3")
                        .build()));

        expected.setSteps(Arrays.asList(
                WorkflowStepRepresentation.create()
                        .of("step-1")
                        .id("1")
                        .withConfig("key1", "v1")
                        .after(Duration.ofSeconds(10))
                        .build(),
                WorkflowStepRepresentation.create()
                        .of("step-2")
                        .id("2")
                        .withConfig("key1", "v1", "v2")
                        .build(),
                WorkflowStepRepresentation.create()
                        .of("step-1")
                        .id("3")
                        .withConfig("key1", "v1")
                        .build()));

        String json = JsonSerialization.writeValueAsPrettyString(expected);

        WorkflowRepresentation actual = JsonSerialization.readValue(json, WorkflowRepresentation.class);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUses(), actual.getUses());
        assertTrue(actual.getOn() instanceof String);
        assertEquals(expected.getOn(), (String) actual.getOn());
        assertArrayEquals(((List<?>) expected.getOnEventReset()).toArray(), ((List<?>) actual.getOnEventReset()).toArray());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getRecurring(), actual.getRecurring());
        assertEquals(expected.getEnabled(), actual.getEnabled());

        List<WorkflowConditionRepresentation> actualConditions = actual.getConditions();
        assertNotNull(actualConditions);
        actualConditions = actualConditions.stream().sorted(Comparator.comparing(WorkflowConditionRepresentation::getUses)).collect(Collectors.toList());
        List<WorkflowConditionRepresentation> expectedConditions = expected.getConditions().stream().sorted(Comparator.comparing(WorkflowConditionRepresentation::getUses)).collect(Collectors.toList());

        assertEquals(expectedConditions.size(), actualConditions.size());
        assertEquals(expectedConditions.get(0).getUses(), actualConditions.get(0).getUses());
        assertEquals(expectedConditions.get(0).getConfig().get("key1"), actualConditions.get(0).getConfig().get("key1"));
        assertEquals(expectedConditions.get(0).getConfig().get("key2"), actualConditions.get(0).getConfig().get("key2"));
        assertEquals(expectedConditions.get(1).getConfig().get("key1"), actualConditions.get(1).getConfig().get("key1"));
        assertEquals(expectedConditions.get(1).getConfig().get("key2"), actualConditions.get(1).getConfig().get("key2"));
        assertEquals(expectedConditions.get(2).getConfig().get("key1"), actualConditions.get(2).getConfig().get("key1"));
        assertEquals(expectedConditions.get(2).getConfig().get("key2"), actualConditions.get(2).getConfig().get("key2"));


        List<WorkflowStepRepresentation> actualSteps = actual.getSteps();
        assertNotNull(actualSteps);
        actualSteps = actualSteps.stream().sorted(Comparator.comparing(WorkflowStepRepresentation::getUses)).collect(Collectors.toList());
        List<WorkflowStepRepresentation> expectedSteps = expected.getSteps().stream().sorted(Comparator.comparing(WorkflowStepRepresentation::getUses)).collect(Collectors.toList());

        assertEquals(expectedSteps.size(), actualSteps.size());
        assertEquals(expectedSteps.get(0).getUses(), actualSteps.get(0).getUses());
        assertEquals(expectedSteps.get(0).getConfig().get("key1"), actualSteps.get(0).getConfig().get("key1"));
        assertEquals(expectedSteps.get(1).getConfig().get("key1"), actualSteps.get(1).getConfig().get("key1"));
        assertEquals(expectedSteps.get(2).getConfig().get("key1"), actualSteps.get(2).getConfig().get("key1"));

        System.out.println(json);
    }

    @Test
    public void testOnEventAsArray() throws IOException {
        WorkflowRepresentation expected = new WorkflowRepresentation();

        expected.setOn("event", "event2");

        String json = JsonSerialization.writeValueAsPrettyString(expected);
        WorkflowRepresentation actual = JsonSerialization.readValue(json, WorkflowRepresentation.class);
        assertTrue(actual.getOn() instanceof List);
        assertEquals(Arrays.asList("event", "event2"), actual.getOn());

        System.out.println(json);
    }

    @Test
    public void testOnEventAsString() throws IOException {
        WorkflowRepresentation expected = new WorkflowRepresentation();

        expected.setOn("event");

        String json = JsonSerialization.writeValueAsPrettyString(expected);
        WorkflowRepresentation actual = JsonSerialization.readValue(json, WorkflowRepresentation.class);
        assertTrue(actual.getOn() instanceof String);
        assertEquals("event", actual.getOn());

        System.out.println(json);
    }

}
