package org.keycloak.it.cli.dist;

import java.util.List;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.junit5.extension.TestProvider;

import com.acme.provider.tenantunit.TenantUnitTestProvider;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A multi-tenant persistence unit defined through {@code db-jpa-packages-<datasource>} and
 * {@code db-orm-multitenant-<datasource>}, see {@code com.acme.provider.tenantunit}.
 */
@DistributionTest(stopServer = Mode.MANUAL)
@RawDistOnly(reason = "Containers are immutable")
@TestProvider(TenantUnitTestProvider.class)
public class ConfigDefinedMultiTenancyDistTest {

    private static final String BASE = "/realms/master/tenant-unit";
    // the Hibernate ORM options cannot be set on the command line
    private static final String MULTITENANT_ENV_VAR = "KC_DB_ORM_MULTITENANT_TENANT_STORE";
    private static final String[] BUILD = { "build", "--db=dev-file", "--db-kind-tenant-store=dev-mem", "--db-jpa-packages-tenant-store=com.acme.provider.tenantunit",
            "--db-kind-tenant-a=dev-mem", "--db-kind-tenant-b=dev-mem" };
    // the tenant datasources have no schema migration, so the table of the entity is created with the connection
    private static final String TENANT_URL = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;INIT=CREATE TABLE IF NOT EXISTS TENANT_UNIT_ENTITY(ID VARCHAR(255) PRIMARY KEY)";

    @Test
    void databaseMultiTenancyRoutesToTheTenantDatasources(KeycloakRunner runner) {
        runner.setEnvVar(MULTITENANT_ENV_VAR, "database");
        CLIResult result = runner.run(BUILD);
        result.assertBuild();

        // with the DATABASE strategy, the tenants are datasources of their own and the datasource of the unit is not used:
        // the unit must start although that datasource is disabled
        result = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false", "--db-enabled-tenant-store=false",
                "--db-url-full-tenant-a=" + TENANT_URL.formatted("tenant-a"), "--db-username-tenant-a=sa", "--db-password-tenant-a=sa",
                "--db-url-full-tenant-b=" + TENANT_URL.formatted("tenant-b"), "--db-username-tenant-b=sa", "--db-password-tenant-b=sa");
        result.assertStarted();
        result.assertNoStartupMessage("persistence unit is skipped");
        result.assertNoStartupMessage("Could not find a suitable persistence unit for model classes/packages");

        when().post(BASE + "/tenant-a/a1").then().statusCode(204);
        when().post(BASE + "/tenant-a/a2").then().statusCode(204);
        when().post(BASE + "/tenant-b/b1").then().statusCode(204);
        assertEquals(List.of("a1", "a2"), listIds("tenant-a"));
        assertEquals(List.of("b1"), listIds("tenant-b"));
        runner.stop();
    }

    @Test
    void multiTenancyStrategyIsValidated(KeycloakRunner runner) {
        runner.setEnvVar(MULTITENANT_ENV_VAR, "bogus");
        CLIResult result = runner.run(BUILD);
        result.assertError("Invalid value for option 'KC_DB_ORM_MULTITENANT_TENANT_STORE': bogus. Expected values are (case insensitive): none, database, schema, discriminator");
    }

    private static List<String> listIds(String tenant) {
        return when().get(BASE + "/" + tenant).then().statusCode(200).extract().jsonPath().getList("", String.class);
    }
}
