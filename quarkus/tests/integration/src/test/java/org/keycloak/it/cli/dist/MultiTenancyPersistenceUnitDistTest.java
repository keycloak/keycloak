package org.keycloak.it.cli.dist;

import java.util.List;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.StopServer;
import org.keycloak.it.junit5.extension.TestProvider;

import com.acme.provider.multitenancy.MultiTenancyTestProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DistributionTest(stopServer = StopServer.Mode.MANUAL)
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
@TestProvider(MultiTenancyTestProvider.class)
public class MultiTenancyPersistenceUnitDistTest {

    private static final String BASE = "/realms/master/multi-tenancy";

    @Test
    @Launch({ "start-dev", "--db=dev-file",
            "--db-kind-database-store=dev-mem", "--db-kind-schema-store=dev-mem",
            "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void databaseMultiTenancyRoutesToTenantDatasource() {
        when().post(BASE + "/database/mem-tenant/a").then().statusCode(204);
        when().post(BASE + "/database/mem-tenant/b").then().statusCode(204);

        assertEquals(List.of("a", "b"), listIds("database", "mem-tenant"));
    }

    @Test
    @Launch({ "start-dev", "--db=dev-file",
            "--db-kind-database-store=dev-mem", "--db-enabled-database-store=false",
            "--db-kind-schema-store=dev-mem", "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void databaseMultiTenancyWorksWhenDeclaredDatasourceDisabled() {
        when().post(BASE + "/database/mem-tenant/a").then().statusCode(204);

        assertEquals(List.of("a"), listIds("database", "mem-tenant"));
    }

    @Test
    @Launch({ "start-dev", "--db=dev-file",
            "--db-kind-database-store=dev-mem", "--db-kind-schema-store=dev-mem",
            "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void schemaMultiTenancyRoutesToSchema(CLIResult result) {
        result.assertMessage("Persistence unit 'schema-store' sets tenant schemas on connections shared with the server");
        when().post(BASE + "/schema/PUBLIC/s").then().statusCode(204);

        assertEquals(List.of("s"), listIds("schema", "PUBLIC"));
    }

    @Test
    @Launch({ "start-dev", "--db=dev-file", "--db-schema=PUBLIC",
            "--db-kind-database-store=dev-mem", "--db-kind-schema-store=dev-mem",
            "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void schemaTenantDoesNotAffectServerQueriesWhenDbSchemaSet(CLIResult result) {
        result.assertNoStartupMessage("sets tenant schemas on connections shared with the server");
        // INFORMATION_SCHEMA holds none of the server tables, so an unqualified server query would fail
        when().get(BASE + "/schema/INFORMATION_SCHEMA/server-query").then().statusCode(200);
        when().get(BASE + "/schema/INFORMATION_SCHEMA/server-query").then().statusCode(200);
        when().post(BASE + "/schema/PUBLIC/s").then().statusCode(204);

        assertEquals(List.of("s"), listIds("schema", "PUBLIC"));
    }

    @Test
    @Launch({ "start-dev", "--db=dev-file",
            "--db-kind-database-store=dev-mem", "--db-kind-schema-store=dev-mem", "--db-enabled-schema-store=false",
            "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void schemaMultiTenancyWorksWhenDeclaredDatasourceDisabled() {
        when().post(BASE + "/schema/PUBLIC/s").then().statusCode(204);

        assertEquals(List.of("s"), listIds("schema", "PUBLIC"));
    }

    @Test
    @Launch({ "start-dev", "--db=dev-file",
            "--db-kind-database-store=dev-mem", "--db-kind-schema-store=dev-mem",
            "--db-kind-disc-store=dev-mem", "--db-kind-mem-tenant=dev-mem" })
    void discriminatorMultiTenancyIsolatesTenants() {
        when().post(BASE + "/discriminator/tenant-1/x").then().statusCode(204);
        when().post(BASE + "/discriminator/tenant-2/y").then().statusCode(204);

        assertEquals(List.of("x"), listIds("discriminator", "tenant-1"));
        assertEquals(List.of("y"), listIds("discriminator", "tenant-2"));
    }

    private static List<String> listIds(String unit, String tenant) {
        return when().get(BASE + "/" + unit + "/" + tenant).then().statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
