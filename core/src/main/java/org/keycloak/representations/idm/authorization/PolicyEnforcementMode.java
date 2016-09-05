/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.idm.authorization;

/**
 * The policy enforcement mode dictates how authorization requests are handled by the server.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public enum PolicyEnforcementMode {

    /**
     * Requests are denied by default even when there is no policy associated with a given resource.
     */
    ENFORCING,

    /**
     * Requests are allowed even when there is no policy associated with a given resource.
     */
    PERMISSIVE,

    /**
     * Completely disables the evaluation of policies and allow access to any resource.
     */
    DISABLED
}
