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
package org.keycloak.saml.processing.core.saml.v2.common;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;

import java.util.UUID;

/**
 * Utility class that generates unique IDs
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 5, 2009
 */
public class IDGenerator {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /*
     * Create a basic unique ID
     */
    public static String create() {
        return UUID.randomUUID().toString();
    }

    /**
     * Create an id that is prefixed by a string
     *
     * @param prefix
     *
     * @return an id
     *
     * @throws IllegalArgumentException when prefix is null
     */
    public static String create(String prefix) {
        if (prefix == null)
            throw logger.nullArgumentError("prefix");
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(IDGenerator.create());
        return sb.toString();
    }
}