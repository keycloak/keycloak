/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Serves for excluding test classes or methods from running in some specific auth server container
 *
 * for example annotation @AuthServerContainerExclude(AuthServer.REMOTE) on a test class makes tests
 * in the class NOT running when -Pauth-server-remote is used
 *
 * The annotation has @Inherited property this means that annotation parent class will exclude all child class
 *
 * for example annotating AbstractAdapterTest will exclude all adapter tests
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface AuthServerContainerExclude {
    AuthServer[] value();

    public enum AuthServer {
        REMOTE
    }
}
