package org.keycloak.connections.truststore;

public enum HostnameVerificationPolicy {

    /**
     * Hostname verification is not done on the server's certificate
     */
    ANY,

    /**
     * Allows wildcards in subdomain names i.e. *.foo.com
     */
    WILDCARD,

    /**
     * CN must match hostname connecting to
     */
    STRICT
}