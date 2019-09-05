package org.keycloak.testsuite.dballocator.client;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.dballocator.client.data.AllocationResult;
import org.keycloak.testsuite.dballocator.client.data.EraseResult;
import org.keycloak.testsuite.dballocator.client.data.ReleaseResult;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorException;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorUnavailableException;
import org.keycloak.testsuite.dballocator.client.mock.MockResponse;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;


public class DBAllocatorServiceClientTest {

    @Test
    public void testSuccessfulAllocation() throws Exception {
        //given
        String mockURI = "http://localhost:8080/test";

        String testProperties = null;
        try(InputStream is = DBAllocatorServiceClientTest.class.getResourceAsStream("/db-allocator-response.properties")) {
            testProperties = IOUtils.toString(is, Charset.defaultCharset());
        }

        Response successfulResponse = new MockResponse(200, testProperties);
        BackoffRetryPolicy retryPolicyMock = callableSupplier -> successfulResponse;

        DBAllocatorServiceClient client = new DBAllocatorServiceClient(mockURI, retryPolicyMock);

        //when
        AllocationResult allocationResult = client.allocate("user", "mariadb_galera_101", 1440, TimeUnit.SECONDS, "geo_RDU");

        //then
        Assert.assertEquals("d328bb0e-3dcc-42da-8ce1-83738a8dfede", allocationResult.getUUID());
        Assert.assertEquals("org.mariadb.jdbc.Driver", allocationResult.getDriver());
        Assert.assertEquals("dbname", allocationResult.getDatabase());
        Assert.assertEquals("username", allocationResult.getUser());
        Assert.assertEquals("password", allocationResult.getPassword());
        Assert.assertEquals("jdbc:mariadb://mariadb-101-galera.keycloak.org:3306", allocationResult.getURL());

        EraseResult erase = client.erase(allocationResult);
        ReleaseResult result = client.release(allocationResult);
    }

    @Test
    public void testFailureAllocation() throws Exception {
        //given
        String mockURI = "http://localhost:8080/test";

        Response serverErrorResponse = new MockResponse(500, null);
        BackoffRetryPolicy retryPolicyMock = callableSupplier -> {
            throw new DBAllocatorUnavailableException(serverErrorResponse);
        };

        DBAllocatorServiceClient client = new DBAllocatorServiceClient(mockURI, retryPolicyMock);

        //when
        try {
            client.allocate("user", "mariadb_galera_101", 1440, TimeUnit.SECONDS, "geo_RDU");
            Assert.fail();
        } catch (DBAllocatorException e) {
            Assert.assertEquals(500, e.getErrorResponse().getStatus());
        }
    }
}