/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile.legacy;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class DefaultUserProfileProvider extends AbstractUserProfileProvider<DefaultUserProfileProvider> {

    private static final String PROVIDER_ID = "legacy-user-profile";

    public DefaultUserProfileProvider() {
        // for reflection
    }

    public DefaultUserProfileProvider(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> validators) {
        super(session, validators);
    }

    @Override
    protected DefaultUserProfileProvider create(KeycloakSession session, Map<UserProfileContext, UserProfileMetadata> metadataRegistry) {
        return new DefaultUserProfileProvider(session, metadataRegistry);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 1;
    }
}
