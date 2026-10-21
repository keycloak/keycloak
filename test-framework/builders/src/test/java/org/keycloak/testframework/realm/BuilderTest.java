package org.keycloak.testframework.realm;

import java.util.List;

import org.keycloak.representations.idm.GroupRepresentation;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BuilderTest {

    @Test
    public void arrayCombine() {
        String[] combined = Builder.combine((String[]) null, new String[] { "baz" });
        Assertions.assertArrayEquals(new String[] { "baz"}, combined);

        combined = Builder.combine(new String[] { "foo", "bar" }, new String[] { "baz" });
        Assertions.assertArrayEquals(new String[] { "foo", "bar", "baz"}, combined);
    }

    @Test
    public void builderCombine() {
        List<String> subGroups = GroupBuilder.create().subGroups("foo").subGroups("bar").build().getSubGroups().stream().map(GroupRepresentation::getName).toList();
        MatcherAssert.assertThat(subGroups, CoreMatchers.hasItems("foo", "bar"));
    }

}
