package org.keycloak.testframework.realm;

import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RealmBuilderTest {

    @Test
    public void builderCombineIdentityProviderMappers() {
        IdentityProviderMapperBuilder builder1 = IdentityProviderMapperBuilder.create()
                .attribute("TEST_ATTRIBUTE_A", "TEST_VALUE_A")
                .attribute("TEST_ATTRIBUTE_B", "TEST_VALUE_B")
                .identityProviderAlias("TEST_ALIAS")
                .identityProviderMapper("TEST_MAPPER")
                .name("TEST_NAME");

        RealmBuilder realmBuilder = RealmBuilder.create().identityProviderMappers(builder1);
        List<IdentityProviderMapperRepresentation> ipmrList = realmBuilder.build().getIdentityProviderMappers();
        Assertions.assertEquals(1, ipmrList.size());
        
        // Checks if the mapper has been build correctly
        IdentityProviderMapperRepresentation ipmr = ipmrList.stream().filter(i -> i.getName().equals("TEST_NAME")).findFirst().get();
        Assertions.assertEquals("TEST_NAME", ipmr.getName());
        Assertions.assertEquals("TEST_MAPPER", ipmr.getIdentityProviderMapper());
        Assertions.assertEquals("TEST_ALIAS", ipmr.getIdentityProviderAlias());
        Assertions.assertEquals(Map.of("TEST_ATTRIBUTE_A", "TEST_VALUE_A", "TEST_ATTRIBUTE_B", "TEST_VALUE_B"), ipmr.getConfig());

        IdentityProviderMapperBuilder builder2 = IdentityProviderMapperBuilder.create()
                .attribute("TEST_ATTRIBUTE_2", "TEST_VALUE_2")
                .identityProviderAlias("TEST_ALIAS_2")
                .identityProviderMapper("TEST_MAPPER_2")
                .name("TEST_NAME_2");
        realmBuilder.identityProviderMappers(builder2);

        ipmrList = realmBuilder.build().getIdentityProviderMappers();
        Assertions.assertEquals(2, ipmrList.size());
        
        // Checks if the first mapper is still there
        ipmr = ipmrList.stream().filter(i -> i.getName().equals("TEST_NAME")).findFirst().get();
        Assertions.assertEquals("TEST_NAME", ipmr.getName());
        Assertions.assertEquals("TEST_MAPPER", ipmr.getIdentityProviderMapper());
        Assertions.assertEquals("TEST_ALIAS", ipmr.getIdentityProviderAlias());
        Assertions.assertEquals(Map.of("TEST_ATTRIBUTE_A", "TEST_VALUE_A", "TEST_ATTRIBUTE_B", "TEST_VALUE_B"), ipmr.getConfig());

        // Checks if the second mapper has been added correctly
        ipmr = ipmrList.stream().filter(i -> i.getName().equals("TEST_NAME_2")).findFirst().get();
        Assertions.assertEquals("TEST_NAME_2", ipmr.getName());
        Assertions.assertEquals("TEST_MAPPER_2", ipmr.getIdentityProviderMapper());
        Assertions.assertEquals("TEST_ALIAS_2", ipmr.getIdentityProviderAlias());
        Assertions.assertEquals(Map.of("TEST_ATTRIBUTE_2", "TEST_VALUE_2"), ipmr.getConfig());

    }
}
