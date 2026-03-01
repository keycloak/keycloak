package org.keycloak.testframework.injection;

import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.config.SuiteConfigSource;
import org.keycloak.testframework.server.KeycloakServerConfig;

public class SuiteSupport {

    private static SuiteConfig suiteConfig = new SuiteConfig();

    public static SuiteConfig startSuite() {
        if (suiteConfig == null) {
            suiteConfig = new SuiteConfig();
        }
        return suiteConfig;
    }

    public static void stopSuite() {
        SuiteConfigSource.clear();
        Config.initConfig();
        Extensions.reset();
        suiteConfig = null;
    }

    public static class SuiteConfig {

        public SuiteConfig registerServerConfig(Class<? extends KeycloakServerConfig> serverConfig) {
            registerSupplierConfig("server", serverConfig);
            return this;
        }

        public SuiteConfig registerSupplierConfig(String supplierValueType, Class<?> supplierConfig) {
            SuiteConfigSource.set("kc.test." + supplierValueType + ".config", supplierConfig.getName());
            return this;
        }

        public SuiteConfig registerSupplierConfig(String supplierValueType, String supplierConfigKey, String supplierConfigValue) {
            SuiteConfigSource.set("kc.test." + supplierValueType + "." + supplierConfigKey, supplierConfigValue);
            return this;
        }

        public SuiteConfig supplier(String name, String supplier) {
            SuiteConfigSource.set("kc.test." + name, supplier);
            return this;
        }

        public SuiteConfig includedSuppliers(String name, String... suppliers) {
            SuiteConfigSource.set("kc.test." + name + ".suppliers.included", String.join(",", suppliers));
            return this;
        }

        public SuiteConfig excludedSuppliers(String name, String... suppliers) {
            SuiteConfigSource.set("kc.test." + name + ".suppliers.excluded", String.join(",", suppliers));
            return this;
        }

    }

}
