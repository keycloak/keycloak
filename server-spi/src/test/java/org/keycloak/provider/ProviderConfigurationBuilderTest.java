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
}
