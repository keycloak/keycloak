package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;

/**
 * @author <a href="mailto:abkaplan07@gmail.com">Adam Kaplan</a>
 */
public class MigrateTo3_1_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("3.1.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms().getRealms().forEach( realm -> {
            PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
            if (passwordPolicy.getPolicyConfig(PasswordPolicy.HASH_ALGORITHM_ID) == null) {
                passwordPolicy.getPolicies().add("hashAlgorithm(pbkdf2)");
            }
            if (passwordPolicy.getPolicyConfig(PasswordPolicy.HASH_ITERATIONS_ID) == null) {
                passwordPolicy.getPolicies().add("hashIterations(20000)");
            }
        });
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
