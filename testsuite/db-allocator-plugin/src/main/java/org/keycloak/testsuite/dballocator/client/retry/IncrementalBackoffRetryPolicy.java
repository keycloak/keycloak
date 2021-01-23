package org.keycloak.testsuite.dballocator.client.retry;

import org.keycloak.testsuite.dballocator.client.BackoffRetryPolicy;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorUnavailableException;

import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.jboss.logging.Logger;

public class IncrementalBackoffRetryPolicy implements BackoffRetryPolicy {

    public static final int DEFAULT_TOTAL_RETRIES = 3;
    public static final int DEFAULT_BACKOFF_TIME = 10;
    public static final TimeUnit DEFAULT_BACKOFF_TIME_UNIT = TimeUnit.SECONDS;

    private final int totalRetries;
    private final int backoffTime;
    private final TimeUnit backoffTimeUnit;
    private final Logger logger = Logger.getLogger(IncrementalBackoffRetryPolicy.class);

    public IncrementalBackoffRetryPolicy() {
        this(DEFAULT_TOTAL_RETRIES, DEFAULT_BACKOFF_TIME, DEFAULT_BACKOFF_TIME_UNIT);
    }

    public IncrementalBackoffRetryPolicy(int totalRetries, int backoffTime, TimeUnit backoffTimeUnit) {
        this.backoffTime = backoffTime;
        this.backoffTimeUnit = backoffTimeUnit;
        this.totalRetries = totalRetries;
    }

    @Override
    public Response retryTillHttpOk(Callable<Response> callableSupplier) throws DBAllocatorUnavailableException {
        return retryTillHttpOk(callableSupplier, totalRetries, backoffTime, backoffTimeUnit);
    }

    private Response retryTillHttpOk(Callable<Response> callableSupplier, int totalRetries, int backoffTime, TimeUnit backoffTimeUnit) throws DBAllocatorUnavailableException {
        int retryCount = 0;
        Response response;
        while(true) {
            try {
                response = callableSupplier.call();
            } catch (Exception e) {
                response = null;
            }

            if (response != null) {
                logger.info("Response status: " + response.getStatus());
                if  (response.getStatus() == 200) {
                    return response;
                }
            }
            
            logger.info("retryCount: " + (retryCount + 1) + ", totalRetries: " + totalRetries);
            if (++retryCount > totalRetries) {
                logger.info("retryCount exceeded: " + retryCount);
                throw new DBAllocatorUnavailableException(response);
            }

            logger.info("backoffTime * retryCount: " + backoffTime * retryCount);
            LockSupport.parkNanos(backoffTimeUnit.toNanos(backoffTime * retryCount));
        }
    }
}
