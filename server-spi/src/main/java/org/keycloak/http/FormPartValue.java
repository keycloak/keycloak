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

package org.keycloak.http;

import java.io.InputStream;

public interface FormPartValue {

    /**
     * Returns the value as a string.
     *
     * @return the string representation of the form part value
     */
    String asString();

    /**
     * Returns the input stream.
     * <p>
     * If the value is originally a string, it will be converted to an input stream.
     *
     * @return the input stream representation of the form part value
     */
    InputStream asInputStream();

    /**
     * Indicates whether the value was originally provided as an input stream.
     *
     * @return {@code true} if the form part value was created from an input stream; {@code false} otherwise
     */
    boolean isInputStream();
}
