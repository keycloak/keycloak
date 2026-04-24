package org.keycloak.testframework.realm;

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

}
