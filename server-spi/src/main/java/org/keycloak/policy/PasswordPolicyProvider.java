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

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:roelof.naude@epiuse.com">Roelof Naude</a>
 */
public interface PasswordPolicyProvider extends Provider {

    String STRING_CONFIG_TYPE = "String";
    String INT_CONFIG_TYPE = "int";

    PolicyError validate(RealmModel realm, UserModel user, String password);
    PolicyError validate(String user, String password);
    Object parseConfig(String value);

    default Integer parseInteger(String value, Integer defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            throw new PasswordPolicyConfigException("Not a valid number");
        }
    }

}
