/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

import org.jboss.logging.Logger;
import org.junit.Assume;

import static org.keycloak.testsuite.arquillian.AppServerTestEnricher.APP_SERVER_SSL_REQUIRED;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

public class ContainerAssume {

    private static final Logger log = Logger.getLogger(ContainerAssume.class);

    public static void assumeNotAuthServerUndertow() {
        Assume.assumeFalse("Doesn't work on auth-server-undertow", 
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT));
    }
    public static void assumeAuthServerUndertow() {
        Assume.assumeTrue("Only works on auth-server-undertow",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.equals(AuthServerTestEnricher.AUTH_SERVER_CONTAINER_DEFAULT));
    }

    public static void assumeClusteredContainer() {
        Assume.assumeTrue(
              String.format("Ignoring test since %s is set to false",
                    AuthServerTestEnricher.AUTH_SERVER_CLUSTER_PROPERTY), AuthServerTestEnricher.AUTH_SERVER_CLUSTER);
    }

    public static void assumeAuthServerSSL() {
        Assume.assumeTrue("Only works with the SSL configured", AUTH_SERVER_SSL_REQUIRED);
    }

    public static void assumeAppServerSSL() {
        Assume.assumeTrue("Only works with the SSL configured for app server", APP_SERVER_SSL_REQUIRED);
    }

    public static void assumeNotAppServerSSL() {
        Assume.assumeFalse("Only works with the SSL disabled for app server", APP_SERVER_SSL_REQUIRED);
    }

    public static void assumeNotAuthServerQuarkus() {
        Assume.assumeFalse("Doesn't work on auth-server-quarkus",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.startsWith("auth-server-quarkus"));
    }

    public static void assumeAuthServerQuarkus() {
        Assume.assumeTrue("Only works on auth-server-quarkus",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.startsWith("auth-server-quarkus"));
    }

    public static void assumeNotAuthServerQuarkusCluster() {
        Assume.assumeTrue("Doesn't work on auth-server-cluster-quarkus",
                AuthServerTestEnricher.AUTH_SERVER_CONTAINER.startsWith("auth-server-cluster-quarkus"));
    }
}
