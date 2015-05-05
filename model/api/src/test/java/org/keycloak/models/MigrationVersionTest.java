package org.keycloak.models;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.migration.ModelVersion;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MigrationVersionTest {

    @Test
    public void testVersion() {
        ModelVersion version_100Beta1 = new ModelVersion("1.0.0.Beta1");
        Assert.assertEquals(version_100Beta1.getMajor(), 1);
        Assert.assertEquals(version_100Beta1.getMinor(), 0);
        Assert.assertEquals(version_100Beta1.getMicro(), 0);
        ModelVersion version_100RC1 = new ModelVersion("1.0.0.RC1");
        ModelVersion version_100 = new ModelVersion("1.0.0");
        ModelVersion version_110Beta1 = new ModelVersion("1.1.0.Beta1");
        ModelVersion version_110RC1 = new ModelVersion("1.1.0.RC1");
        ModelVersion version_110 = new ModelVersion("1.1.0");
        ModelVersion version_111Beta1 = new ModelVersion("1.1.1.Beta1");
        ModelVersion version_111RC1 = new ModelVersion("1.1.1.RC1");
        ModelVersion version_111 = new ModelVersion("1.1.1");
        ModelVersion version_211Beta1 = new ModelVersion("2.1.1.Beta1");
        ModelVersion version_211RC1 = new ModelVersion("2.1.1.RC1");
        Assert.assertEquals(version_211RC1.getMajor(), 2);
        Assert.assertEquals(version_211RC1.getMinor(), 1);
        Assert.assertEquals(version_211RC1.getMicro(), 1);
        Assert.assertEquals(version_211RC1.getQualifier(), "RC1");
        ModelVersion version_211 = new ModelVersion("2.1.1");

        Assert.assertFalse(version_100Beta1.lessThan(version_100Beta1));
        Assert.assertTrue(version_100Beta1.lessThan(version_100RC1));
        Assert.assertTrue(version_100Beta1.lessThan(version_100));
        Assert.assertTrue(version_100Beta1.lessThan(version_110Beta1));
        Assert.assertTrue(version_100Beta1.lessThan(version_110RC1));
        Assert.assertTrue(version_100Beta1.lessThan(version_110));

        Assert.assertFalse(version_211.lessThan(version_110RC1));

    }
}
