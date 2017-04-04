/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken;

import org.keycloak.representations.JsonWebToken;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author hmlnarik
 */
public class DefaultActionTokenKey extends JsonWebToken {

    // The authenticationSession note with ID of the user authenticated via the action token
    public static final String ACTION_TOKEN_USER_ID = "ACTION_TOKEN_USER";

    public DefaultActionTokenKey(String userId, String actionId) {
        subject = userId;
        type = actionId;
    }

    @JsonIgnore
    public String getUserId() {
        return getSubject();
    }

    @JsonIgnore
    public String getActionId() {
        return getType();
    }

}
