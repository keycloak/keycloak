package org.keycloak.testsuite.dballocator;

public interface Constants {
    String PROPERTY_ALLOCATED_DB = "dballocator.allocated.uuid";
    String PROPERTY_DB_ALLOCATOR_URI = "dballocator.uri";
    String PROPERTY_DB_ALLOCATOR_USER = "dballocator.user";
    String PROPERTY_DB_ALLOCATOR_USER_FALLBACK = "user.name";
    String PROPERTY_DB_ALLOCATOR_DATABASE_TYPE = "dballocator.type";
    String PROPERTY_DB_ALLOCATOR_EXPIRATION_MIN = "dballocator.expirationMin";
    String PROPERTY_DB_ALLOCATOR_LOCATION = "dballocator.location";
    String PROPERTY_TO_BE_SET_DRIVER = "dballocator.properties.driver";
    String PROPERTY_TO_BE_SET_DATABASE = "dballocator.properties.database";
    String PROPERTY_TO_BE_SET_USER = "dballocator.properties.user";
    String PROPERTY_TO_BE_SET_PASSWORD = "dballocator.properties.password";
    String PROPERTY_TO_BE_SET_JDBC_URL = "dballocator.properties.url";
    String PROPERTY_PRINT_SUMMARY = "dballocator.summary";
    String PROPERTY_SKIP = "dballocator.skip";
    String PROPERTY_RETRY_TOTAL_RETRIES = "dballocator.retry.totalRetries";
    String PROPERTY_RETRY_BACKOFF_SECONDS = "dballocator.retry.backoffSeconds";
}
