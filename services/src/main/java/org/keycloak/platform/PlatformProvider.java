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

package org.keycloak.platform;

import java.io.File;


public interface PlatformProvider {

    String name();

    default void onStartup(Runnable runnable) {

    }

    default void onShutdown(Runnable runnable) {

    }

    void exit(Throwable cause);

    /**
     * @return tmp directory specific to target platform. Implementation can make sure to create "tmp" directory in case it does not exists.
     * The directory should be usually inside the corresponding server directory. In production, it should not be system directory like "/tmp" .
     */
    File getTmpDirectory();

}
