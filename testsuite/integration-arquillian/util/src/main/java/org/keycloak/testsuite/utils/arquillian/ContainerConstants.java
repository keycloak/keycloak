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
package org.keycloak.testsuite.utils.arquillian;

public interface ContainerConstants {
    public static final String APP_SERVER_PREFIX = "app-server-";

    public static final String APP_SERVER_UNDERTOW = APP_SERVER_PREFIX + "undertow";

    public static final String APP_SERVER_WILDFLY = APP_SERVER_PREFIX + "wildfly";
    public static final String APP_SERVER_WILDFLY_CLUSTER = APP_SERVER_WILDFLY + "-ha-node-1;" + APP_SERVER_WILDFLY + "-ha-node-2";

    public static final String APP_SERVER_WILDFLY_DEPRECATED = APP_SERVER_PREFIX + "wildfly-deprecated";
    public static final String APP_SERVER_WILDFLY_DEPRECATED_CLUSTER = APP_SERVER_WILDFLY_DEPRECATED + "-ha-node-1;" + APP_SERVER_WILDFLY_DEPRECATED + "-ha-node-2";

    public static final String APP_SERVER_EAP = APP_SERVER_PREFIX + "eap";
    public static final String APP_SERVER_EAP_CLUSTER = APP_SERVER_EAP + "-ha-node-1;" + APP_SERVER_EAP + "-ha-node-2";

    public static final String APP_SERVER_EAP71 = APP_SERVER_PREFIX + "eap71";

    public static final String APP_SERVER_EAP6 = APP_SERVER_PREFIX + "eap6";
    public static final String APP_SERVER_EAP6_CLUSTER = APP_SERVER_EAP6 + "-ha-node-1;" + APP_SERVER_EAP6 + "-ha-node-2";

    public static final String APP_SERVER_FUSE63 = APP_SERVER_PREFIX + "fuse63";
    public static final String APP_SERVER_FUSE7X = APP_SERVER_PREFIX + "fuse7x";

    public static final String APP_SERVER_JETTY94 = APP_SERVER_PREFIX + "jetty94";
    public static final String APP_SERVER_JETTY93 = APP_SERVER_PREFIX + "jetty93";
    public static final String APP_SERVER_JETTY92 = APP_SERVER_PREFIX + "jetty92";

    public static final String APP_SERVER_TOMCAT7 = APP_SERVER_PREFIX + "tomcat7";
    public static final String APP_SERVER_TOMCAT8 = APP_SERVER_PREFIX + "tomcat8";
    public static final String APP_SERVER_TOMCAT9 = APP_SERVER_PREFIX + "tomcat9";

}
