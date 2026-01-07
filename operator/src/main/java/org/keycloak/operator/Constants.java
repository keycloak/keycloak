/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator;

import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Constants {
    public static final String CRDS_GROUP = "k8s.keycloak.org";
    public static final String CRDS_VERSION = "v2alpha1";
    public static final String SHORT_NAME = "kc";
    public static final String NAME = "keycloak";
    public static final String PLURAL_NAME = "keycloaks";
    public static final String INSTANCE_LABEL = "app.kubernetes.io/instance";
    public static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    public static final String MANAGED_BY_VALUE = "keycloak-operator";
    public static final String COMPONENT_LABEL = "app.kubernetes.io/component";
    public static final String KEYCLOAK_MIGRATING_ANNOTATION = "operator.keycloak.org/migrating";
    public static final String KEYCLOAK_RECREATE_UPDATE_ANNOTATION = "operator.keycloak.org/recreate-update";
    public static final String KEYCLOAK_UPDATE_REASON_ANNOTATION = "operator.keycloak.org/update-reason";
    public static final String KEYCLOAK_UPDATE_REVISION_ANNOTATION = "operator.keycloak.org/update-revision";
    public static final String KEYCLOAK_UPDATE_HASH_ANNOTATION = "operator.keycloak.org/update-hash";
    public static final String APP_LABEL = "app";

    public static final String DEFAULT_LABELS_AS_STRING = "app=keycloak,app.kubernetes.io/managed-by=keycloak-operator";

    public static final Map<String, String> DEFAULT_LABELS = Collections
            .unmodifiableMap(Stream.of(DEFAULT_LABELS_AS_STRING.split(",")).map(s -> s.split("="))
                    .collect(Collectors.toMap(e -> e[0], e -> e[1], (u1, u2) -> u1, TreeMap::new)));

    public static final List<ValueOrSecret> DEFAULT_DIST_CONFIG_LIST = List.of(
            new ValueOrSecret("health-enabled", "true"),
            new ValueOrSecret("cache", "ispn")
    );


    public static final Integer KEYCLOAK_HTTP_PORT = 8080;
    public static final Integer KEYCLOAK_HTTPS_PORT = 8443;
    public static final String KEYCLOAK_HTTP_PORT_NAME = "http";
    public static final String KEYCLOAK_HTTPS_PORT_NAME = "https";
    public static final String KEYCLOAK_SERVICE_PROTOCOL = "TCP";
    public static final String KEYCLOAK_SERVICE_SUFFIX = "-service";
    public static final Integer KEYCLOAK_DISCOVERY_SERVICE_PORT = 7800;
    public static final String KEYCLOAK_DISCOVERY_TCP_PORT_NAME = "tcp";
    public static final String KEYCLOAK_DISCOVERY_SERVICE_SUFFIX = "-discovery";
    public static final Integer KEYCLOAK_JGROUPS_DATA_PORT = 7800;
    public static final Integer KEYCLOAK_JGROUPS_FD_PORT = 57800;
    public static final String KEYCLOAK_JGROUPS_PROTOCOL = "TCP";
    public static final Integer KEYCLOAK_MANAGEMENT_PORT = 9000;
    public static final String KEYCLOAK_MANAGEMENT_PORT_NAME = "management";

    public static final String KEYCLOAK_INGRESS_SUFFIX = "-ingress";

    public static final String INSECURE_DISABLE = "INSECURE-DISABLE";
    public static final String CERTIFICATES_FOLDER = "/mnt/certificates";

    public static final String CONFIG_FOLDER = "/opt/keycloak/conf";
    public static final String TRUSTSTORES_FOLDER = CONFIG_FOLDER + "/truststores";
    public static final String CACHE_CONFIG_SUBFOLDER = "cache";
    public static final String CACHE_CONFIG_FOLDER = CONFIG_FOLDER + "/" + CACHE_CONFIG_SUBFOLDER;

    public static final String KEYCLOAK_HTTP_RELATIVE_PATH_KEY = "http-relative-path";
    public static final String KEYCLOAK_HTTP_MANAGEMENT_RELATIVE_PATH_KEY = "http-management-relative-path";

    public static final String KEYCLOAK_NETWORK_POLICY_SUFFIX = "-network-policy";
}
