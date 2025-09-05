package org.keycloak.testframework.cache;

public enum CacheType {

    // Local Infinispan Cache for Embedded Deployment only
    LOCAL,

    // Clustered Infinispan Cache can be set for Embedded or External Deployment
    ISPN;
}
