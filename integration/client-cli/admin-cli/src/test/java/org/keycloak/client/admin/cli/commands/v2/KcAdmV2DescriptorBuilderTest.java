package org.keycloak.client.admin.cli.commands.v2;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.client.admin.cli.v2.KcAdmV2CommandDescriptor;
import org.keycloak.client.admin.cli.v2.KcAdmV2DescriptorBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KcAdmV2DescriptorBuilderTest {

    private static final String TEST_OPENAPI = "/test-openapi.json";

    @Test
    public void testConvertProducesCorrectResourceName() {
        KcAdmV2CommandDescriptor descriptor = buildDescriptor();

        assertNotNull(descriptor.getResources());
        assertEquals(1, descriptor.getResources().size());
        assertEquals("thing", descriptor.getResources().get(0).getName());
    }

    @Test
    public void testVersion() {
        KcAdmV2CommandDescriptor descriptor = buildDescriptor();
        assertEquals("1.0.0-test", descriptor.getVersion());
    }

    @Test
    public void testConvertProducesAllCommands() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        assertEquals("Should have 5 commands", 5, byName.size());
        assertTrue(byName.containsKey("list"));
        assertTrue(byName.containsKey("create"));
        assertTrue(byName.containsKey("get"));
        assertTrue(byName.containsKey("patch"));
        assertTrue(byName.containsKey("delete"));
    }

    @Test
    public void testCollectionCommandsDoNotRequireId() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        assertFalse("list should not require id", byName.get("list").isRequiresId());
        assertFalse("create should not require id", byName.get("create").isRequiresId());
    }

    @Test
    public void testSingleResourceCommandsRequireId() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        assertTrue("get should require id", byName.get("get").isRequiresId());
        assertTrue("patch should require id", byName.get("patch").isRequiresId());
        assertTrue("delete should require id", byName.get("delete").isRequiresId());
    }

    @Test
    public void testHttpMethodsAreCorrect() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        assertEquals("GET", byName.get("list").getHttpMethod());
        assertEquals("POST", byName.get("create").getHttpMethod());
        assertEquals("GET", byName.get("get").getHttpMethod());
        assertEquals("PATCH", byName.get("patch").getHttpMethod());
        assertEquals("DELETE", byName.get("delete").getHttpMethod());
    }

    @Test
    public void testDescriptionsFromOpenApiSummary() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        assertEquals("List all things", byName.get("list").getDescription());
        assertEquals("Create a thing", byName.get("create").getDescription());
    }

    @Test
    public void testFallbackDescriptionWhenNoSummary() {
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> byName = commandsByName();

        // getThing has no summary in test-openapi.json
        assertEquals("Get thing", byName.get("get").getDescription());
    }

    @Test
    public void testSerializeDeserializeRoundtrip() throws Exception {
        KcAdmV2CommandDescriptor original = buildDescriptor();

        byte[] json = new ObjectMapper().writeValueAsBytes(original);
        KcAdmV2CommandDescriptor deserialized = KcAdmV2DescriptorBuilder.readDescriptor(
                new ByteArrayInputStream(json));

        assertEquals(original.getVersion(), deserialized.getVersion());
        assertEquals(original.getResources().size(), deserialized.getResources().size());
        assertEquals(
                original.getResources().get(0).getCommands().size(),
                deserialized.getResources().get(0).getCommands().size());

        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> originalByName = commandsByName(original);
        Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> deserializedByName = commandsByName(deserialized);
        for (String name : originalByName.keySet()) {
            assertEquals(originalByName.get(name).getHttpMethod(), deserializedByName.get(name).getHttpMethod());
            assertEquals(originalByName.get(name).isRequiresId(), deserializedByName.get(name).isRequiresId());
            assertEquals(originalByName.get(name).getDescription(), deserializedByName.get(name).getDescription());
        }
    }

    private static KcAdmV2CommandDescriptor buildDescriptor() {
        OpenAPI openApi = KcAdmV2DescriptorBuilder.parseOpenApi(
                () -> KcAdmV2DescriptorBuilderTest.class.getResourceAsStream(TEST_OPENAPI));
        return KcAdmV2DescriptorBuilder.convert(openApi);
    }

    private static Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> commandsByName() {
        return commandsByName(buildDescriptor());
    }

    private static Map<String, KcAdmV2CommandDescriptor.CommandDescriptor> commandsByName(
            KcAdmV2CommandDescriptor descriptor) {
        return descriptor.getResources().get(0).getCommands().stream()
                .collect(Collectors.toMap(KcAdmV2CommandDescriptor.CommandDescriptor::getName, c -> c));
    }
}
