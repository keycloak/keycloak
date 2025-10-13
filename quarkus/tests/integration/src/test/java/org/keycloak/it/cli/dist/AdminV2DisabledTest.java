package org.keycloak.it.cli.dist;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@DistributionTest(keepAlive = true,
        containerExposedPorts = {8080, 9000})
public class AdminV2DisabledTest {
    public static final String TEST_URL = "/admin/api/v2/realms/master/clients/account";

    @Test
    @DryRun
    @Launch({ "start-dev", "--openapi-enabled=true"})
    public void testOpenApiRequiresClientAdminApiV2Feature(CLIResult cliResult) {
        cliResult.assertError("Disabled option: '--openapi-enabled'. Available only when Client Admin API v2 Feature is enabled");
    }

    @Test
    @Launch({"start-dev"})
    public void testApiUnavailableWhenClientAdminApiV2FeatureDisabled() {
        when().get(TEST_URL)
                .then().statusCode(404);
    }

    @Test
    @Launch({"start-dev", "--features=client-admin-api:v2"})
    public void testApiAvailableWhenClientAdminApiV2FeatureEnabled() {
        when().get(TEST_URL)
                .then().statusCode(not(equalTo(404)));
    }
}
