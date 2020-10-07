package org.keycloak.testsuite.dballocator.client.retry;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorUnavailableException;
import org.keycloak.testsuite.dballocator.client.retry.IncrementalBackoffRetryPolicy;

import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;


public class IncrementalBackoffRetryPolicyTest {

    static class BackoffCounter implements Callable<Response> {

        LongAdder adder = new LongAdder();
        Response responseToReport;

        public BackoffCounter(Response responseToReport) {
            this.responseToReport = responseToReport;
        }

        @Override
        public Response call() throws Exception {
            adder.add(1);
            return responseToReport;
        }

        public Long getCounter() {
            return adder.longValue();
        }
    }

    @Test
    public void testBackoffLoop() {
        //given
        long expectedNumberOfRetries = 2;
        long expectedNumberOfInvocations = expectedNumberOfRetries + 1;
        BackoffCounter counter = new BackoffCounter(Response.serverError().build());
        IncrementalBackoffRetryPolicy backoffRetryPolicy = new IncrementalBackoffRetryPolicy((int) expectedNumberOfRetries, 0, TimeUnit.NANOSECONDS);

        //when
        try {
            backoffRetryPolicy.retryTillHttpOk(counter);
            Assert.fail();
        } catch (DBAllocatorUnavailableException e) {
            //then
            Assert.assertEquals(expectedNumberOfInvocations, counter.getCounter().longValue());
        }
    }

    @Test
    public void testIgnoringBackoffWhenGettingSuccessfulResponse() throws Exception {
        //given
        BackoffCounter counter = new BackoffCounter(Response.ok().build());
        IncrementalBackoffRetryPolicy backoffRetryPolicy = new IncrementalBackoffRetryPolicy(3, 0, TimeUnit.NANOSECONDS);

        //when
        Response response = backoffRetryPolicy.retryTillHttpOk(counter);

        //then
        Assert.assertEquals(1, counter.getCounter().longValue());
        Assert.assertEquals(200, response.getStatus());
    }

}