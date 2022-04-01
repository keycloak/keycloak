package org.keycloak.quarkus.runtime.configuration.test;


import org.junit.Test;
import org.keycloak.quarkus.runtime.configuration.QuarkusPropertiesConfigSource;

import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigRegExPatternMatchingTest {

    @Test
    public void quarkusPropertyMultipleDatasourcePatternTest(){
        Pattern p = Pattern.compile(QuarkusPropertiesConfigSource.QUARKUS_DATASOURCE_BUILDTIME_REGEX);
        assertTrue(p.matcher("quarkus.datasource.user.jdbc.transactions").matches());
        assertTrue(p.matcher("quarkus.datasource.user-store.jdbc.enable-metrics").matches());
        assertTrue(p.matcher("quarkus.datasource.user12_store.db-kind").matches());
        assertTrue(p.matcher("quarkus.datasource.user12_-__--store.db-kind").matches());
        assertFalse(p.matcher("quarkus.datasource.user-store.db-username").matches());
        assertFalse(p.matcher("quarkus.datasource.user-store.db-kin").matches());
    }
}
