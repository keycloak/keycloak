package org.keycloak.testsuite.dballocator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.keycloak.testsuite.dballocator.client.data.AllocationResult;
import org.keycloak.testsuite.dballocator.client.DBAllocatorServiceClient;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorException;
import org.keycloak.testsuite.dballocator.client.retry.IncrementalBackoffRetryPolicy;
import org.keycloak.testsuite.dballocator.client.data.ReleaseResult;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Releases a DB from DB Allocator Service.
 */
@Mojo(name = "release", defaultPhase = LifecyclePhase.TEST)
public class ReleaseDBMojo extends AbstractMojo {

    private final Log logger = getLog();

    @Parameter(defaultValue = "${project}", required = true)
    protected MavenProject project;

    /**
     * Enables printing out a summary after execution.
     */
    @Parameter(property = Constants.PROPERTY_PRINT_SUMMARY, defaultValue = "true")
    private boolean printSummary;

    /**
     * Skips the execution of this Mojo.
     */
    @Parameter(property = Constants.PROPERTY_SKIP, defaultValue = "false")
    private boolean skip;

    /**
     * The number of retries for reaching the DB Allocator Service
     */
    @Parameter(property = Constants.PROPERTY_RETRY_TOTAL_RETRIES, defaultValue = "3")
    private int totalRetries;

    /**
     * Backoff time for reaching out the DB Allocator Service.
     */
    @Parameter(property = Constants.PROPERTY_RETRY_BACKOFF_SECONDS, defaultValue = "10")
    private int backoffTimeSeconds;

    /**
     * URI to the DB Allocator Service.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_URI)
    private String dbAllocatorURI;

    /**
     * UUID for releasing the allocated DB.
     */
    @Parameter(property = Constants.PROPERTY_ALLOCATED_DB)
    private String allocatedUUID;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            logger.info("Skipping");
            return;
        }

        logger.info("Total retries: " + totalRetries + "; backOffTime: " + backoffTimeSeconds);
        try {
            IncrementalBackoffRetryPolicy retryPolicy = new IncrementalBackoffRetryPolicy(totalRetries, backoffTimeSeconds, TimeUnit.SECONDS);
            DBAllocatorServiceClient client = new DBAllocatorServiceClient(dbAllocatorURI, retryPolicy);

            ReleaseResult release = client.release(AllocationResult.forRelease(allocatedUUID));

            if (printSummary) {
                logger.info("Released database:");
                logger.info("-- UUID: " + release.getUUID());
            }

        } catch (DBAllocatorException e) {
            String error = e.getMessage();
            if (e.getErrorResponse() != null) {
                error = String.format("[%s](%s)", e.getErrorResponse().getStatus(), e.getErrorResponse().readEntity(String.class));
            }
            throw new MojoFailureException("An error occurred while communicating with DBAllocator (" + error + ")", e);
        }
    }
}
