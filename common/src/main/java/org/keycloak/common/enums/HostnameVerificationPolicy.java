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

package org.keycloak.common.enums;

public enum HostnameVerificationPolicy {

    /**
     * Hostname verification is not done on the server's certificate
     */
    ANY,

    /**
     * Allows wildcards in subdomain names (e.g. *.foo.com) to match anything, including multiple levels (e.g. a.b.foo.com)
     */
    @Deprecated
    WILDCARD,

    /**
     * CN must match hostname connecting to
     */
    @Deprecated
    STRICT,

    /**
     * Similar to STRICT, but checks against a more complete public suffix matcher
     */
    DEFAULT
}