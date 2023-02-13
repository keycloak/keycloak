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

package org.keycloak.rotation;

import java.security.Key;
import java.security.KeyManagementException;

/**
 * This interface defines a method for obtaining a security key by ID.
 * <p>
 * If the {@code KeyLocator} implementor wants to make all its keys available for iteration,
 * it should implement {@link Iterable}&lt;{@code T extends }{@link Key}&gt; interface.
 * The base {@code KeyLocator} does not extend this interface to enable {@code KeyLocators}
 * that do not support listing their keys.
 *
 * @author <a href="mailto:hmlnarik@redhat.com">Hynek Mlnařík</a>
 */
public interface KeyLocator {

    /**
     * Returns a key with a particular ID.
     * @param kid Key ID
     * @param configuration Configuration
     * @return key, which should be used for verify signature on given "input"
     * @throws KeyManagementException
     */
    Key getKey(String kid) throws KeyManagementException;

    /**
     * If this key locator caches keys in any way, forces this cache cleanup
     * and refreshing the keys.
     */
    void refreshKeyCache();

}
