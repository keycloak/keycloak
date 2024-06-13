/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class DatabaseSpec {

    @JsonPropertyDescription("The database vendor.")
    private String vendor;

    @JsonPropertyDescription("The reference to a secret holding the username of the database user.")
    private SecretKeySelector usernameSecret;

    @JsonPropertyDescription("The reference to a secret holding the password of the database user.")
    private SecretKeySelector passwordSecret;

    @JsonPropertyDescription("Sets the database name of the default JDBC URL of the chosen vendor. If the `url` option is set, this option is ignored.")
    private String database;

    @JsonPropertyDescription("Sets the hostname of the default JDBC URL of the chosen vendor. If the `url` option is set, this option is ignored.")
    private String host;

    @JsonPropertyDescription("Sets the port of the default JDBC URL of the chosen vendor. If the `url` option is set, this option is ignored.")
    private Integer port;

    @JsonPropertyDescription("The database schema to be used.")
    private String schema;

    @JsonPropertyDescription("The full database JDBC URL. If not provided, a default URL is set based on the selected database vendor. " +
            "For instance, if using 'postgres', the default JDBC URL would be 'jdbc:postgresql://localhost/keycloak'. ")
    private String url;

    @JsonPropertyDescription("The initial size of the connection pool.")
    private Integer poolInitialSize;

    @JsonPropertyDescription("The minimal size of the connection pool.")
    private Integer poolMinSize;

    @JsonPropertyDescription("The maximum size of the connection pool.")
    private Integer poolMaxSize;

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public SecretKeySelector getUsernameSecret() {
        return usernameSecret;
    }

    public void setUsernameSecret(SecretKeySelector usernameSecret) {
        this.usernameSecret = usernameSecret;
    }

    public SecretKeySelector getPasswordSecret() {
        return passwordSecret;
    }

    public void setPasswordSecret(SecretKeySelector passwordSecret) {
        this.passwordSecret = passwordSecret;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getPoolInitialSize() {
        return poolInitialSize;
    }

    public void setPoolInitialSize(Integer poolInitialSize) {
        this.poolInitialSize = poolInitialSize;
    }

    public Integer getPoolMinSize() {
        return poolMinSize;
    }

    public void setPoolMinSize(Integer poolMinSize) {
        this.poolMinSize = poolMinSize;
    }

    public Integer getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(Integer poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }
}
