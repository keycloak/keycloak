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

package org.keycloak.userprofile;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class AttributeContext {

    private final KeycloakSession session;
    private final Map.Entry<String, List<String>> attribute;
    private final UserModel user;
    private final AttributeMetadata metadata;
    private UserProfileContext context;

    public AttributeContext(UserProfileContext context, KeycloakSession session, Map.Entry<String, List<String>> attribute,
            UserModel user, AttributeMetadata metadata) {
        this.context = context;
        this.session = session;
        this.attribute = attribute;
        this.user = user;
        this.metadata = metadata;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public Map.Entry<String, List<String>> getAttribute() {
        return attribute;
    }

    public UserModel getUser() {
        return user;
    }

    public UserProfileContext getContext() {
        return context;
    }

    public AttributeMetadata getMetadata() {
        return metadata;
    }
}
