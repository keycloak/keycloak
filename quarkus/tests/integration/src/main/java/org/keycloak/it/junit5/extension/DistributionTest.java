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
import org.junit.jupiter.api.extension.ExtendWith;

@Target(ElementType.TYPE)
@ExtendWith({ CLITestExtension.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributionTest {

    boolean debug() default false;
    boolean keepAlive() default false;
    boolean createAdminUser() default false;
    boolean enableTls() default false;

    enum ReInstall {

        /**
         * Install the distribution only once before running a test class.
         */
        BEFORE_ALL,

        /**
         * Re-install the distribution before running a test method.
         */
        BEFORE_TEST,

        /**
         * Does not reset the distribution such as removing data, providers, and conf directories.
         */
        NEVER;
    }

    ReInstall reInstall() default ReInstall.BEFORE_ALL;

    /**
     * If any build option must be unset after the running the build command.
     */
    boolean removeBuildOptionsAfterBuild() default false;

    /**
     * If any option must be set when starting the server.
     */
    String[] defaultOptions() default {};
}

