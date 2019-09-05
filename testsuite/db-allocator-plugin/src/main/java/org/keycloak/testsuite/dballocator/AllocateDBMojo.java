package org.keycloak.testsuite.dballocator;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.keycloak.testsuite.dballocator.client.data.AllocationResult;
import org.keycloak.testsuite.dballocator.client.DBAllocatorServiceClient;
import org.keycloak.testsuite.dballocator.client.data.EraseResult;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorException;
import org.keycloak.testsuite.dballocator.client.retry.IncrementalBackoffRetryPolicy;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Allocated a DB from DB Allocator Service.
 */
@Mojo(name = "allocate", defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES)
public class AllocateDBMojo extends AbstractMojo {


    private final Log logger = getLog();

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    List<MavenProject> reactorProjects;

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
     * Username used for allocating DBs.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_USER)
    private String user;

    /**
     * Fallback username used for allocating DBs.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_USER_FALLBACK)
    private String fallbackUser;

    /**
     * Type of the database to be used.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_DATABASE_TYPE)
    private String type;

    /**
     * Expiration in minutes.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_EXPIRATION_MIN, defaultValue = "1440")
    private int expirationInMinutes;

    /**
     * Preferred DB location.
     */
    @Parameter(property = Constants.PROPERTY_DB_ALLOCATOR_LOCATION, defaultValue = "geo_RDU")
    private String location;

    /**
     * A property set as an output of this Mojo for JDBC Driver.
     */
    @Parameter(property = Constants.PROPERTY_TO_BE_SET_DRIVER, defaultValue = "keycloak.connectionsJpa.driver")
    private String propertyDriver;

    /**
     * A property set as an output of this Mojo for Database Schema.
     */
    @Parameter(property = Constants.PROPERTY_TO_BE_SET_DATABASE, defaultValue = "keycloak.connectionsJpa.database")
    private String propertyDatabase;

    /**
     * A property set as an output of this Mojo for DB Username.
     */
    @Parameter(property = Constants.PROPERTY_TO_BE_SET_USER, defaultValue = "keycloak.connectionsJpa.user")
    private String propertyUser;

    /**
     * A property set as an output of this Mojo for DB Password.
     */
    @Parameter(property = Constants.PROPERTY_TO_BE_SET_PASSWORD, defaultValue = "keycloak.connectionsJpa.password")
    private String propertyPassword;

    /**
     * A property set as an output of this Mojo for JDBC Connection URI.
     */
    @Parameter(property = Constants.PROPERTY_TO_BE_SET_JDBC_URL, defaultValue = "keycloak.connectionsJpa.url")
    private String propertyURL;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            logger.info("Skipping");
            return;
        }

        try {
            IncrementalBackoffRetryPolicy retryPolicy = new IncrementalBackoffRetryPolicy(totalRetries, backoffTimeSeconds, TimeUnit.SECONDS);
            DBAllocatorServiceClient client = new DBAllocatorServiceClient(dbAllocatorURI, retryPolicy);

            setFallbackUserIfNecessary();
            AllocationResult allocate = client.allocate(user, type, expirationInMinutes, TimeUnit.MINUTES, location);

            reactorProjects.forEach((project) -> setPropertiesToProject(project, allocate));

            if (printSummary) {
                logger.info("Allocated database:");
                logger.info("-- UUID: " + allocate.getUUID());
                logger.info("-- Driver: " + allocate.getDriver());
                logger.info("-- Database: " + allocate.getDatabase());
                logger.info("-- User: " + allocate.getUser());
                logger.info("-- Password: " + allocate.getPassword());
                logger.info("-- URL: " + allocate.getURL());
            }

            EraseResult eraseResult = client.erase(allocate);
            if (printSummary) {
                logger.info("Erased database:");
                logger.info("-- UUID: " + eraseResult.getUUID());
            }

        } catch (DBAllocatorException e) {
            String error = e.getMessage();
            if (e.getErrorResponse() != null) {
                error = String.format("[%s](%s)", e.getErrorResponse().getStatus(), e.getErrorResponse().readEntity(String.class));
            }
            throw new MojoFailureException("An error occurred while communicating with DBAllocator (" + error + ")", e);
        }
    }

    private void setFallbackUserIfNecessary() {
        if (StringUtils.isBlank(user)) {
            if (StringUtils.isBlank(fallbackUser)) {
                throw new IllegalArgumentException("Both " + Constants.PROPERTY_DB_ALLOCATOR_USER + " and " + Constants.PROPERTY_DB_ALLOCATOR_USER_FALLBACK + " are empty");
            }
            user = fallbackUser;
        }
    }

    private void setPropertiesToProject(MavenProject project, AllocationResult allocate) {
        project.getProperties().setProperty(propertyDriver, allocate.getDriver());
        project.getProperties().setProperty(propertyDatabase, allocate.getDatabase());
        project.getProperties().setProperty(propertyUser, allocate.getUser());
        project.getProperties().setProperty(propertyPassword, allocate.getPassword());
        project.getProperties().setProperty(propertyURL, allocate.getURL());
        project.getProperties().setProperty(Constants.PROPERTY_ALLOCATED_DB, allocate.getUUID());
    }
}
