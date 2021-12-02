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

package org.keycloak.authentication.requiredactions.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.userprofile.UserProfileContext;

/**
 * Abstraction, which allows to display updateProfile page in various contexts (Required action of already existing user, or first identity provider
 * login when user doesn't yet exists in Keycloak DB)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface UpdateProfileContext {
    
    UserProfileContext getUserProfileContext();

    boolean isEditUsernameAllowed();

    String getUsername();

    void setUsername(String username);

    String getEmail();

    void setEmail(String email);

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    Map<String, List<String>> getAttributes();

    void setSingleAttribute(String name, String value);

    void setAttribute(String key, List<String> value);

    String getFirstAttribute(String name);

    /**
     * @deprecated Use {@link #getAttributeStream(String) getAttributeStream} instead.
     */
    @Deprecated
    default List<String> getAttribute(String key) {
        return this.getAttributeStream(key).collect(Collectors.toList());
    }

    /**
     * Obtains all values associated with the specified attribute name.
     *
     * @param name the name of the attribute.
     * @return a non-null {@link Stream} of attribute values.
     */
    Stream<String> getAttributeStream(String name);
}
