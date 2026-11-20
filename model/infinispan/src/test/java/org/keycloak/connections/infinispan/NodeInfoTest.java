package org.keycloak.connections.infinispan;

import java.io.IOException;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Assert;
import org.junit.Test;

public class NodeInfoTest {

    @Test
    public void testConfiguredNodeName() throws IOException {
        Assert.assertEquals("my-node", nodeNameFor("my-node"));
    }

    @Test
    public void testEmptyNodeNameIsTreatedAsAbsent() throws IOException {
        assertGenerated(nodeNameFor(""));
    }

    @Test
    public void testBlankNodeNameIsTreatedAsAbsent() throws IOException {
        assertGenerated(nodeNameFor("  "));
    }

    private static void assertGenerated(String nodeName) {
        Assert.assertFalse(nodeName.isBlank());
        Assert.assertTrue(nodeName.startsWith(InfinispanConnectionProvider.NODE_PREFIX));
    }

    private static String nodeNameFor(String configuredName) throws IOException {
        GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
        global.transport().nodeName(configuredName);
        try (DefaultCacheManager cacheManager = new DefaultCacheManager(global.build())) {
            return NodeInfo.of(cacheManager).nodeName();
        }
    }
}
