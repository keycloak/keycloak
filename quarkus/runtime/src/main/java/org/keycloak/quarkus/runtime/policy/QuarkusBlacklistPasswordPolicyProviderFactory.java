/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.policy;

import java.io.File;

import org.keycloak.policy.BlacklistPasswordPolicyProviderFactory;
import org.keycloak.quarkus.runtime.Environment;

/**
 * <p>Quarkus implementation of the BlacklistPasswordPolicyProviderFactory. The
 * default path for the list files is calculated using the quarkus environment
 * class, in order to obtain the correct <em>data</em> directory.
 *
 * @author rmartinc
 */
public class QuarkusBlacklistPasswordPolicyProviderFactory extends BlacklistPasswordPolicyProviderFactory {

    @Override
    public String getDefaultBlacklistsBasePath() {
        return Environment.getDataDir().map(d -> d + File.separator + PASSWORD_BLACKLISTS_FOLDER).orElse(null);
    }
}
