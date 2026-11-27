package org.keycloak.storage.ldap;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LDAPConfigTest {

    private static LDAPConfig configWithBatchSize(String value) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        if (value != null) {
            map.putSingle(LDAPConstants.BATCH_SIZE_FOR_SYNC, value);
        }
        return new LDAPConfig(map);
    }

    @Test
    public void zeroBatchSizeFallsBackToDefault() {
        assertEquals(LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC,
                configWithBatchSize("0").getBatchSizeForSync());
    }

    @Test
    public void negativeBatchSizeFallsBackToDefault() {
        assertEquals(LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC,
                configWithBatchSize("-5").getBatchSizeForSync());
    }

    @Test
    public void unsetBatchSizeUsesDefault() {
        assertEquals(LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC,
                configWithBatchSize(null).getBatchSizeForSync());
    }

    @Test
    public void positiveBatchSizeIsKept() {
        assertEquals(500, configWithBatchSize("500").getBatchSizeForSync());
    }
}
