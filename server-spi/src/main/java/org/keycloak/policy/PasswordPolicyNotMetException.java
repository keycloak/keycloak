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

package org.keycloak.policy;

import org.keycloak.models.ModelException;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class PasswordPolicyNotMetException extends ModelException {

    private String username;

    public PasswordPolicyNotMetException() {
        super();
    }

    public PasswordPolicyNotMetException(String message) {
        super(message);
    }

    public PasswordPolicyNotMetException(String message, String username) {
        super(message);
        this.username = username;
    }

    public PasswordPolicyNotMetException(String message, String username, Throwable cause) {
        super(message, cause);
        this.username = username;
    }

    public PasswordPolicyNotMetException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getUsername() {
        return username;
    }
}
