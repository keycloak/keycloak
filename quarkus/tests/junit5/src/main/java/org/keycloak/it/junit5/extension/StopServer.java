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

package org.keycloak.it.junit5.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.it.utils.KeycloakDistribution;

/**
 * {@link StopServer} is used to control when a distribution server stops at a
 * method level.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface StopServer {

    public enum Mode {
        /**
         * Stops the server process before quarkus augmentation or startup.
         */
        BEFORE_QUARKUS,
        /**
         * Stops the server process after database initialization, but before
         * bootstrapping (creating the master realm, boostrap admin, etc.).
         */
        BEFORE_BOOTSTRAP, 
        /**
         * Stops the server immediately after it successfully starts.
         */
        AFTER_START, 
        /**
         * Server will stop if {@link KeycloakDistribution#stop()} is called, another run is called. 
         * If the server is still running at the end of the method, it will be stopped automatically.
         */
        MANUAL 
    }

    Mode value() default Mode.AFTER_START;

}
