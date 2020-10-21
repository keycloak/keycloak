/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.privacy.anonymize;

/**
 * Allows to anonymize sensitive input with potentially personal identifiable information (PII).
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public interface Anonymizer {

    /**
     * Anonymizes the given input string according to the rules provided for the given type hint.
     *
     * @param input
     * @param typeHint
     * @return
     */
    String anonymize(String input, String typeHint);

}
