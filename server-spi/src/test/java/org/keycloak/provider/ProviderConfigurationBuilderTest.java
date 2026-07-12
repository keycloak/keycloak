package org.keycloak.provider;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ProviderConfigurationBuilderTest {

    @Test
    public void testAddProperty() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property()
            .name("property1")
            .label("Property 1")
            .helpText("Help text for property 1")
            .type("string")
            .defaultValue("default1")
            .add();

        builder.property()
            .name("property2")
            .label("Property 2")
            .helpText("Help text for property 2")
            .type("int")
            .defaultValue(10)
            .add();

        List<ProviderConfigProperty> properties = builder.build();

        Assert.assertEquals(2, properties.size());
        Assert.assertEquals("property1", properties.get(0).getName());
        Assert.assertEquals("Property 1", properties.get(0).getLabel());
        Assert.assertEquals("default1", properties.get(0).getDefaultValue());

        Assert.assertEquals("property2", properties.get(1).getName());
        Assert.assertEquals(10, properties.get(1).getDefaultValue());
    }

    @Test
    public void testDuplicatePropertyNameThrowsException() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        builder.property()
            .name("property1")
            .label("Property 1")
            .helpText("Help text for property 1")
            .type("string")
            .defaultValue("default1")
            .add();

        ProviderConfigPropertyNameNotUniqueException exception = Assert.assertThrows(
            ProviderConfigPropertyNameNotUniqueException.class,
            () -> builder.property()
                .name("property1")
                .label("Duplicate Property 1")
                .helpText("Help text for duplicate property 1")
                .type("string")
                .defaultValue("default2")
                .add()
        );

        Assert.assertEquals("ProviderConfigProperty with name 'property1' already exists.", exception.getMessage());
    }

    @Test
    public void testPropertyBuilderCanBeUsedStandalone() {
        ProviderConfigProperty property = new ProviderConfigurationBuilder.ProviderConfigPropertyBuilder()
            .name("standalone")
            .label("Standalone Property")
            .helpText("Built without a parent builder")
            .type("string")
            .defaultValue("value")
            .secret(true)
            .required(true)
            .build();

        Assert.assertEquals("standalone", property.getName());
        Assert.assertEquals("Standalone Property", property.getLabel());
        Assert.assertEquals("Built without a parent builder", property.getHelpText());
        Assert.assertEquals("string", property.getType());
        Assert.assertEquals("value", property.getDefaultValue());
        Assert.assertTrue(property.isSecret());
        Assert.assertTrue(property.isRequired());
    }

    @Test
    public void testStandaloneBuilderCannotAddWithoutParent() {
        ProviderConfigurationBuilder.ProviderConfigPropertyBuilder standalone =
            new ProviderConfigurationBuilder.ProviderConfigPropertyBuilder()
                .name("orphan")
                .label("Orphan Property");

        IllegalStateException exception = Assert.assertThrows(
            IllegalStateException.class,
            standalone::add
        );

        Assert.assertTrue(exception.getMessage().contains("build()"));
    }

    @Test
    public void testStandaloneBuiltPropertyCanLaterBeAddedToAParentBuilder() {
        ProviderConfigProperty property = new ProviderConfigurationBuilder.ProviderConfigPropertyBuilder()
            .name("added-later")
            .label("Added Later")
            .build();

        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();
        List<ProviderConfigProperty> properties = builder.property(property).build();

        Assert.assertEquals(1, properties.size());
        Assert.assertSame(property, properties.get(0));
    }

    @Test
    public void testPropertyReturnedByFactoryMethodAddsBackToSameParent() {
        ProviderConfigurationBuilder builder = ProviderConfigurationBuilder.create();

        ProviderConfigurationBuilder returned = builder.property()
            .name("chained")
            .label("Chained")
            .add();

        Assert.assertSame(builder, returned);
        Assert.assertEquals(1, builder.build().size());
    }
}
