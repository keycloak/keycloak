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
package org.keycloak.models;

/**
 * A representation of a Script with some additional meta-data.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public interface ScriptModel {

    /**
     * MIME-Type for JavaScript
     */
    String TEXT_JAVASCRIPT = "text/javascript";

    /**
     * Returns the unique id of the script. {@literal null} for ad-hoc created scripts.
     */
    String getId();

    /**
     * Returns the realm id in which the script was defined.
     */
    String getRealmId();

    /**
     * Returns the name of the script.
     */
    String getName();

    /**
     * Returns the MIME-type if the script code, e.g. for Java Script the MIME-type, {@code text/javascript} is used.
     */
    String getMimeType();

    /**
     * Returns the actual source code of the script.
     */
    String getCode();

    /**
     * Returns the description of the script.
     */
    String getDescription();
}
