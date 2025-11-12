package org.keycloak.representations.workflows;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkflowDefinitionTest {

    @Test
    public void testFullDefinition() throws IOException {
        WorkflowRepresentation expected = new WorkflowRepresentation();

        expected.setId("workflow-id");
        expected.setName("my-name");
        expected.setOn("event");
        expected.setConditions("condition-1(v1) AND (condition-2(key1:v1) OR condition-3(key2:v2,v3))");
        expected.setSteps(null);
        expected.setEnabled(true);

        expected.setConcurrency(new WorkflowConcurrencyRepresentation(true));

        expected.setSteps(Arrays.asList(
                WorkflowStepRepresentation.create()
                        .of("step-1")
                        .withConfig("key1", "v1")
                        .after(Duration.ofSeconds(10))
                        .build(),
                WorkflowStepRepresentation.create()
                        .of("step-2")
                        .withConfig("key1", "v1", "v2")
                        .build(),
                WorkflowStepRepresentation.create()
                        .of("step-1")
                        .withConfig("key1", "v1")
                        .build()));

        String json = JsonSerialization.writeValueAsPrettyString(expected);

        WorkflowRepresentation actual = JsonSerialization.readValue(json, WorkflowRepresentation.class);

        assertEquals(expected.getId(), actual.getId());
        assertNotNull(actual.getOn());
        assertEquals(expected.getOn(), actual.getOn());
        assertEquals(expected.getConcurrency(), actual.getConcurrency());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getEnabled(), actual.getEnabled());
        assertEquals(expected.getConditions(), actual.getConditions());

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
    public void testOnEventAsString() throws IOException {
        WorkflowRepresentation expected = new WorkflowRepresentation();

        expected.setOn("event OR other-event");

        String json = JsonSerialization.writeValueAsPrettyString(expected);
        WorkflowRepresentation actual = JsonSerialization.readValue(json, WorkflowRepresentation.class);
        assertNotNull(actual.getOn());
        assertEquals("event OR other-event", actual.getOn());

        System.out.println(json);
    }

}
