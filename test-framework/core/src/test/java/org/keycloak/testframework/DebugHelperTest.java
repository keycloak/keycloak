package org.keycloak.testframework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DebugHelperTest {

    @Test
    public void testInTest() throws NoSuchMethodException {
        Assertions.assertFalse(DebugHelper.isInTest());
        DebugHelper.testStarted(DebugHelperTest.class, getClass().getMethod("testInTest"));

        Assertions.assertTrue(DebugHelper.isInTest());

        Assertions.assertTrue(DebugHelper.isInTest("DebugHelperTest"));
        Assertions.assertFalse(DebugHelper.isInTest("OtherTest"));
        Assertions.assertTrue(DebugHelper.isInTest("org.keycloak.testframework.DebugHelperTest"));
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.testframework.OtherTest"));
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.othermethod.DebugHelperTest"));

        Assertions.assertTrue(DebugHelper.isInTest("#testInTest"));
        Assertions.assertFalse(DebugHelper.isInTest("#testInTest2"));

        Assertions.assertTrue(DebugHelper.isInTest("DebugHelperTest#testInTest"));
        Assertions.assertTrue(DebugHelper.isInTest("org.keycloak.testframework.DebugHelperTest#testInTest"));
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.testframework.OtherTest#testInTest"));
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.testframework.DebugHelperTest#testInTest2"));
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.other.DebugHelperTest#testInTest"));
        Assertions.assertFalse(DebugHelper.isInTest("OtherTest#testInTest"));

        DebugHelper.testFinished();

        Assertions.assertFalse(DebugHelper.isInTest());
        Assertions.assertFalse(DebugHelper.isInTest("org.keycloak.testframework.DebugHelperTest#testInTest"));
    }

}
