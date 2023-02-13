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

package org.keycloak.representations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.common.Version;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VersionRepresentation {
    public static final VersionRepresentation SINGLETON;

    private final String version = Version.VERSION;
    private final String buildTime = Version.BUILD_TIME;

    static {
         SINGLETON = new VersionRepresentation();
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("build-time")
    public String getBuildTime() {
        return buildTime;
    }
}
