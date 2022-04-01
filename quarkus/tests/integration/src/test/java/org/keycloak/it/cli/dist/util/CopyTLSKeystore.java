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

package org.keycloak.it.cli.dist.util;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.keycloak.it.utils.KeycloakDistribution;

public class CopyTLSKeystore implements Consumer<KeycloakDistribution> {

    @Override
    public void accept(KeycloakDistribution distribution) {
        distribution.copyOrReplaceFileFromClasspath("/server.keystore", Path.of("conf", "server.keystore"));
    }
}
