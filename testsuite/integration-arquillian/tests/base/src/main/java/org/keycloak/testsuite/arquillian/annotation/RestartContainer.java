/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019 Red Hat Inc. and/or its affiliates and other contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.arquillian.annotation;

import org.keycloak.common.Profile;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks test class to cause restart of running container before the tests.
 * Use parameters to pass information to: {@link org.keycloak.testsuite.arquillian.containers.KeycloakContainerEventsController#afterOriginalContainerStop(RestartContainer)}
 * and {@link org.keycloak.testsuite.arquillian.containers.KeycloakContainerEventsController#beforeNewContainerStart(RestartContainer)} methods.
 * It is useful for tests which needs configuration changes before the container is started.
 * Like copy or remove {@code keycloak-admin-user.json} file.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE})
public @interface RestartContainer {

    /**
     * @return Flag whether to re-start the auth server with keycloak-add-user.json file.
     */
    boolean withoutKeycloakAddUserFile() default true;

    /**
     * @return Flag whether to perform database initialization (all tables will be dropped)
     */
    boolean initializeDatabase() default false;

    /**
     * @return Wait time in milliseconds after database initialization.
     */
    long intializeDatabaseWait() default 0;
}
