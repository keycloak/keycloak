/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.arquillian.annotation;

import org.keycloak.testsuite.arquillian.InfinispanStatistics.Constants;
import org.keycloak.testsuite.crossdc.DC;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author hmlnarik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface JmxInfinispanChannelStatistics {

    /** JMX domain. Should be set to default (@{code ""}) if the node to get the statistics from should be obtained from {@link #dcIndex()} and {@link #dcNodeIndex()}. */
    String domain() default "";

    // JMX address properties
    String type() default Constants.TYPE_CHANNEL;
    String cluster() default "*";

    // Host address - either given by arrangement of DC ...

    /** Index of the data center, starting from 0 */
    DC dc() default DC.UNDEFINED;
    /** Index of the node within data center, starting from 0. Nodes are ordered by arquillian qualifier as per {@link AuthServerTestEnricher} */
    int dcNodeIndex() default -1;

    /** Port for management */
    int managementPort() default -1;
    /** Name of system property to obtain management port from */
    String managementPortProperty() default "";
    /** Host name to connect to */
    String host() default "";
    /** Name of system property to obtain host name from */
    String hostProperty() default "";
}
